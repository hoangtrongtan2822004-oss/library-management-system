package com.ibizabroker.lms.dto;

import com.ibizabroker.lms.entity.InventorySession;
import com.ibizabroker.lms.entity.InventorySessionStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventorySessionDto {
    private Long id;
    private String name;
    private InventorySessionStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer expectedTotal;
    private Integer scannedTotal;

    public static InventorySessionDto fromEntity(InventorySession session) {
        if (session == null) return null;
        return InventorySessionDto.builder()
            .id(session.getId())
            .name(session.getName())
            .status(session.getStatus())
            .startedAt(session.getStartedAt())
            .completedAt(session.getCompletedAt())
            .expectedTotal(session.getExpectedTotal())
            .scannedTotal(session.getScannedTotal())
            .build();
    }
}
