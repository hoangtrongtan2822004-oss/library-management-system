package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 🏆 Leaderboard Entry Response DTO
 * 
 * Dùng cho endpoint GET /api/public/gamification/leaderboard
 * 
 * 📌 Leaderboard Fields:
 * - userId: ID người dùng
 * - userName: Tên người dùng
 * - totalPoints: Tổng điểm tích lũy
 * - level: Cấp độ hiện tại
 * - badgesCount: Số huy hiệu đã đạt
 * 
 * 👉 Pattern: Read-Only Response DTO
 * - Chỉ dùng cho response (output)
 * - Sắp xếp theo totalPoints DESC
 * - Tính toán từ User + Gamification tables
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LeaderboardEntryDto {
    private Integer userId;
    private String userName;
    private Integer totalPoints;
    private Integer level;
    private Integer badgesCount;
}
