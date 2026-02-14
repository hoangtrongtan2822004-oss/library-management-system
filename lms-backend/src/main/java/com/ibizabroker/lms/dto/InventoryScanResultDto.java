package com.ibizabroker.lms.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventoryScanResultDto {
    private Long sessionId;
    private Integer bookId;
    private String bookName;
    private String isbn;
    private String shelfCode;
    private LocalDateTime scannedAt;
    private boolean duplicate;
    private boolean unknown;
}
