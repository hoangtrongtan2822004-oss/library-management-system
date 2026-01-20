package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 📊 Point History Response DTO
 * 
 * Dùng cho endpoint GET /api/user/gamification/points/history
 * 
 * 📌 Point History Tracking:
 * - Lịch sử biến động điểm theo ngày
 * - Ghi nhận cả tăng và giảm điểm
 * - Lý do: mượn sách, trả đúng hạn, viết review, hoàn thành challenge
 * 
 * 👉 Pattern: Response-Only DTO with Nested DTO
 * - Chỉ dùng cho response (output)
 * - Sắp xếp theo date DESC (mới nhất trước)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PointHistoryResponse {
    private List<PointHistoryDTO> history;

    /**
     * 📊 Nested DTO cho từng entry trong lịch sử điểm
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PointHistoryDTO {
        private LocalDate date;
        private Integer points;  // Tổng điểm sau thay đổi
        private Integer change;  // Biến động (+10, -5)
        private String reason;   // Lý do: "Borrowed book", "Completed challenge"
    }
}
