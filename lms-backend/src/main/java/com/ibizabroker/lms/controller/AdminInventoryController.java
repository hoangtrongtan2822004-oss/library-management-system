package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dto.ApiResponse;
import com.ibizabroker.lms.dto.InventoryScanRequest;
import com.ibizabroker.lms.dto.InventoryScanResultDto;
import com.ibizabroker.lms.dto.InventorySessionDto;
import com.ibizabroker.lms.dto.InventorySummaryDto;
import com.ibizabroker.lms.service.InventoryService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
public class AdminInventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/sessions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InventorySessionDto>> startSession(
        @RequestBody(required = false) InventorySessionDto payload
    ) {
        String name = payload != null ? payload.getName() : null;
        Integer expectedTotal = payload != null ? payload.getExpectedTotal() : null;
        InventorySessionDto session = inventoryService.startSession(name, expectedTotal);
        return ResponseEntity.ok(ApiResponse.success(session, "Bắt đầu kiểm kê"));
    }

    @PostMapping("/sessions/{sessionId}/scan")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InventoryScanResultDto>> recordScan(
        @PathVariable Long sessionId,
        @Valid @RequestBody InventoryScanRequest request
    ) {
        InventoryScanResultDto result = inventoryService.recordScan(sessionId, request);
        return ResponseEntity.ok(ApiResponse.success(result, "Đã ghi nhận quét"));
    }

    @PostMapping("/sessions/{sessionId}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InventorySessionDto>> completeSession(@PathVariable Long sessionId) {
        InventorySessionDto session = inventoryService.completeSession(sessionId);
        return ResponseEntity.ok(ApiResponse.success(session, "Kết thúc kiểm kê"));
    }

    @GetMapping("/sessions/{sessionId}/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InventorySummaryDto>> getSummary(@PathVariable Long sessionId) {
        InventorySummaryDto summary = inventoryService.buildSummary(sessionId);
        return ResponseEntity.ok(ApiResponse.success(summary, "Báo cáo kiểm kê"));
    }

    @GetMapping("/sessions/{sessionId}/export/excel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportExcel(@PathVariable Long sessionId) throws IOException {
        byte[] data = inventoryService.exportSummaryExcel(sessionId);
        String filename = "inventory-report-" + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(data);
    }
}
