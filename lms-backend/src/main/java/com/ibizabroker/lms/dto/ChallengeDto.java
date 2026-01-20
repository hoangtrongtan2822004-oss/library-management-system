package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 🎯 Challenge (Thử thách) DTO (Hybrid - Request & Response)
 * 
 * Dùng cho:
 * - POST /api/admin/gamification/challenges (Request)
 * - GET /api/public/gamification/challenges (Response)
 * 
 * 📌 Validation Rules (Request):
 * - nameVi, nameEn: Bắt buộc (bilingual support)
 * - targetBooks: Bắt buộc, >= 1
 * - startDate, endDate: Bắt buộc
 * - pointsReward: Optional, >= 0 nếu có
 * 
 * 📌 Challenge Fields:
 * - nameVi/nameEn: Tên thử thách song ngữ
 * - descriptionVi/descriptionEn: Mô tả song ngữ
 * - targetBooks: Số sách mục tiêu
 * - startDate, endDate: Thời gian thử thách
 * - pointsReward: Số điểm thưởng khi hoàn thành
 * - badgeId: Huy hiệu nhận được (optional)
 * 
 * 🎯 Examples:
 * - "Read 10 books in 30 days" (targetBooks=10, duration=30 days)
 * - "Summer Reading Challenge" (seasonal)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChallengeDto {

    @NotEmpty(message = "Tên thử thách (VI) không được để trống")
    private String nameVi;

    @NotEmpty(message = "Tên thử thách (EN) không được để trống")
    private String nameEn;

    private String descriptionVi;
    private String descriptionEn;

    @NotNull(message = "Số sách mục tiêu không được để trống")
    @Min(value = 1, message = "Số sách mục tiêu phải >= 1")
    private Integer targetBooks;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDate endDate;

    @Min(value = 0, message = "Điểm thưởng không được âm")
    private Integer pointsReward;
    
    private Long badgeId;
}
