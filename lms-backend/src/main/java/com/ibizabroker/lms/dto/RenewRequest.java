package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 🔄 Loan Renewal Request DTO
 * 
 * Dùng cho endpoint POST /api/user/loans/renew
 * 
 * 📌 Business Rules:
 * - User chỉ có thể renew khi chưa quá hạn
 * - Mỗi loan chỉ được renew tối đa N lần (check trong service)
 * - extraDays mặc định = 7 ngày
 * 
 * 📌 Validation Rules:
 * - loanId: Bắt buộc
 * - extraDays: Optional, >= 1 nếu có
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RenewRequest {
    
    @NotNull(message = "Loan ID không được để trống")
    private Integer loanId;
    
    @Builder.Default
    @Min(value = 1, message = "Số ngày gia hạn phải >= 1")
    private Integer extraDays = 7;
}
