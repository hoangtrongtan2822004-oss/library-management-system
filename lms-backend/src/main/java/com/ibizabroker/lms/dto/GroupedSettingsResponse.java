package com.ibizabroker.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupedSettingsResponse {
    private Map<String, CategoryGroup> groups;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryGroup {
        private String displayName;
        private String icon;
        private List<SettingDto> settings;
    }
}
