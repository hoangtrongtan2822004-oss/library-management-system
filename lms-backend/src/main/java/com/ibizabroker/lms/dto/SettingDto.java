package com.ibizabroker.lms.dto;

import com.ibizabroker.lms.entity.SettingCategory;
import com.ibizabroker.lms.entity.SettingDataType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettingDto {
    private String key;
    private String value;
    private String defaultValue;
    private String description;
    private SettingCategory category;
    private SettingDataType dataType;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
