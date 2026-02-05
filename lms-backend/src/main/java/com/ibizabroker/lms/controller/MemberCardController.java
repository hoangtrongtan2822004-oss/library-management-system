package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dto.ApiResponse;
import com.ibizabroker.lms.dto.MemberCardRequest;
import com.ibizabroker.lms.dto.MemberCardResponse;
import com.ibizabroker.lms.entity.MemberCard;
import com.ibizabroker.lms.service.MemberCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/member-cards")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MemberCardController {

    private final MemberCardService memberCardService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<MemberCardResponse>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) MemberCard.MemberCardStatus status,
            @RequestParam(required = false) MemberCard.BarcodeType barcodeType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(sort = "issuedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<MemberCardResponse> page = memberCardService.search(keyword, status, barcodeType, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Lấy danh sách thẻ thành công"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MemberCardResponse>> get(@PathVariable Long id) {
        MemberCardResponse response = memberCardService.get(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin thẻ thành công"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MemberCardResponse>> create(@RequestBody @Valid MemberCardRequest request) {
        MemberCardResponse response = memberCardService.create(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Tạo thẻ thành công"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MemberCardResponse>> update(
            @PathVariable Long id,
            @RequestBody @Valid MemberCardRequest request
    ) {
        MemberCardResponse response = memberCardService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật thẻ thành công"));
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<ApiResponse<MemberCardResponse>> revoke(
            @PathVariable Long id,
            @RequestParam(required = false) String reason
    ) {
        MemberCardResponse response = memberCardService.revoke(id, reason);
        return ResponseEntity.ok(ApiResponse.success(response, "Thu hồi thẻ thành công"));
    }

    @GetMapping(value = "/{id}/barcode", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> barcode(
            @PathVariable Long id,
            @RequestParam(required = false) Integer width,
            @RequestParam(required = false) Integer height
    ) {
        int resolvedWidth = sanitizeDimension(width, 600, 80, 1600);
        int resolvedHeight = sanitizeDimension(height, 200, 60, 800);
        byte[] image = memberCardService.generateBarcodeImage(id, resolvedWidth, resolvedHeight);

        String filename = "member-card-" + memberCardService.get(id).getCardNumber() + ".png";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDispositionInline(filename))
                .contentType(MediaType.IMAGE_PNG)
                .body(image);
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        MemberCardResponse card = memberCardService.get(id);
        byte[] pdf = memberCardService.generatePdf(id);
        String filename = "member-card-" + card.getCardNumber() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDispositionInline(filename))
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private String contentDispositionInline(String filename) {
        String encoded = java.net.URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return "inline; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded;
    }

    private int sanitizeDimension(Integer value, int defaultValue, int min, int max) {
        if (value == null) {
            return defaultValue;
        }
        int v = Math.max(min, Math.min(max, value));
        return v;
    }
}
