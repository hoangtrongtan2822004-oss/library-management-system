package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * 👤 User Creation Request DTO (Admin only)
 * 
 * Dùng cho endpoint POST /api/admin/users
 * 
 * 📌 Security Notes:
 * - Chỉ dùng cho request (input)
 * - Chỉ ADMIN mới được gọi endpoint này
 * - Password sẽ được hash bằng BCrypt trước khi lưu DB
 * - KHÔNG BAO GIỜ return password trong response
 * 
 * 📌 Validation Rules:
 * - name: Bắt buộc
 * - email: Bắt buộc, phải đúng format email
 * - username: Bắt buộc, >= 3 ký tự
 * - password: Bắt buộc, >= 6 ký tự
 * - roles: Optional (default USER role nếu không có)
 * 
 * 🎯 Khác biệt so với RegisterRequest:
 * - UserCreateDto: Admin tạo user với tùy chọn roles
 * - RegisterRequest: User tự đăng ký (mặc định role USER)
 * 
 * 🎯 TODO: Nâng cấp thêm
 * - [ ] @ValidRoles check roles có tồn tại trong DB
 * - [ ] @UniqueEmail check trùng email
 * - [ ] @UniqueUsername check trùng username
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserCreateDto {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Username is required")
    @Size(min = 3, message = "Username must be at least 3 characters long")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password;

    private Set<String> roles;
}