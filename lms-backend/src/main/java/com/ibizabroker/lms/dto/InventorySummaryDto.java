package com.ibizabroker.lms.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventorySummaryDto {
    private Long sessionId;
    private Integer expectedTotal;
    private Integer scannedTotal;
    private Integer missingTotal;
    private Integer misplacedTotal;
    private Integer unknownTotal;
    private List<MissingItem> missingItems;
    private List<MisplacedItem> misplacedItems;
    private List<UnknownItem> unknownItems;

    public static InventorySummaryDto empty(Long sessionId) {
        return InventorySummaryDto.builder()
            .sessionId(sessionId)
            .expectedTotal(0)
            .scannedTotal(0)
            .missingTotal(0)
            .misplacedTotal(0)
            .unknownTotal(0)
            .missingItems(new ArrayList<>())
            .misplacedItems(new ArrayList<>())
            .unknownItems(new ArrayList<>())
            .build();
    }

    @Data
    @Builder
    public static class MissingItem {
        private Integer bookId;
        private String bookName;
        private String isbn;
        private String expectedShelfCode;
    }

    @Data
    @Builder
    public static class MisplacedItem {
        private Integer bookId;
        private String bookName;
        private String isbn;
        private String expectedShelfCode;
        private String scannedShelfCode;
    }

    @Data
    @Builder
    public static class UnknownItem {
        private String isbn;
        private String shelfCode;
    }
}
