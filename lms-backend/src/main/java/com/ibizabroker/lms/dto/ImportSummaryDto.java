package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 📊 CSV Import Summary Response DTO
 * 
 * Dùng cho endpoint POST /api/admin/import/books hoặc /api/admin/import/users
 * 
 * 📌 Import Results:
 * - successCount: Số record import thành công
 * - failedCount: Số record lỗi
 * - skippedCount: Số record bị skip (duplicate, invalid format)
 * - errors: Danh sách lỗi chi tiết (row number + error message)
 * 
 * 👉 Pattern: Response-Only DTO
 * - Chỉ dùng cho response (output)
 * - Tổng hợp kết quả import CSV/Excel
 * 
 * 🎯 Example Response:
 * - successCount: 95
 * - failedCount: 3
 * - skippedCount: 2
 * - errors: ["Row 10: ISBN invalid", "Row 15: Author not found"]
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImportSummaryDto {
    private Integer successCount;
    private Integer failedCount;
    private Integer skippedCount;
    
    @Builder.Default
    private List<String> errors = new ArrayList<>();
}
