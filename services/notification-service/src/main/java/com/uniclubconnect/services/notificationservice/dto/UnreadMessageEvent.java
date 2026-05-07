package com.uniclubconnect.services.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnreadMessageEvent implements Serializable {
    private String messageId;
    private String senderId;
    private String recipientId;
    private String contentPreview;
    private LocalDateTime timestamp;
}
