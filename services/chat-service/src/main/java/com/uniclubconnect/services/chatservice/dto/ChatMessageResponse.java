package com.uniclubconnect.services.chatservice.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ChatMessageResponse {
    private String id;
    private String senderId;
    private String recipientId;
    private String content;
    private String status;
    private LocalDateTime timestamp;
    private String clientMessageId;

    // ✅ YENİ
    @Builder.Default
    private boolean isEdited = false;

    private LocalDateTime editedAt;
}
