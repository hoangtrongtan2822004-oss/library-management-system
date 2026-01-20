package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 🎯 Daily Quests Response DTO
 * 
 * Dùng cho endpoint GET /api/user/gamification/daily-quests
 * 
 * 📌 Daily Quest System:
 * - Mỗi ngày có nhiệm vụ mới
 * - Hoàn thành để nhận điểm thưởng
 * - Reset vào 00:00 hàng ngày
 * 
 * 👉 Pattern: Response-Only DTO with Nested DTO
 * - Chỉ dùng cho response (output)
 * - Nested DailyQuestDTO cho từng nhiệm vụ
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DailyQuestsResponse {
    private List<DailyQuestDTO> quests;

    /**
     * 🎯 Nested DTO cho từng Daily Quest
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DailyQuestDTO {
        private Long id;
        private String title;
        private String description;
        private Integer points;
        private Boolean completed;
        private Integer progress;
        private Integer target;
    }
}
