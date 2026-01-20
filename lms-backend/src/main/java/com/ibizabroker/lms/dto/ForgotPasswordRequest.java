package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 📧 Forgot Password Request DTO
 * 
 * Dùng cho endpoint POST /api/auth/forgot-password
 * 
 * 📌 Security Flow:
 * 1. User nhập email
 * 2. System gửi reset token qua email
 * 3. User click link trong email → ResetPasswordRequest
 * 
 * 📌 Validation Rules:
 * - email: Bắt buộc, phải đúng format email
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ForgotPasswordRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;
}
