package com.uniclubconnect.services.chatservice.controller;

import com.uniclubconnect.services.chatservice.dto.ActiveChatResponse;
import com.uniclubconnect.services.chatservice.dto.ChatMessageRequest;
import com.uniclubconnect.services.chatservice.dto.ChatMessageResponse;
import com.uniclubconnect.services.chatservice.model.MessageStatus;
import com.uniclubconnect.services.chatservice.security.dto.UserPrincipal;
import com.uniclubconnect.services.chatservice.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;

    // 1. ANA MESAJLAŞMA
    @MessageMapping("/chat.send")
    public void processMessage(@Payload ChatMessageRequest chatMessage, Principal principal) {
        if (principal == null) throw new RuntimeException("Unauthorized");

        UserPrincipal user = (UserPrincipal) ((Authentication) principal).getPrincipal();
        String senderId = user.getAuthId();

        // Mesajı Kaydet (Status = SENT)
        ChatMessageResponse savedMsg = chatService.saveMessage(senderId, chatMessage);

        // Gönderene Echo (UI deduplication id ile yapılacak)
        messagingTemplate.convertAndSendToUser(senderId, "/queue/messages", savedMsg);

        // 👇 KRİTİK: ONLINE KONTROLÜ (CONTROLLER'A TAŞINDI) 👇
        boolean isOnline = Boolean.TRUE.equals(redisTemplate.hasKey("online:user:" + chatMessage.getRecipientId()));

        if (isOnline) {
            // Online ise direkt WebSocket'e gönder
            messagingTemplate.convertAndSendToUser(
                    chatMessage.getRecipientId(),
                    "/queue/messages",
                    savedMsg
            );
        } else {
            // Offline ise RabbitMQ'ya Notification Eventi Fırlat
            log.info("User {} offline. RabbitMQ Notification tetiklendi.", chatMessage.getRecipientId());
            rabbitTemplate.convertAndSend("notification_exchange", "unread.message",
                    Map.of("messageId", savedMsg.getId(), "recipientId", chatMessage.getRecipientId()));
        }
    }

    // 👇 BONUS 1: ACK (İLETİLDİ / OKUNDU BİLDİRİMİ) 👇
    @MessageMapping("/chat.status")
    public void updateStatus(@Payload Map<String, String> payload, Principal principal) {
        String messageId = payload.get("messageId");
        String senderIdToNotify = payload.get("senderId"); // Mesajı kim attıysa ona iletilecek
        MessageStatus newStatus = MessageStatus.valueOf(payload.get("status")); // DELIVERED veya READ

        // DB'de statüyü güncelle
        chatService.updateMessageStatus(messageId, newStatus);

        // Orijinal göndericiye (Sender) "Mesajın okundu/iletildi" bilgisini WebSocket üzerinden geri yolla
        messagingTemplate.convertAndSendToUser(
                senderIdToNotify,
                "/queue/status",
                Map.of("messageId", messageId, "status", newStatus.name())
        );
    }

    // 👇 BONUS 2: YAZIYOR... (TYPING INDICATOR) 👇
    @MessageMapping("/chat.typing")
    public void typingIndicator(@Payload Map<String, String> payload, Principal principal) {
        if (principal == null) return;
        UserPrincipal user = (UserPrincipal) ((Authentication) principal).getPrincipal();

        String recipientId = payload.get("recipientId");
        boolean isTyping = Boolean.parseBoolean(payload.get("isTyping"));

        messagingTemplate.convertAndSendToUser(
                recipientId,
                "/queue/typing",
                Map.of("senderId", user.getAuthId(), "isTyping", isTyping)
        );
    }

    // REST: HISTORY API (PAGINATION)
    @GetMapping("/api/chat/history/{recipientId}")
    public ResponseEntity<Page<ChatMessageResponse>> getChatHistory(
            @PathVariable String recipientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Principal principal) {

        UserPrincipal user = (UserPrincipal) ((Authentication) principal).getPrincipal();
        return ResponseEntity.ok(chatService.getChatHistory(user.getAuthId(), recipientId, PageRequest.of(page, size)));
    }

    // 👇 WEBSOCKET HATA YAKALAYICI (ERROR HANDLER) 👇
    @org.springframework.messaging.handler.annotation.MessageExceptionHandler
    @org.springframework.messaging.simp.annotation.SendToUser("/queue/errors")
    public String handleException(Exception ex) {
        log.error("WebSocket Hatası: {}", ex.getMessage());
        // Bu mesaj client'ın "/user/queue/errors" kanalına gidecek.
        // Frontend bu kanalı dinleyip kırmızı bir Toast/Snackbar gösterecek.
        return ex.getMessage();
    }

    // 👇 YENİ REST API: Aktif Sohbetler Listesi 👇
    @GetMapping("/api/chat/active")
    public ResponseEntity<java.util.List<ActiveChatResponse>> getActiveChats(Principal principal) {
        if (principal == null) throw new RuntimeException("Unauthorized");

        UserPrincipal user = (UserPrincipal) ((Authentication) principal).getPrincipal();
        return ResponseEntity.ok(chatService.getActiveChats(user.getAuthId()));
    }
}
