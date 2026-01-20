package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 📍 Book Physical Location DTO (Hybrid - Request & Response)
 * 
 * Dùng cho:
 * - POST /api/admin/books/{bookId}/location (Request)
 * - GET /api/public/books/{bookId}/location (Response)
 * 
 * 📌 Validation Rules (Request):
 * - floor: Bắt buộc, >= 1
 * - shelfCode: Bắt buộc (e.g., "A1", "B3")
 * - zone, rowNumber, position: Optional
 * 
 * 📌 Location Fields:
 * - floor: Tầng (1, 2, 3...)
 * - zone: Khu vực (A, B, C...)
 * - shelfCode: Mã kệ sách (A1, B3...)
 * - rowNumber: Số hàng (1, 2, 3...)
 * - position: Vị trí trong hàng (1, 2, 3...)
 * - description: Ghi chú thêm
 * 
 * 🎯 Example:
 * - Floor 2, Zone A, Shelf A3, Row 5, Position 12
 * - "Tầng 2, Khu A, Kệ A3, Hàng 5, Vị trí 12"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookLocationDto {

    @NotNull(message = "Tầng không được để trống")
    @Min(value = 1, message = "Tầng phải >= 1")
    private Integer floor;

    private String zone;

    @NotEmpty(message = "Mã kệ sách không được để trống")
    private String shelfCode;

    @Min(value = 1, message = "Số hàng phải >= 1")
    private Integer rowNumber;
    
    @Min(value = 1, message = "Vị trí phải >= 1")
    private Integer position;
    
    private String description;
}
