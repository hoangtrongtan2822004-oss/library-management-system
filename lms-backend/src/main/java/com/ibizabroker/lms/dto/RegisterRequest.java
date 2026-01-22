package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.ibizabroker.lms.validation.ValidPhoneNumber;
import com.ibizabroker.lms.validation.UniqueUsername;
import com.ibizabroker.lms.validation.PasswordStrength;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 📝 User Registration Request DTO
 * 
 * Dùng cho endpoint POST /api/auth/register
 * 
 * 📌 Security Notes:
 * - Chỉ dùng cho request (input)
 * - Password sẽ được hash bằng BCrypt trước khi lưu DB
 * - KHÔNG BAO GIỜ return password trong response
 * 
 * 📌 Validation Rules:
 * - name: Bắt buộc
 * - username: Bắt buộc, 4-50 ký tự
 * - password: Bắt buộc, >= 6 ký tự
 * - studentClass: Optional
 * - phoneNumber: Optional, nếu có thì phải đúng format (10-11 số, bắt đầu 0)
 * 
 * 🎯 Validators applied
 * - `@ValidPhoneNumber`, `@UniqueUsername`, `@PasswordStrength` are applied to fields
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegisterRequest {

    @NotBlank(message = "Name cannot be empty")
    private String name;

    @NotBlank(message = "Username cannot be empty")
    @Size(min = 4, max = 50, message = "Username must be between 4 and 50 characters")
    @UniqueUsername
    private String username;

    @NotBlank(message = "Password cannot be empty")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    @PasswordStrength
    private String password;

    private String email;  // ✅ Phase 8: Add email field for user registration

    private String studentClass;
    
    @ValidPhoneNumber
    private String phoneNumber;
}