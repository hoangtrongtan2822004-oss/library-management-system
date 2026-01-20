package com.ibizabroker.lms.entity;

public enum SettingDataType {
    NUMBER("Số"),
    BOOLEAN("Bật/Tắt"),
    TEXT("Văn bản ngắn"),
    TEXTAREA("Văn bản dài"),
    TIME("Giờ");

    private final String displayName;

    SettingDataType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
