package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dao.UsersRepository;
// Using typed request/response DTOs: EbookCreateRequest, EbookResponse
import com.ibizabroker.lms.entity.Ebook;
import com.ibizabroker.lms.entity.EbookDownload;
import com.ibizabroker.lms.entity.Users;
import com.ibizabroker.lms.service.EbookService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Feature 5: E-book Management Controller
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EbookController {

    private final EbookService ebookService;
    private final UsersRepository usersRepository;

    // ============ PUBLIC ENDPOINTS ============

    @GetMapping("/public/ebooks")
    public ResponseEntity<Page<Ebook>> getPublicEbooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ebookService.getPublicEbooks(pageable));
    }

    @GetMapping("/public/ebooks/{id}")
    public ResponseEntity<Ebook> getEbookById(@PathVariable Long id) {
        ebookService.incrementViewCount(id);
        return ResponseEntity.ok(ebookService.getEbookById(id));
    }

    @GetMapping("/public/ebooks/search")
    public ResponseEntity<Page<Ebook>> searchEbooks(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String fileType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ebookService.searchEbooks(search, fileType, true, pageable));
    }

    @GetMapping("/public/ebooks/top")
    public ResponseEntity<List<Ebook>> getTopDownloaded(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ebookService.getTopDownloaded(limit));
    }

    @GetMapping("/public/ebooks/newest")
    public ResponseEntity<List<Ebook>> getNewestEbooks(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ebookService.getNewestEbooks(limit));
    }

    // ============ USER ENDPOINTS ============

    @GetMapping("/user/ebooks/{id}/can-download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Boolean>> canDownload(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Integer userId = getCurrentUserId(userDetails);
        boolean canDownload = ebookService.canUserDownload(id, userId);
        return ResponseEntity.ok(Map.of("canDownload", canDownload));
    }

    @SuppressWarnings("null")
    @GetMapping("/user/ebooks/{id}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadEbook(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) throws IOException {
        
        Integer userId = getCurrentUserId(userDetails);
        String ipAddress = request.getRemoteAddr();
        
        // Record download
        ebookService.recordDownload(id, userId, ipAddress);
        
        // Get file
        Ebook ebook = ebookService.getEbookById(id);
        Path filePath = Paths.get(ebook.getFilePath());
        Resource resource = new UrlResource(filePath.toUri());
        
        if (!resource.exists()) {
            throw new RuntimeException("File không tồn tại");
        }
        
        String contentType = "application/octet-stream";
        String filename = ebook.getTitle().replaceAll("[^a-zA-Z0-9.-]", "_") + "." + ebook.getFileType().toLowerCase();
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @GetMapping("/user/ebooks/my-downloads")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EbookDownload>> getMyDownloads(@AuthenticationPrincipal UserDetails userDetails) {
        Integer userId = getCurrentUserId(userDetails);
        return ResponseEntity.ok(ebookService.getUserDownloadHistory(userId));
    }

    // ============ ADMIN ENDPOINTS ============

    @GetMapping("/admin/ebooks")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Ebook>> getAllEbooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ebookService.getAllEbooks(pageable));
    }

    @PostMapping(value = "/admin/ebooks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.ibizabroker.lms.dto.EbookResponse> uploadEbook(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "author", required = false) String author,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "coverUrl", required = false) String coverUrl,
            @RequestParam(value = "isPublic", defaultValue = "true") Boolean isPublic,
            @RequestParam(value = "maxDownloadsPerUser", defaultValue = "3") Integer maxDownloadsPerUser,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        
        Integer uploaderId = getCurrentUserId(userDetails);
        
        com.ibizabroker.lms.dto.EbookCreateRequest dto = com.ibizabroker.lms.dto.EbookCreateRequest.builder()
            .title(title)
            .author(author)
            .description(description)
            .coverUrl(coverUrl)
            .isPublic(isPublic)
            .maxDownloadsPerUser(maxDownloadsPerUser)
            .build();

        Ebook created = ebookService.uploadEbook(file, dto, uploaderId);
        com.ibizabroker.lms.dto.EbookResponse resp = com.ibizabroker.lms.dto.EbookResponse.builder()
            .id(created.getId())
            .bookId(created.getBook() != null ? created.getBook().getId() : null)
            .title(created.getTitle())
            .author(created.getAuthor())
            .description(created.getDescription())
            .coverUrl(created.getCoverUrl())
            .isPublic(created.getIsPublic())
            .maxDownloadsPerUser(created.getMaxDownloadsPerUser())
            .format(created.getFileType())
            .fileSize(created.getFileSize())
            .downloadCount(created.getDownloadCount())
            .build();

        return ResponseEntity.ok().body(resp);
    }

    @PutMapping("/admin/ebooks/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.ibizabroker.lms.dto.EbookResponse> updateEbook(
            @PathVariable Long id,
            @Valid @RequestBody com.ibizabroker.lms.dto.EbookCreateRequest dto) {
        Ebook updated = ebookService.updateEbook(id, dto);
        com.ibizabroker.lms.dto.EbookResponse resp = com.ibizabroker.lms.dto.EbookResponse.builder()
                .id(updated.getId())
                .bookId(updated.getBook() != null ? updated.getBook().getId() : null)
                .title(updated.getTitle())
                .author(updated.getAuthor())
                .description(updated.getDescription())
                .coverUrl(updated.getCoverUrl())
                .isPublic(updated.getIsPublic())
                .maxDownloadsPerUser(updated.getMaxDownloadsPerUser())
                .format(updated.getFileType())
                .fileSize(updated.getFileSize())
                .downloadCount(updated.getDownloadCount())
                .build();
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/admin/ebooks/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteEbook(@PathVariable Long id) throws IOException {
        ebookService.deleteEbook(id);
        return ResponseEntity.ok(Map.of("message", "Xóa ebook thành công"));
    }

    // Helper method to get current user ID
    private Integer getCurrentUserId(UserDetails userDetails) {
        Users user = usersRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        return user.getUserId();
    }
}
