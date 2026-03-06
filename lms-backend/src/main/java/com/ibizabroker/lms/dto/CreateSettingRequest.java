package com.ibizabroker.lms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateSettingRequest {

    /** Khóa cấu hình, ví dụ: MY_CUSTOM_KEY (sẽ tự động uppercase) */
    @NotBlank(message = "Khóa cấu hình không được để trống")
    private String key;

    /** Giá trị hiện tại */
    @NotBlank(message = "Giá trị không được để trống")
    private String value;

    /** Giá trị mặc định (tuỳ chọn) */
    private String defaultValue;

    /** Mô tả cho người dùng hiểu (tuỳ chọn) */
    private String description;

    /** Danh mục: LOAN_POLICY | EMAIL_NOTIFICATION | SYSTEM */
    private String category;

    /** Kiểu dữ liệu: NUMBER | BOOLEAN | TEXT | TEXTAREA | TIME */
    private String dataType;
}
