package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 🏅 Badge (Huy hiệu) DTO (Hybrid - Request & Response)
 * 
 * Dùng cho:
 * - POST /api/admin/gamification/badges (Request)
 * - GET /api/public/gamification/badges (Response)
 * 
 * 📌 Validation Rules (Request):
 * - code: Bắt buộc (unique identifier: "first_book", "speed_reader")
 * - nameVi, nameEn: Bắt buộc (bilingual support)
 * - pointsReward: Optional, >= 0 nếu có
 * 
 * 📌 Badge Fields:
 * - code: Mã huy hiệu (unique)
 * - nameVi/nameEn: Tên song ngữ
 * - descriptionVi/descriptionEn: Mô tả song ngữ
 * - iconUrl: Link icon huy hiệu
 * - pointsReward: Số điểm thưởng khi đạt
 * - category: Loại (milestone, achievement, special)
 * - requirementValue: Giá trị yêu cầu (đọc 10 quyển, trả đúng hạn 30 lần)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BadgeDto {

    @NotEmpty(message = "Mã huy hiệu không được để trống")
    private String code;

    @NotEmpty(message = "Tên huy hiệu (VI) không được để trống")
    private String nameVi;

    @NotEmpty(message = "Tên huy hiệu (EN) không được để trống")
    private String nameEn;

    private String descriptionVi;
    private String descriptionEn;
    private String iconUrl;
    
    @Min(value = 0, message = "Điểm thưởng không được âm")
    private Integer pointsReward;
    
    private String category;
    
    @Min(value = 1, message = "Giá trị yêu cầu phải >= 1")
    private Integer requirementValue;
}
