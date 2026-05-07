package com.uniclubconnect.services.chatservice.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ActiveChatResponse {
    private String roomId;
    private String otherUserId; // Karşı tarafın Auth ID'si (Grup mesajlarında null kalabilir)
    private String chatName;    // Grup eklendiğinde "Yazılım Kulübü" gibi isimler buraya gelecek
    private String lastMessage; // "Naber, nasılsın?"
    private LocalDateTime lastMessageTime;
    private String lastMessageStatus; // Mavi tik / Gri tik iconu için (READ/DELIVERED/SENT)
    private String chatType;    // DIRECT veya GROUP
}
