package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dao.RoleRepository;
import com.ibizabroker.lms.dao.UsersRepository;
import com.ibizabroker.lms.entity.Role;
import com.ibizabroker.lms.entity.Users;
import com.ibizabroker.lms.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/auth")
public class OAuth2LoginController {

    private final UsersRepository usersRepository;
    private final RoleRepository roleRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public OAuth2LoginController(UsersRepository usersRepository, RoleRepository roleRepository, 
                                JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.usersRepository = usersRepository;
        this.roleRepository = roleRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Endpoint nhận Google OAuth token từ frontend, tạo/update user và trả về JWT
     * Frontend sẽ gọi Google OAuth trực tiếp, sau đó gửi idToken lên đây
     */
    @PostMapping("/google")
    public ResponseEntity<Map<String, Object>> googleLogin(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String name = payload.get("name");
        String googleId = payload.get("googleId");
        String picture = payload.get("picture");

        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email không hợp lệ"));
        }

        String normalizedEmail = email.trim().toLowerCase();
        String normalizedGoogleId = googleId != null ? googleId.trim() : null;

        Optional<Users> googleMatchedUser = hasText(normalizedGoogleId)
                ? usersRepository.findByGoogleId(normalizedGoogleId)
                : Optional.empty();

        Optional<Users> emailMatchedUser = usersRepository.findFirstByEmailIgnoreCaseOrderByUserIdAsc(normalizedEmail)
                .or(() -> usersRepository.findByUsernameWithRolesIgnoreCase(normalizedEmail));

        // Ưu tiên user đã map với googleId để tránh vi phạm unique key
        Users user = googleMatchedUser.orElseGet(() -> emailMatchedUser.orElseGet(() -> {
            Users newUser = new Users();
            newUser.setUsername(generateUniqueUsername(normalizedEmail));
            newUser.setEmail(normalizedEmail);
            newUser.setName(name != null && !name.isBlank() ? name : normalizedEmail);

            // Set random password (user không cần biết, chỉ dùng OAuth)
            newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

            // Default role USER - lấy từ database
            Role userRole = roleRepository.findByRoleName("ROLE_USER")
                    .orElseThrow(() -> new RuntimeException("Role ROLE_USER không tồn tại trong database"));
            newUser.addRole(userRole);

            if (hasText(normalizedGoogleId)) {
                newUser.setGoogleId(normalizedGoogleId);
            }
            newUser.setAvatar(picture);

            return usersRepository.save(newUser);
        }));

        // Nếu email match là user khác với googleId match thì dùng googleId match làm bản ghi chuẩn
        if (hasText(normalizedGoogleId)) {
            Optional<Users> existingGoogleOwner = usersRepository.findByGoogleId(normalizedGoogleId);
            if (existingGoogleOwner.isPresent() && !existingGoogleOwner.get().getUserId().equals(user.getUserId())) {
                user = existingGoogleOwner.get();
            }
        }

        // Cập nhật thông tin hồ sơ, nhưng tránh chạm unique key
        boolean changed = false;
        if (hasText(normalizedGoogleId) && (user.getGoogleId() == null || !normalizedGoogleId.equals(user.getGoogleId()))) {
            user.setGoogleId(normalizedGoogleId);
            changed = true;
        }

        if (picture != null && !picture.isBlank() && !picture.equals(user.getAvatar())) {
            user.setAvatar(picture);
            changed = true;
        }

        if ((user.getEmail() == null || user.getEmail().isBlank())) {
            user.setEmail(normalizedEmail);
            changed = true;
        }

        if ((user.getUsername() == null || user.getUsername().isBlank())) {
            user.setUsername(generateUniqueUsername(normalizedEmail));
            changed = true;
        }

        if ((user.getName() == null || user.getName().isBlank()) && name != null && !name.isBlank()) {
            user.setName(name);
            changed = true;
        }

        if (changed) {
            user = usersRepository.save(user);
        }

        // Generate JWT với proper UserDetails và claims
        List<String> roles = user.getRoles().stream()
                .map(Role::getRoleName)
                .collect(Collectors.toList());
        
        UserDetails userDetails = User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList()))
                .build();
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("roles", roles);
        
        String token = jwtUtil.generateToken(userDetails, claims);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("username", user.getUsername());
        response.put("name", user.getName());
        response.put("userId", user.getUserId());
        response.put("role", roles.isEmpty() ? "ROLE_USER" : roles.get(0));
        response.put("roles", roles);

        return ResponseEntity.ok(response);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String generateUniqueUsername(String baseUsername) {
        String candidate = baseUsername;
        int suffix = 1;

        while (usersRepository.existsByUsernameIgnoreCase(candidate)) {
            candidate = baseUsername + "_g" + suffix;
            suffix++;
        }

        return candidate;
    }
}
