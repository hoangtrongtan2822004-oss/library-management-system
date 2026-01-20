package com.ibizabroker.lms.service;

import com.ibizabroker.lms.dao.BooksRepository;
import com.ibizabroker.lms.dao.LoanRepository;
import com.ibizabroker.lms.dao.UsersRepository;
import com.ibizabroker.lms.dto.LoanDetailsDto;
import com.ibizabroker.lms.dto.ReportDataDto;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Feature 3: Report Service - Export PDF/Excel
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final LoanRepository loanRepository;
    private final BooksRepository booksRepository;
    private final UsersRepository usersRepository;

    public byte[] generateLoansExcelReport(LocalDate startDate, LocalDate endDate) throws IOException {
        List<LoanDetailsDto> loans = loanRepository.findLoanDetailsForReport(startDate, endDate);
        
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("Báo cáo mượn sách");
            
            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            // Headers
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Tên sách", "Người mượn", "Ngày mượn", "Ngày hẹn trả", "Ngày trả", "Trạng thái", "Tiền phạt"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Data rows
            int rowNum = 1;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            
            for (LoanDetailsDto loan : loans) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(loan.getLoanId());
                row.createCell(1).setCellValue(loan.getBookName());
                row.createCell(2).setCellValue(loan.getUserName());
                row.createCell(3).setCellValue(formatDate(loan.getLoanDate(), formatter));
                row.createCell(4).setCellValue(formatDate(loan.getDueDate(), formatter));
                row.createCell(5).setCellValue(formatDate(loan.getReturnDate(), formatter));
                row.createCell(6).setCellValue(loan.getStatus() != null ? loan.getStatus().name() : "");
                row.createCell(7).setCellValue(loan.getFineAmount() != null ? loan.getFineAmount().toString() : "0");
            }
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] generateBooksExcelReport() throws IOException {
        var books = booksRepository.findAll();
        
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("Danh sách sách");
            
            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            // Headers
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Tên sách", "Tác giả", "Thể loại", "Số bản có sẵn", "ISBN", "Năm XB"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Data rows
            int rowNum = 1;
            for (var book : books) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(book.getId());
                row.createCell(1).setCellValue(book.getName());
                row.createCell(2).setCellValue(book.getAuthors().stream()
                        .map(a -> a.getName())
                        .reduce((a, b) -> a + ", " + b).orElse(""));
                row.createCell(3).setCellValue(book.getCategories().stream()
                        .map(c -> c.getName())
                        .reduce((a, b) -> a + ", " + b).orElse(""));
                row.createCell(4).setCellValue(book.getNumberOfCopiesAvailable() != null ? book.getNumberOfCopiesAvailable() : 0);
                row.createCell(5).setCellValue(book.getIsbn() != null ? book.getIsbn() : "");
                row.createCell(6).setCellValue(book.getPublishedYear() != null ? book.getPublishedYear() : 0);
            }
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] generateUsersExcelReport() throws IOException {
        var users = usersRepository.findAll();
        
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("Danh sách người dùng");
            
            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            // Headers
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Tên đăng nhập", "Họ tên", "Email", "Lớp", "SĐT", "Vai trò"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Data rows
            int rowNum = 1;
            for (var user : users) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(user.getUserId());
                row.createCell(1).setCellValue(user.getUsername());
                row.createCell(2).setCellValue(user.getName());
                row.createCell(3).setCellValue(user.getEmail() != null ? user.getEmail() : "");
                row.createCell(4).setCellValue(user.getStudentClass() != null ? user.getStudentClass() : "");
                row.createCell(5).setCellValue(user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
                row.createCell(6).setCellValue(user.getRoles().stream()
                        .map(r -> r.getRoleName())
                        .reduce((a, b) -> a + ", " + b).orElse(""));
            }
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public ReportDataDto getReportSummary(LocalDate startDate, LocalDate endDate) {
        long totalLoans = loanRepository.countByDateRange(startDate, endDate);
        long returnedLoans = loanRepository.countReturnedByDateRange(startDate, endDate);
        long overdueLoans = loanRepository.countOverdueByDateRange(startDate, endDate);
        BigDecimal totalFines = loanRepository.sumFinesByDateRange(startDate, endDate);
        
        return new ReportDataDto(
                totalLoans,
                returnedLoans,
                overdueLoans,
                totalFines != null ? totalFines : BigDecimal.ZERO  // ✅ Use BigDecimal instead of double
        );
    }

    private String formatDate(Object date, DateTimeFormatter formatter) {
        if (date == null) return "";
        if (date instanceof LocalDate) {
            return ((LocalDate) date).format(formatter);
        }
        if (date instanceof LocalDateTime) {
            return ((LocalDateTime) date).format(formatter);
        }
        return date.toString();
    }
}
