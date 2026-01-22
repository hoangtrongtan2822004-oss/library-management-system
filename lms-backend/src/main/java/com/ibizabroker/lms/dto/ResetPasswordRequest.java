package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.ibizabroker.lms.validation.PasswordStrength;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 🔑 Reset Password Request DTO
 * 
 * Dùng cho endpoint POST /api/auth/reset-password
 * 
 * 📌 Security Flow:
 * 1. User click link trong email → có token
 * 2. User nhập newPassword
 * 3. System verify token và update password
 * 
 * 📌 Validation Rules:
 * - token: Bắt buộc (JWT token hoặc UUID)
 * - newPassword: Bắt buộc, >= 6 ký tự
 * 
 * 🎯 Notes:
 * - `@PasswordStrength` validator is available; consider enforcing for stronger passwords
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResetPasswordRequest {

    @NotBlank(message = "Token không được để trống")
    private String token;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    @PasswordStrength
    private String newPassword;
}
