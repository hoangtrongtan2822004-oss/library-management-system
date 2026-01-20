package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 🏆 Gamification Statistics Response DTO
 * 
 * Dùng cho endpoint GET /api/user/gamification/stats
 * 
 * 📌 Gamification Metrics:
 * - totalPoints: Tổng điểm tích lũy
 * - currentLevel: Cấp độ hiện tại
 * - rank: Xếp hạng trong hệ thống
 * - badgesCount: Số huy hiệu đã đạt
 * - booksBorrowedCount: Số sách đã mượn
 * - booksReturnedOnTime: Số sách trả đúng hạn
 * - reviewsWritten: Số đánh giá đã viết
 * - streakDays: Số ngày liên tiếp có hoạt động
 * - activeChallenges: Số thử thách đang tham gia
 * - completedChallenges: Số thử thách đã hoàn thành
 * 
 * 👉 Pattern: Read-Only Response DTO
 * - Chỉ dùng cho response (output)
 * - Tính toán từ nhiều bảng (User, Loan, Review, Badge, Challenge)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GamificationStatsDto {
    private Integer totalPoints;
    private Integer currentLevel;
    private Integer rank;
    private Integer badgesCount;
    private Integer booksBorrowedCount;
    private Integer booksReturnedOnTime;
    private Integer reviewsWritten;
    private Integer streakDays;
    private Long activeChallenges;
    private Long completedChallenges;
}
