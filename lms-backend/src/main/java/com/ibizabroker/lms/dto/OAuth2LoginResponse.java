package com.ibizabroker.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OAuth2 Login Response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2LoginResponse {
    private String token;
    private String username;
    private String email;
    private String fullName;
    private String role;
    private boolean isNewUser;
}
