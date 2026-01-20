// src/main/java/com/ibizabroker/lms/dto/LoginRequest.java
package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 🔐 Login Request DTO
 * 
 * Dùng cho endpoint POST /api/auth/login
 * 
 * 📌 Security Notes:
 * - Chỉ dùng cho request (input)
 * - KHÔNG BAO GIỜ return password trong response
 * - Password sẽ được hash bằng BCrypt trước khi so sánh
 * 
 * 📌 Validation Rules:
 * - username: Bắt buộc (không blank)
 * - password: Bắt buộc (không blank)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginRequest {
  @NotBlank(message = "Username không được để trống")
  private String username;
  
  @NotBlank(message = "Password không được để trống")
  private String password;
}
