package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.entity.JwtRequest;
import com.ibizabroker.lms.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@RequestMapping("/api/auth")
public class JwtController {

    private final JwtService jwtService;
    public JwtController(JwtService jwtService){ this.jwtService = jwtService; }

    // JwtController.java
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody JwtRequest req) {
  try {
    String token = jwtService.loginAndIssueToken(req.getUsername(), req.getPassword());
    return ResponseEntity.ok(java.util.Map.of("token", token));
  } catch (BadCredentialsException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
  } catch (Exception ex) {
    ex.printStackTrace(); // sẽ in "Caused by:" cụ thể (secret ngắn? thiếu jjwt-impl? ...)
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
  }
}
}
