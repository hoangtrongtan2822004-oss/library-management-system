package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 📝 Book Reservation Request DTO
 * 
 * Dùng cho endpoint POST /api/user/reservations
 * 
 * 📌 Business Logic:
 * - User đặt chỗ khi sách hết (availableQuantity = 0)
 * - Khi có sách trả về → notify user theo thứ tự reservation
 * - Reservation có thời hạn (7-14 ngày), quá hạn sẽ hủy tự động
 * 
 * 📌 Validation Rules:
 * - bookId: Bắt buộc
 * - memberId: Bắt buộc (hoặc lấy từ JWT token)
 * - quantity: Optional, >= 1 nếu có (mặc định = 1)
 * - studentName: Optional (ghi chú)
 * - studentClass: Optional (ghi chú)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReservationRequest {
    
    @NotNull(message = "Book ID không được để trống")
    private Integer bookId;
    
    @NotNull(message = "Member ID không được để trống")
    private Integer memberId;
    
    @Min(value = 1, message = "Số lượng đặt chỗ phải >= 1")
    private Integer quantity;
    
    private String studentName;
    private String studentClass;
}
