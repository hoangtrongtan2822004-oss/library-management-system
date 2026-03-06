package com.ibizabroker.lms.service;

import com.ibizabroker.lms.dao.BooksRepository;
import com.ibizabroker.lms.dao.InventoryScanRepository;
import com.ibizabroker.lms.dao.InventorySessionRepository;
import com.ibizabroker.lms.dto.InventoryScanRequest;
import com.ibizabroker.lms.dto.InventoryScanResultDto;
import com.ibizabroker.lms.dto.InventorySessionDto;
import com.ibizabroker.lms.dto.InventorySummaryDto;
import com.ibizabroker.lms.entity.Books;
import com.ibizabroker.lms.entity.InventoryScan;
import com.ibizabroker.lms.entity.InventorySession;
import com.ibizabroker.lms.entity.InventorySessionStatus;
import com.ibizabroker.lms.exceptions.NotFoundException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryService {

    private final InventorySessionRepository sessionRepository;
    private final InventoryScanRepository scanRepository;
    private final BooksRepository booksRepository;

    public InventorySessionDto startSession(String name, Integer expectedTotal) {
        InventorySession session = new InventorySession();
        session.setName(name);
        session.setExpectedTotal(expectedTotal);
        session.setStatus(InventorySessionStatus.IN_PROGRESS);
        session.setStartedAt(LocalDateTime.now());
        session.setScannedTotal(0);
        return InventorySessionDto.fromEntity(sessionRepository.save(session));
    }

    @SuppressWarnings("null")
    public InventoryScanResultDto recordScan(Long sessionId, InventoryScanRequest request) {
        InventorySession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên kiểm kê"));

        String rawCode = request.getCode() != null ? request.getCode().trim() : "";
        String cleanedCode = rawCode.replaceAll("[-\\s]", "");
        String shelfCode = normalizeShelfCode(request.getShelfCode());

        Books book = findBookByCode(rawCode, cleanedCode);
        boolean isDuplicate = false;
        boolean isUnknown = book == null;

        if (book != null) {
            isDuplicate = scanRepository.existsBySessionIdAndBookId(sessionId, book.getId());
        }

        InventoryScan scan = new InventoryScan();
        scan.setSession(session);
        scan.setBook(book);
        scan.setScannedCode(cleanedCode.isEmpty() ? rawCode : cleanedCode);
        scan.setScannedShelfCode(shelfCode);
        scan.setDuplicate(isDuplicate);
        scan.setUnknown(isUnknown);
        scanRepository.save(scan);

        if (!isDuplicate) {
            session.setScannedTotal((session.getScannedTotal() != null ? session.getScannedTotal() : 0) + 1);
            sessionRepository.save(session);
        }

        return InventoryScanResultDto.builder()
            .sessionId(sessionId)
            .bookId(book != null ? book.getId() : null)
            .bookName(book != null ? book.getName() : null)
            .isbn(book != null ? book.getIsbn() : cleanedCode)
            .shelfCode(shelfCode)
            .scannedAt(scan.getScannedAt())
            .duplicate(isDuplicate)
            .unknown(isUnknown)
            .build();
    }

    @SuppressWarnings("null")
    public InventorySessionDto completeSession(Long sessionId) {
        InventorySession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên kiểm kê"));
        session.setStatus(InventorySessionStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());
        return InventorySessionDto.fromEntity(sessionRepository.save(session));
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public InventorySummaryDto buildSummary(Long sessionId) {
        InventorySession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên kiểm kê"));

        List<InventoryScan> scans = scanRepository.findBySessionId(sessionId);
        if (scans.isEmpty()) {
            return InventorySummaryDto.empty(sessionId);
        }

        Set<Integer> scannedBookIds = scans.stream()
            .filter(scan -> !scan.isDuplicate())
            .map(InventoryScan::getBook)
            .filter(book -> book != null)
            .map(Books::getId)
            .collect(Collectors.toSet());

        Set<String> scannedShelfCodes = scans.stream()
            .map(InventoryScan::getScannedShelfCode)
            .filter(code -> code != null && !code.isBlank())
            .map(this::normalizeShelfCode)
            .collect(Collectors.toSet());

        List<Books> expectedBooks;
        Integer expectedTotal = session.getExpectedTotal();
        if (expectedTotal == null) {
            if (!scannedShelfCodes.isEmpty()) {
                expectedTotal = (int) booksRepository.countByShelfCodeIn(new ArrayList<>(scannedShelfCodes));
                expectedBooks = booksRepository.findByShelfCodeIn(new ArrayList<>(scannedShelfCodes));
            } else {
                expectedTotal = (int) booksRepository.count();
                expectedBooks = booksRepository.findAll();
            }
        } else {
            if (!scannedShelfCodes.isEmpty()) {
                expectedBooks = booksRepository.findByShelfCodeIn(new ArrayList<>(scannedShelfCodes));
            } else {
                expectedBooks = booksRepository.findAll();
            }
        }

        List<InventorySummaryDto.MissingItem> missingItems = new ArrayList<>();
        for (Books book : expectedBooks) {
            if (!scannedBookIds.contains(book.getId())) {
                missingItems.add(InventorySummaryDto.MissingItem.builder()
                    .bookId(book.getId())
                    .bookName(book.getName())
                    .isbn(book.getIsbn())
                    .expectedShelfCode(book.getShelfCode())
                    .build());
            }
        }

        List<InventorySummaryDto.MisplacedItem> misplacedItems = new ArrayList<>();
        for (InventoryScan scan : scans) {
            Books book = scan.getBook();
            if (book == null || scan.getScannedShelfCode() == null) {
                continue;
            }
            String expectedShelf = normalizeShelfCode(book.getShelfCode());
            String actualShelf = normalizeShelfCode(scan.getScannedShelfCode());
            if (expectedShelf != null && actualShelf != null && !expectedShelf.equalsIgnoreCase(actualShelf)) {
                misplacedItems.add(InventorySummaryDto.MisplacedItem.builder()
                    .bookId(book.getId())
                    .bookName(book.getName())
                    .isbn(book.getIsbn())
                    .expectedShelfCode(expectedShelf)
                    .scannedShelfCode(actualShelf)
                    .build());
            }
        }

        List<InventorySummaryDto.UnknownItem> unknownItems = scanRepository.findBySessionIdAndUnknownTrue(sessionId)
            .stream()
            .map(scan -> InventorySummaryDto.UnknownItem.builder()
                .isbn(scan.getScannedCode())
                .shelfCode(scan.getScannedShelfCode())
                .build())
            .collect(Collectors.toList());

        return InventorySummaryDto.builder()
            .sessionId(sessionId)
            .expectedTotal(expectedTotal)
            .scannedTotal(session.getScannedTotal())
            .missingTotal(missingItems.size())
            .misplacedTotal(misplacedItems.size())
            .unknownTotal(unknownItems.size())
            .missingItems(missingItems)
            .misplacedItems(misplacedItems)
            .unknownItems(unknownItems)
            .build();
    }

    @Transactional(readOnly = true)
    public byte[] exportSummaryExcel(Long sessionId) throws IOException {
        InventorySummaryDto summary = buildSummary(sessionId);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            writeSummarySheet(workbook, summary);
            writeMissingSheet(workbook, summary);
            writeMisplacedSheet(workbook, summary);
            writeUnknownSheet(workbook, summary);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void writeSummarySheet(Workbook workbook, InventorySummaryDto summary) {
        Sheet sheet = workbook.createSheet("Summary");
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Metric");
        header.createCell(1).setCellValue("Value");
        header.getCell(0).setCellStyle(headerStyle);
        header.getCell(1).setCellStyle(headerStyle);

        Object[][] rows = new Object[][]{
            {"Session", summary.getSessionId()},
            {"Expected", summary.getExpectedTotal()},
            {"Scanned", summary.getScannedTotal()},
            {"Missing", summary.getMissingTotal()},
            {"Misplaced", summary.getMisplacedTotal()},
            {"Unknown", summary.getUnknownTotal()}
        };

        int rowIdx = 1;
        for (Object[] row : rows) {
            Row r = sheet.createRow(rowIdx++);
            r.createCell(0).setCellValue(String.valueOf(row[0]));
            r.createCell(1).setCellValue(row[1] != null ? String.valueOf(row[1]) : "0");
        }
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void writeMissingSheet(Workbook workbook, InventorySummaryDto summary) {
        Sheet sheet = workbook.createSheet("Missing");
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);

        Row header = sheet.createRow(0);
        String[] headers = {"#", "Book", "ISBN", "Expected Shelf"};
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
            header.getCell(i).setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (InventorySummaryDto.MissingItem item : summary.getMissingItems()) {
            Row row = sheet.createRow(rowIdx);
            row.createCell(0).setCellValue(rowIdx);
            row.createCell(1).setCellValue(valueOrEmpty(item.getBookName()));
            row.createCell(2).setCellValue(valueOrEmpty(item.getIsbn()));
            row.createCell(3).setCellValue(valueOrEmpty(item.getExpectedShelfCode()));
            rowIdx++;
        }
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void writeMisplacedSheet(Workbook workbook, InventorySummaryDto summary) {
        Sheet sheet = workbook.createSheet("Misplaced");
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);

        Row header = sheet.createRow(0);
        String[] headers = {"#", "Book", "ISBN", "Expected Shelf", "Scanned Shelf"};
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
            header.getCell(i).setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (InventorySummaryDto.MisplacedItem item : summary.getMisplacedItems()) {
            Row row = sheet.createRow(rowIdx);
            row.createCell(0).setCellValue(rowIdx);
            row.createCell(1).setCellValue(valueOrEmpty(item.getBookName()));
            row.createCell(2).setCellValue(valueOrEmpty(item.getIsbn()));
            row.createCell(3).setCellValue(valueOrEmpty(item.getExpectedShelfCode()));
            row.createCell(4).setCellValue(valueOrEmpty(item.getScannedShelfCode()));
            rowIdx++;
        }
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void writeUnknownSheet(Workbook workbook, InventorySummaryDto summary) {
        Sheet sheet = workbook.createSheet("Unknown");
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);

        Row header = sheet.createRow(0);
        String[] headers = {"#", "Code/ISBN", "Shelf"};
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
            header.getCell(i).setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (InventorySummaryDto.UnknownItem item : summary.getUnknownItems()) {
            Row row = sheet.createRow(rowIdx);
            row.createCell(0).setCellValue(rowIdx);
            row.createCell(1).setCellValue(valueOrEmpty(item.getIsbn()));
            row.createCell(2).setCellValue(valueOrEmpty(item.getShelfCode()));
            rowIdx++;
        }
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private Books findBookByCode(String rawCode, String cleanedCode) {
        if (cleanedCode != null && !cleanedCode.isBlank()) {
            var byIsbn = booksRepository.findByIsbnIgnoreCase(cleanedCode);
            if (byIsbn.isPresent()) return byIsbn.get();
        }
        if (rawCode != null && !rawCode.isBlank() && !rawCode.equals(cleanedCode)) {
            var byIsbn = booksRepository.findByIsbnIgnoreCase(rawCode);
            if (byIsbn.isPresent()) return byIsbn.get();
        }
        if (cleanedCode != null && cleanedCode.matches("\\d{1,9}")) {
            try {
                int id = Integer.parseInt(cleanedCode);
                return booksRepository.findById(id).orElse(null);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String normalizeShelfCode(String shelfCode) {
        if (shelfCode == null) return null;
        String trimmed = shelfCode.trim();
        return trimmed.isEmpty() ? null : trimmed.toUpperCase();
    }

    private String valueOrEmpty(String value) {
        return value != null ? value : "";
    }
}
