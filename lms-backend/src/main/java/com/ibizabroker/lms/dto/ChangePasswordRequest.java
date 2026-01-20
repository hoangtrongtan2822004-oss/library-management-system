package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 🔑 Change Password Request DTO
 * 
 * Dùng cho endpoint POST /api/user/change-password
 * 
 * 📌 Security Notes:
 * - Chỉ dùng cho request (input)
 * - User chỉ có thể đổi password của chính mình
 * - Phải xác thực oldPassword trước khi đổi
 * - KHÔNG BAO GIỞ return password trong response
 * 
 * 📌 Validation Rules:
 * - oldPassword: Bắt buộc (không blank)
 * - newPassword: Bắt buộc, >= 6 ký tự
 * 
 * 🎯 TODO: Nâng cấp thêm
 * - [ ] @DifferentPasswords check newPassword khác oldPassword
 * - [ ] @PasswordStrength (yêu cầu chữ hoa, số, ký tự đặc biệt)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChangePasswordRequest {

    @NotBlank(message = "Mật khẩu cũ không được để trống")
    private String oldPassword;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 6, message = "Mật khẩu mới phải có ít nhất 6 ký tự")
    private String newPassword;
}
