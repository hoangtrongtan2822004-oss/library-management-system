package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dto.ApiResponse;
import com.ibizabroker.lms.dto.MemberCardDto;
import com.ibizabroker.lms.dto.MemberCardRequest;
import com.ibizabroker.lms.entity.BarcodeType;
import com.ibizabroker.lms.entity.MemberCardStatus;
import com.ibizabroker.lms.service.MemberCardService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/member-cards")
@RequiredArgsConstructor
public class AdminMemberCardController {

    private final MemberCardService memberCardService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<MemberCardDto>> search(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) MemberCardStatus status,
        @RequestParam(required = false) BarcodeType barcodeType,
        @RequestParam(required = false) String from,
        @RequestParam(required = false) String to,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        LocalDateTime fromDate = parseDateTime(from);
        LocalDateTime toDate = parseDateTime(to);
        Page<MemberCardDto> result = memberCardService.search(
            keyword,
            status,
            barcodeType,
            fromDate,
            toDate,
            PageRequest.of(page, size)
        );
        return ApiResponse.success(result, "Danh sách thẻ");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<MemberCardDto> get(@PathVariable Long id) {
        return ApiResponse.success(memberCardService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<MemberCardDto> create(@Valid @RequestBody MemberCardRequest request) {
        return ApiResponse.success(memberCardService.create(request), "Tạo thẻ thành công");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<MemberCardDto> update(
        @PathVariable Long id,
        @Valid @RequestBody MemberCardRequest request
    ) {
        return ApiResponse.success(memberCardService.update(id, request), "Cập nhật thẻ thành công");
    }

    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<MemberCardDto> revoke(
        @PathVariable Long id,
        @RequestParam(required = false) String reason
    ) {
        return ApiResponse.success(memberCardService.revoke(id, reason), "Thu hồi thẻ");
    }

    @GetMapping("/{id}/barcode")
    @PreAuthorize("hasRole('ADMIN')")
    @SuppressWarnings("null") // MediaType constants lack @NonNull; usage is safe
    public ResponseEntity<byte[]> downloadBarcode(
        @PathVariable Long id,
        @RequestParam(defaultValue = "300") int width,
        @RequestParam(defaultValue = "100") int height
    ) throws IOException {
        byte[] data = memberCardService.generateBarcodePng(id, width, height);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=member-card-" + id + ".png")
            .contentType(MediaType.IMAGE_PNG)
            .body(data);
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    @SuppressWarnings("null") // MediaType constants lack @NonNull; usage is safe
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) throws IOException {
        byte[] data = memberCardService.generateCardPdf(id);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=member-card-" + id + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(data);
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException ex) {
            try {
                return LocalDateTime.parse(value + "T00:00:00");
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }
}
