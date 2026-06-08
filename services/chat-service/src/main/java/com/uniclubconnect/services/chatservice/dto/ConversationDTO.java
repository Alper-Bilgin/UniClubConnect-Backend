package com.uniclubconnect.services.chatservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationDTO {

    private String conversationId; // chatRoomId

    private String otherParticipantId;

    private String lastMessagePreview;

    private String lastMessageSenderId;

    private LocalDateTime lastMessageTime;

    private int unreadCount;

    private boolean isArchived;

    private LocalDateTime updatedAt;
}
