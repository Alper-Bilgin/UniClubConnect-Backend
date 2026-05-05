package com.uniclubconnect.services.chatservice.dto;

import lombok.Data;

@Data
public class ChatMessageRequest {
    private String clientMessageId;
    private String recipientId;
    private String content;
}
