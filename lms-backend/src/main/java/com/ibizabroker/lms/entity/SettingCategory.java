package com.ibizabroker.lms.entity;

public enum SettingCategory {
    LOAN_POLICY("📘 Chính sách mượn trả"),
    EMAIL_NOTIFICATION("📧 Email & Thông báo"),
    SYSTEM("⚙️ Hệ thống");

    private final String displayName;

    SettingCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
