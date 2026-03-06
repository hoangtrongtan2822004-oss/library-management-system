package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dto.CreateSettingRequest;
import com.ibizabroker.lms.dto.GroupedSettingsResponse;
import com.ibizabroker.lms.entity.SettingCategory;
import com.ibizabroker.lms.entity.SystemSetting;
import com.ibizabroker.lms.service.SystemSettingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
@CrossOrigin("http://localhost:4200/")
@PreAuthorize("hasRole('ROLE_ADMIN')")
@RequiredArgsConstructor
public class AdminSettingsController {

    private final SystemSettingService settingsService;

    @GetMapping
    public ResponseEntity<List<SystemSetting>> list() {
        return ResponseEntity.ok(settingsService.findAll());
    }

    /**
     * Tạo mới một cấu hình động.
     * Trả về 409 nếu key đã tồn tại.
     */
    @PostMapping
    public ResponseEntity<?> create(
            @Valid @RequestBody CreateSettingRequest body,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String username = userDetails != null ? userDetails.getUsername() : "SYSTEM";
            SystemSetting s = settingsService.createSetting(body, username);
            return ResponseEntity.status(HttpStatus.CREATED).body(s);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Get settings grouped by category with metadata
     */
    @GetMapping("/grouped")
    public ResponseEntity<GroupedSettingsResponse> getGrouped() {
        return ResponseEntity.ok(settingsService.getGroupedSettings());
    }

    public static class UpdateSettingRequest {
        @NotBlank
        public String value;
    }

    @PutMapping("/{key}")
    public ResponseEntity<SystemSetting> update(
            @PathVariable String key, 
            @RequestBody UpdateSettingRequest body,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String username = userDetails != null ? userDetails.getUsername() : "SYSTEM";
        SystemSetting s = settingsService.upsert(key, body.value, username);
        return ResponseEntity.ok(s);
    }

    /**
     * Reset a single setting to its default value
     */
    @PostMapping("/{key}/reset")
    public ResponseEntity<SystemSetting> resetToDefault(
            @PathVariable String key,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String username = userDetails != null ? userDetails.getUsername() : "SYSTEM";
        SystemSetting s = settingsService.resetToDefault(key, username);
        return ResponseEntity.ok(s);
    }

    /**
     * Reset all settings in a category to default values
     */
    @PostMapping("/reset-category/{category}")
    public ResponseEntity<Map<String, Object>> resetCategoryToDefaults(
            @PathVariable String category,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            SettingCategory cat = SettingCategory.valueOf(category.toUpperCase());
            String username = userDetails != null ? userDetails.getUsername() : "SYSTEM";
            int count = settingsService.resetCategoryToDefaults(cat, username);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã khôi phục " + count + " thiết lập về giá trị mặc định",
                    "count", count
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Danh mục không hợp lệ: " + category
            ));
        }
    }
}
