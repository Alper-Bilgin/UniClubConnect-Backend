package com.uniclubconnect.services.chatservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnreadMessageCountResponse {
    private String senderId; // Mesajı gönderen kişinin ID'si
    private Long unreadCount; // Ondan gelen okunmamış mesaj sayısı
}
