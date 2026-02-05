package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dao.NewsRepository;
import com.ibizabroker.lms.dao.UsersRepository;
import com.ibizabroker.lms.entity.News;
import com.ibizabroker.lms.entity.Users;
import com.ibizabroker.lms.dto.NewsDto;
import com.ibizabroker.lms.service.EmailService;
import com.ibizabroker.lms.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;

@CrossOrigin("http://localhost:4200/")
@RestController
@RequestMapping("/api/admin/news")
@PreAuthorize("hasRole('ROLE_ADMIN')")
@RequiredArgsConstructor
public class AdminNewsController {

    private final NewsRepository newsRepository;
    private final UsersRepository usersRepository;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;

    @Value("${file.upload-dir:uploads/news}")
    private String uploadDir;

    @GetMapping
    public List<NewsDto> getAll() {
        return newsRepository.findAllOrderByPinnedAndDate()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NewsDto create(@RequestBody News news,
                      @RequestParam(name = "notifyEmail", defaultValue = "false") boolean notifyEmail) {
        news.setId(null);
        
        // Set default status if not provided
        if (news.getStatus() == null) {
            news.setStatus(News.NewsStatus.PUBLISHED);
        }
        
        // If publishing immediately, set publishedAt to now
        if (news.getStatus() == News.NewsStatus.PUBLISHED && news.getPublishedAt() == null) {
            news.setPublishedAt(Instant.now());
        }
        
        News saved = newsRepository.save(news);
        
        if (notifyEmail && news.getStatus() == News.NewsStatus.PUBLISHED) {
            broadcastNews(saved);
        }
        return toDto(saved);
    }

    @PutMapping("/{id}")
    @SuppressWarnings("null")
    public ResponseEntity<NewsDto> update(@PathVariable Long id,
                                       @RequestBody News news,
                                       @RequestParam(name = "notifyEmail", defaultValue = "false") boolean notifyEmail) {
        return newsRepository.findById(id)
                .map(existing -> {
                    existing.setTitle(news.getTitle());
                    existing.setContent(news.getContent());
                    existing.setCoverImageUrl(news.getCoverImageUrl());
                    existing.setPinned(news.isPinned());
                    existing.setStatus(news.getStatus());
                    existing.setPublishedAt(news.getPublishedAt());
                    
                    News saved = newsRepository.save(existing);
                    
                    if (notifyEmail && saved.getStatus() == News.NewsStatus.PUBLISHED) {
                        broadcastNews(saved);
                    }
                    return ResponseEntity.ok(toDto(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @SuppressWarnings("null")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (newsRepository.existsById(id)) {
            newsRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/upload-image")
    public ResponseEntity<String> uploadImage(@RequestParam("image") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("No file provided");
        }

        try {
            // Create upload directory if not exists
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                throw new IllegalArgumentException("Đường dẫn file không hợp lệ");
            }
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = UUID.randomUUID().toString() + extension;
            
            // Save file
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath);

            // Return URL
            String imageUrl = "/uploads/news/" + filename;
            return ResponseEntity.ok(imageUrl);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload image: " + e.getMessage());
        }
    }

    @PostMapping("/preview-email")
    public ResponseEntity<String> previewEmail(@RequestBody News news) {
        String html = generateEmailHtml(news);
        return ResponseEntity.ok(html);
    }

    @PostMapping("/test-email")
    public ResponseEntity<String> sendTestEmail(@RequestBody News news,
                                                @RequestHeader("Authorization") String authHeader) {
        try {
            // Extract current user email from JWT
            String token = authHeader.substring(7);
            String username = jwtUtil.getUsernameFromToken(token);
            
            return usersRepository.findByUsername(username)
                    .map(user -> {
                        if (user.getEmail() == null || user.getEmail().isBlank()) {
                            return ResponseEntity.badRequest()
                                    .body("Tài khoản của bạn chưa có email");
                        }
                        
                        String subject = "[TEST] Tin tức mới: " + news.getTitle();
                        String html = generateEmailHtml(news);
                        
                        emailService.sendHtmlMessage(user.getEmail(), subject, html);
                        return ResponseEntity.ok("Đã gửi email thử nghiệm đến: " + user.getEmail());
                    })
                    .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body("Không tìm thấy thông tin người dùng"));
                    
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi gửi email: " + e.getMessage());
        }
    }

    private void broadcastNews(News news) {
        List<Users> usersWithEmail = usersRepository.findAll().stream()
                .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())
                .toList();

        String subject = "Tin tức mới: " + news.getTitle();
        String html = generateEmailHtml(news);

        usersWithEmail.forEach(u -> emailService.sendHtmlMessage(u.getEmail(), subject, html));
    }

    private String generateEmailHtml(News news) {
        StringBuilder html = new StringBuilder();
        html.append("<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>");
        html.append("<p>Chào bạn,</p>");
        html.append("<p>Có tin tức mới trên hệ thống thư viện:</p>");
        
        if (news.getCoverImageUrl() != null && !news.getCoverImageUrl().isBlank()) {
            html.append("<img src='http://localhost:8081")
                .append(news.getCoverImageUrl())
                .append("' style='max-width: 100%; height: auto; border-radius: 8px; margin: 16px 0;' />");
        }
        
        html.append("<h2 style='color: #2c3e50;'>").append(news.getTitle()).append("</h2>");
        html.append("<div style='line-height: 1.6; color: #34495e;'>").append(news.getContent()).append("</div>");
        html.append("<hr style='margin: 24px 0; border: none; border-top: 1px solid #ecf0f1;' />");
        html.append("<p style='color: #7f8c8d; font-size: 14px;'>Trân trọng,<br/>Thư viện LMS</p>");
        html.append("</div>");
        
        return html.toString();
    }

    private NewsDto toDto(News n) {
        return NewsDto.builder()
                .id(n.getId())
                .title(n.getTitle())
                .coverImageUrl(n.getCoverImageUrl())
                .publishedAt(n.getPublishedAt())
                .content(n.getContent())
                .pinned(n.isPinned())
                .status(n.getStatus() != null ? n.getStatus().name() : null)
                .createdAt(n.getCreatedAt())
                .build();
    }
}
