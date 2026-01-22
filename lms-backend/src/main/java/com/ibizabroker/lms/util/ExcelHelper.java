package com.ibizabroker.lms.util;

/**
 * 📊 Excel Helper - Apache POI Utilities
 * 
 * 🎯 Purpose:
 * - Centralized Excel read/write operations
 * - Extract from ImportExportController for reusability
 * - Support .xlsx and .xls formats
 * 
 * 📌 Planned Features:
 * 1. Read Excel to List<Map<String, Object>>
 * 2. Write List<T> to Excel with column mapping
 * 3. Validate Excel structure (headers, data types)
 * 4. Generate Excel templates
 * 
 * ✅ Migrated logic from ImportExportController into helper methods
 * 
 * Current state: Books/Users import/export in ImportExportController
 * Target state: Generic ExcelHelper.read/write methods
 * 
 * @author Library Management System
 * @since Phase 8: Utility Enhancement (Planned)
 */
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExcelHelper {

    /**
     * Read an Excel or CSV file and map columns by the provided ordered list of column keys.
     * The implementation expects the first row in the spreadsheet to be a header row
     * and will start reading data from the second row (index 1). For CSV files the
     * same behaviour is applied.
     *
     * @param file    uploaded MultipartFile (.xlsx, .xls or .csv)
     * @param columns ordered list of keys to map each cell by column index
     * @return list of rows where each row is a Map of columnKey -> String value
     */
    public static List<Map<String, Object>> readExcel(MultipartFile file, List<String> columns) {
        String originalFilename = file.getOriginalFilename();
        String filename = (originalFilename == null ? "" : originalFilename).toLowerCase(Locale.ROOT);
        try {
            if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                return readExcelRows(file, columns);
            }
            return readCsvRows(file, columns);
        } catch (IOException e) {
            throw new RuntimeException("Không đọc được file: " + e.getMessage(), e);
        }
    }

    private static List<Map<String, Object>> readExcelRows(MultipartFile file, List<String> columns) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            List<Map<String, Object>> rows = new ArrayList<>();
            int firstDataRow = 1; // assume header exists
            int maxCols = columns.size();
            for (int r = firstDataRow; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Map<String, Object> map = new HashMap<>();
                for (int c = 0; c < maxCols; c++) {
                    String key = columns.get(c);
                    String val = "";
                    if (row.getCell(c) != null) {
                        val = row.getCell(c).toString().trim();
                    }
                    map.put(key, val);
                }
                rows.add(map);
            }
            return rows;
        }
    }

    private static List<Map<String, Object>> readCsvRows(MultipartFile file, List<String> columns) throws IOException {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) { first = false; continue; } // skip header
                String[] cols = line.split(",", -1);
                Map<String, Object> map = new HashMap<>();
                for (int i = 0; i < columns.size(); i++) {
                    String key = columns.get(i);
                    String val = i < cols.length ? cols[i].trim() : "";
                    map.put(key, val);
                }
                rows.add(map);
            }
        }
        return rows;
    }

    /**
     * Write a list of maps to an XLSX byte array. The ordered `columns` list defines
     * both the header row and the order of values for each row; for each map the
     * value for the column key will be written (toString), missing keys produce an empty cell.
     *
     * @param data    list of rows as Map<columnKey, value>
     * @param columns ordered list of column keys / header names
     * @return XLSX file content as byte[]
     */
    public static byte[] writeExcel(List<Map<String, Object>> data, List<String> columns) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            Row header = sheet.createRow(0);
            for (int i = 0; i < columns.size(); i++) {
                header.createCell(i).setCellValue(columns.get(i));
            }

            for (int r = 0; r < data.size(); r++) {
                Map<String, Object> rowMap = data.get(r);
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < columns.size(); c++) {
                    Object v = rowMap.get(columns.get(c));
                    row.createCell(c).setCellValue(v == null ? "" : String.valueOf(v));
                }
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Không thể xuất excel", e);
        }
    }
}
