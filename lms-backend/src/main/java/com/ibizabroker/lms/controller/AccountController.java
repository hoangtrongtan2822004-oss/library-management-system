package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dao.UsersRepository;
import com.ibizabroker.lms.dto.ChangePasswordRequest;
import com.ibizabroker.lms.dto.UserDto;
import com.ibizabroker.lms.entity.Users;
import com.ibizabroker.lms.exceptions.NotFoundException;
import com.ibizabroker.lms.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/account")
@CrossOrigin("http://localhost:4200/")
@RequiredArgsConstructor
public class AccountController {

    private final UsersRepository usersRepository;
    private final UserService userService;

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> getProfile(@AuthenticationPrincipal UserDetails principal) {
        Users currentUser = usersRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found for token"));
        return ResponseEntity.ok(userService.mapToUserDto(currentUser));
    }

    @PutMapping("/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal UserDetails principal,
                                            @Valid @RequestBody ChangePasswordRequest request) {
        Users currentUser = usersRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found for token"));
        try {
            userService.changeOwnPassword(currentUser, request);
            return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", ex.getMessage()));
        }
    }
}
