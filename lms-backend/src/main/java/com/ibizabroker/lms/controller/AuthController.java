package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dao.RoleRepository;
import com.ibizabroker.lms.dao.UsersRepository;
import com.ibizabroker.lms.dto.ForgotPasswordRequest;
import com.ibizabroker.lms.dto.RegisterRequest;
import com.ibizabroker.lms.dto.ResetPasswordRequest;
import com.ibizabroker.lms.entity.Role;
import com.ibizabroker.lms.entity.Users;
import com.ibizabroker.lms.util.JwtUtil;
import com.ibizabroker.lms.service.PasswordResetService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus; // ⭐️ ĐÃ THÊM
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException; // ⭐️ ĐÃ THÊM
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth") // Đảm bảo đã có /api
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    private final UsersRepository usersRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final PasswordResetService passwordResetService;

    public AuthController(UsersRepository usersRepo,
                          RoleRepository roleRepo,
                          PasswordEncoder encoder,
                          AuthenticationManager authManager,
                          JwtUtil jwtUtil,
                          PasswordResetService passwordResetService) {
        this.usersRepo = usersRepo;
        this.roleRepo = roleRepo;
        this.encoder = encoder;
        this.authManager = authManager;
        this.jwtUtil = jwtUtil;
        this.passwordResetService = passwordResetService;
    }

    // ===== REGISTER =====
    @SuppressWarnings("null")
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        String username = req.getUsername().trim().toLowerCase();
        if (usersRepo.existsByUsernameIgnoreCase(username)) {
            return ResponseEntity.status(409).body(Map.of("message", "Username is already taken."));
        }

        Users u = new Users();
        u.setName(req.getName().trim());
        u.setUsername(username);
        u.setPassword(encoder.encode(req.getPassword()));
        u.setEmail(req.getEmail());
        u.setStudentClass(req.getStudentClass());
        u.setPhoneNumber(req.getPhoneNumber());

        Role userRole = roleRepo.findByRoleName("ROLE_USER").orElseGet(() -> {
            Role r = new Role();
            r.setRoleName("ROLE_USER");
            return roleRepo.save(r);
        });
        u.addRole(userRole);

        Users saved = usersRepo.save(u);
        return ResponseEntity.created(URI.create("/users/" + saved.getUserId())).build();
    }

    // ===== AUTHENTICATE =====
    public static record LoginRequest(String username, String password) {}
    public static record LoginResponse(String token, String refreshToken, Integer userId, String name, List<String> roles) {}

    @SuppressWarnings("null")
    @PostMapping(
            value = "/authenticate",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    // ⭐️ SỬA: Đổi kiểu trả về thành ResponseEntity<?>
    public ResponseEntity<?> authenticate(@RequestBody LoginRequest req) {
        if (!StringUtils.hasText(req.username()) || !StringUtils.hasText(req.password())) {
            return ResponseEntity.badRequest().build();
        }

        String username = req.username().trim().toLowerCase();
        Authentication auth;

        // ⭐️ SỬA: Thêm try-catch để bắt lỗi sai mật khẩu
        try {
            auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, req.password())
            );
        } catch (BadCredentialsException e) {
            // Trả về 401 (Unauthorized) thay vì sập 500
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(Map.of("message", "Tên đăng nhập hoặc mật khẩu không đúng"));
        }
        // ⭐️ KẾT THÚC SỬA

        // Code bên dưới chỉ chạy nếu đăng nhập thành công
        Users u = usersRepo.findByUsernameWithRolesIgnoreCase(username)
                .orElseThrow(() -> new RuntimeException("User not found after auth"));

        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", u.getUserId());
        claims.put("roles", roles);

        UserDetails principal = (UserDetails) auth.getPrincipal();
        String token = jwtUtil.generateToken(principal, claims);
        String refreshToken = jwtUtil.generateRefreshToken(principal, claims);

        return ResponseEntity.ok(new LoginResponse(token, refreshToken, u.getUserId(), u.getName(), roles));
    }

    // ===== REFRESH TOKEN =====
    public static record RefreshTokenRequest(String refreshToken) {}

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest req) {
        if (!StringUtils.hasText(req.refreshToken())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Refresh token không hợp lệ"));
        }

        try {
            String username = jwtUtil.getUsernameFromToken(req.refreshToken());
            
            // Verify it's a refresh token (not access token)
            Claims claims = jwtUtil.parseAllClaims(req.refreshToken());
            String tokenType = (String) claims.get("tokenType");
            if (!"REFRESH".equals(tokenType)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Token không phải là refresh token"));
            }

            // Load user and generate new access token
            Users u = usersRepo.findByUsernameWithRolesIgnoreCase(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<String> roles = u.getRoles().stream()
                    .map(role -> role.getRoleName())
                    .toList();

            Map<String, Object> newClaims = new HashMap<>();
            newClaims.put("userId", u.getUserId());
            newClaims.put("roles", roles);

            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username(u.getUsername())
                    .password(u.getPassword())
                    .authorities(roles.stream()
                            .map(r -> new org.springframework.security.core.authority.SimpleGrantedAuthority(r))
                            .toList())
                    .build();

            String newAccessToken = jwtUtil.generateToken(userDetails, newClaims);

            return ResponseEntity.ok(Map.of(
                "token", newAccessToken,
                "userId", u.getUserId(),
                "name", u.getName(),
                "roles", roles
            ));
        } catch (ExpiredJwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Refresh token đã hết hạn, vui lòng đăng nhập lại"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Refresh token không hợp lệ"));
        }
    }

    // ===== FORGOT PASSWORD =====
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.initiateReset(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "Nếu email tồn tại, hướng dẫn khôi phục đã được gửi."));
    }

    // ===== RESET PASSWORD =====
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Đặt lại mật khẩu thành công."));
    }

    // ===== LOGOUT =====
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        // In a production environment, you would:
        // 1. Extract the JWT token from the Authorization header
        // 2. Add it to a blacklist in Redis with expiration matching the token's expiration
        // 3. JwtRequestFilter would check this blacklist before validating tokens
        
        // For now, we'll just acknowledge the logout request
        // The actual session clearing happens on the frontend
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            
        }
        
        return ResponseEntity.ok(Map.of("message", "Đăng xuất thành công"));
    }
}
