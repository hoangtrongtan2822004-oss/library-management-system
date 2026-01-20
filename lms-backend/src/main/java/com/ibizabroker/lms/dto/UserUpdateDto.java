package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 📝 User Update Request DTO (Admin only)
 * 
 * Dùng cho endpoint PUT/PATCH /api/admin/users/{id}
 * 
 * 📌 Security Notes:
 * - Chỉ dùng cho request (input)
 * - Chỉ ADMIN mới được gọi endpoint này
 * - KHÔNG cho phép update password qua DTO này (dùng ChangePasswordRequest)
 * 
 * 📌 Validation Rules:
 * - Tất cả fields đều optional (partial update)
 * - name: Nếu có thì >= 1 ký tự
 * - username: Nếu có thì >= 3 ký tự
 * - roles: Optional
 * 
 * 👉 Pattern: Partial Update (PATCH semantic)
 * - Chỉ update các field không null
 * - Không cần gửi full object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserUpdateDto {
    @Size(min = 1, message = "Tên phải có ít nhất 1 ký tự")
    private String name;
    
    @Size(min = 3, message = "Username phải có ít nhất 3 ký tự")
    private String username;
    
    private List<String> roles;
}
