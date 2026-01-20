package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dao.UsersRepository;
import com.ibizabroker.lms.dto.GroupedSettingsResponse;
import com.ibizabroker.lms.entity.SettingCategory;
import com.ibizabroker.lms.entity.SystemSetting;
import com.ibizabroker.lms.entity.Users;
import com.ibizabroker.lms.exceptions.NotFoundException;
import com.ibizabroker.lms.service.SystemSettingService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
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
    private final UsersRepository usersRepository;

    @GetMapping
    public ResponseEntity<List<SystemSetting>> list() {
        return ResponseEntity.ok(settingsService.findAll());
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
