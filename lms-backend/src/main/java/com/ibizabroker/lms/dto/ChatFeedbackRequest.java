package com.ibizabroker.lms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatFeedbackRequest {
    @NotBlank
    private String conversationId;

    @Size(max = 64)
    private String messageId;

    @NotNull
    private Boolean helpful;

    @Size(max = 500)
    private String reason;

    public boolean isHelpful() {
        return Boolean.TRUE.equals(helpful);
    }
}
