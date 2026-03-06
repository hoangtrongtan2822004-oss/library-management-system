package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dao.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public endpoints — no authentication required.
 * Security config already permits /api/public/** for all.
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final UsersRepository usersRepository;

    /**
     * Check if an email is already registered.
     * Used by the signup form for async email validation.
     *
     * GET /api/public/check-email?email=foo@bar.com
     * Response: true  → email already exists
     *           false → email is available
     */
    @GetMapping("/check-email")
    public ResponseEntity<Boolean> checkEmailExists(@RequestParam String email) {
        boolean exists = usersRepository.existsByEmailIgnoreCase(email.trim());
        return ResponseEntity.ok(exists);
    }
}
