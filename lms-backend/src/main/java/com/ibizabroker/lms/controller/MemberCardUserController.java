package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dao.UsersRepository;
import com.ibizabroker.lms.dto.ApiResponse;
import com.ibizabroker.lms.dto.MemberCardResponse;
import com.ibizabroker.lms.entity.Users;
import com.ibizabroker.lms.exceptions.NotFoundException;
import com.ibizabroker.lms.service.MemberCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/user/member-card")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MemberCardUserController {

    private final MemberCardService memberCardService;
    private final UsersRepository usersRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<MemberCardResponse>> myCard(Authentication authentication) {
        Integer userId = extractUserId(authentication);
        MemberCardResponse card = memberCardService.getOrCreateForUser(userId);
        return ResponseEntity.ok(ApiResponse.success(card, "Lấy thẻ thành công"));
    }

    @GetMapping(value = "/barcode", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> barcode(
            Authentication authentication,
            @RequestParam(required = false) Integer width,
            @RequestParam(required = false) Integer height
    ) {
        Integer userId = extractUserId(authentication);
        int resolvedWidth = sanitizeDimension(width, 600, 80, 1600);
        int resolvedHeight = sanitizeDimension(height, 200, 60, 800);
        byte[] image = memberCardService.generateBarcodeForUser(userId, resolvedWidth, resolvedHeight);

        String filename = "my-member-card.png";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDispositionInline(filename))
                .contentType(MediaType.IMAGE_PNG)
                .body(image);
    }

    private Integer extractUserId(Authentication authentication) {
        String username = authentication.getName();
        return usersRepository.findByUsernameIgnoreCase(username)
                .map(Users::getUserId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
    }

    private String contentDispositionInline(String filename) {
        String encoded = java.net.URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return "inline; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded;
    }

    private int sanitizeDimension(Integer value, int defaultValue, int min, int max) {
        if (value == null) {
            return defaultValue;
        }
        return Math.max(min, Math.min(max, value));
    }
}
