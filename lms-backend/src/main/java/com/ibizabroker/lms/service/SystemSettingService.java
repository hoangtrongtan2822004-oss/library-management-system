package com.ibizabroker.lms.service;

import com.ibizabroker.lms.dao.SystemSettingRepository;
import com.ibizabroker.lms.dto.GroupedSettingsResponse;
import com.ibizabroker.lms.dto.SettingDto;
import com.ibizabroker.lms.entity.SettingCategory;
import com.ibizabroker.lms.entity.SystemSetting;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SystemSettingService {

    public static final String KEY_LOAN_MAX_DAYS = "LOAN_MAX_DAYS";
    public static final String KEY_FINE_PER_DAY = "FINE_PER_DAY";

    private final SystemSettingRepository repo;

    @Transactional(readOnly = true)
    @Cacheable(value = "system-settings", key = "'all'")
    public List<SystemSetting> findAll() {
        return repo.findAll();
    }

    /**
     * Get settings grouped by category with metadata
     */
    @Transactional(readOnly = true)
    public GroupedSettingsResponse getGroupedSettings() {
        List<SystemSetting> allSettings = repo.findAll();
        
        Map<String, GroupedSettingsResponse.CategoryGroup> groups = new LinkedHashMap<>();
        
        // Group by category
        Map<SettingCategory, List<SystemSetting>> byCategory = allSettings.stream()
                .collect(Collectors.groupingBy(SystemSetting::getCategory));
        
        for (SettingCategory category : SettingCategory.values()) {
            List<SystemSetting> categorySettings = byCategory.getOrDefault(category, new ArrayList<>());
            
            List<SettingDto> dtos = categorySettings.stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
            
            String icon = getIconForCategory(category);
            groups.put(category.name(), new GroupedSettingsResponse.CategoryGroup(
                    category.getDisplayName(),
                    icon,
                    dtos
            ));
        }
        
        return new GroupedSettingsResponse(groups);
    }

    private String getIconForCategory(SettingCategory category) {
        return switch (category) {
            case LOAN_POLICY -> "📘";
            case EMAIL_NOTIFICATION -> "📧";
            case SYSTEM -> "⚙️";
        };
    }

    private SettingDto toDto(SystemSetting setting) {
        return new SettingDto(
                setting.getKey(),
                setting.getValue(),
                setting.getDefaultValue(),
                setting.getDescription(),
                setting.getCategory(),
                setting.getDataType(),
                setting.getUpdatedBy(),
                setting.getUpdatedAt()
        );
    }

    @CacheEvict(value = "system-settings", allEntries = true)
    public SystemSetting upsert(String key, String value) {
        Optional<SystemSetting> existing = repo.findByKeyIgnoreCase(key);
        SystemSetting s = existing.orElseGet(() -> new SystemSetting(key, value));
        s.setKey(key);
        s.setValue(value);
        return repo.save(s);
    }

    @CacheEvict(value = "system-settings", allEntries = true)
    public SystemSetting upsert(String key, String value, String updatedBy) {
        Optional<SystemSetting> existing = repo.findByKeyIgnoreCase(key);
        SystemSetting s = existing.orElseGet(() -> new SystemSetting(key, value));
        s.setKey(key);
        s.setValue(value);
        s.setUpdatedBy(updatedBy);
        return repo.save(s);
    }

    /**
     * Reset a setting to its default value
     */
    @CacheEvict(value = "system-settings", allEntries = true)
    public SystemSetting resetToDefault(String key, String updatedBy) {
        SystemSetting setting = repo.findByKeyIgnoreCase(key)
                .orElseThrow(() -> new IllegalArgumentException("Setting not found: " + key));
        
        if (setting.getDefaultValue() == null) {
            throw new IllegalStateException("No default value defined for: " + key);
        }
        
        setting.setValue(setting.getDefaultValue());
        setting.setUpdatedBy(updatedBy);
        return repo.save(setting);
    }

    /**
     * Reset all settings in a category to default values
     */
    @CacheEvict(value = "system-settings", allEntries = true)
    public int resetCategoryToDefaults(SettingCategory category, String updatedBy) {
        List<SystemSetting> categorySettings = repo.findAll().stream()
                .filter(s -> s.getCategory() == category)
                .filter(s -> s.getDefaultValue() != null)
                .toList();
        
        for (SystemSetting setting : categorySettings) {
            setting.setValue(setting.getDefaultValue());
            setting.setUpdatedBy(updatedBy);
            repo.save(setting);
        }
        
        return categorySettings.size();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "system-settings", key = "'int:' + #key")
    public int getInt(String key, int defaultValue) {
        return repo.findByKeyIgnoreCase(key)
                .map(SystemSetting::getValue)
                .map(val -> {
                    try { return Integer.parseInt(val.trim()); } catch (Exception e) { return defaultValue; }
                })
                .orElse(defaultValue);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "system-settings", key = "'decimal:' + #key")
    public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        return repo.findByKeyIgnoreCase(key)
                .map(SystemSetting::getValue)
                .map(val -> {
                    try { return new BigDecimal(val.trim()); } catch (Exception e) { return defaultValue; }
                })
                .orElse(defaultValue);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "system-settings", key = "'bool:' + #key")
    public boolean getBoolean(String key, boolean defaultValue) {
        return repo.findByKeyIgnoreCase(key)
                .map(SystemSetting::getValue)
                .map(val -> "true".equalsIgnoreCase(val.trim()) || "1".equals(val.trim()))
                .orElse(defaultValue);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "system-settings", key = "'string:' + #key")
    public String getString(String key, String defaultValue) {
        return repo.findByKeyIgnoreCase(key)
                .map(SystemSetting::getValue)
                .orElse(defaultValue);
    }
}
