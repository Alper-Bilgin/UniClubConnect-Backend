package com.uniclubconnect.services.chatservice.controller;

import com.uniclubconnect.services.chatservice.dto.ChatMessageRequest;
import com.uniclubconnect.services.chatservice.dto.ChatMessageResponse;
import com.uniclubconnect.services.chatservice.dto.ConversationDTO;
import com.uniclubconnect.services.chatservice.dto.EditMessageRequest;
import com.uniclubconnect.services.chatservice.model.MessageStatus;
import com.uniclubconnect.services.chatservice.repository.ConversationRepository;
import com.uniclubconnect.services.chatservice.repository.MessageRepository;
import com.uniclubconnect.services.chatservice.security.dto.UserPrincipal;
import com.uniclubconnect.services.chatservice.service.ChatService;
import com.uniclubconnect.services.chatservice.service.ConversationService;
import com.uniclubconnect.services.chatservice.util.ChatRoomUtil;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ChatController {
    private final ConversationService conversationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository; // ✅ YENİ
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
    // ==========================================
    // 👇 CONVERSATION (INBOX) ENDPOINTS 👇
    // ==========================================

    /**
     * Kullanıcının konuşma listesini (inbox) getir
     *
     * GET /api/chat/conversations?page=0&size=20
     * Response: Page<ConversationDTO>
     */
    @GetMapping("/api/chat/conversations")
    public ResponseEntity<Page<ConversationDTO>> getConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {

        UserPrincipal user = (UserPrincipal) ((Authentication) principal).getPrincipal();
        Page<ConversationDTO> conversations = conversationService.getUserConversations(
                user.getAuthId(),
                PageRequest.of(page, size)
        );

        return ResponseEntity.ok(conversations);
    }

    /**
     * Arşivlenmiş konuşmaları getir
     *
     * GET /api/chat/conversations/archived?page=0&size=20
     */
    @GetMapping("/api/chat/conversations/archived")
    public ResponseEntity<Page<ConversationDTO>> getArchivedConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {

        UserPrincipal user = (UserPrincipal) ((Authentication) principal).getPrincipal();
        Page<ConversationDTO> conversations = conversationService.getUserArchivedConversations(
                user.getAuthId(),
                PageRequest.of(page, size)
        );

        return ResponseEntity.ok(conversations);
    }

    /**
     * Okunmamış konuşmaları getir
     *
     * GET /api/chat/conversations/unread?page=0&size=20
     */
    @GetMapping("/api/chat/conversations/unread")
    public ResponseEntity<Page<ConversationDTO>> getUnreadConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {

        UserPrincipal user = (UserPrincipal) ((Authentication) principal).getPrincipal();
        Page<ConversationDTO> conversations = conversationService.getUnreadConversations(
                user.getAuthId(),
                PageRequest.of(page, size)
        );

        return ResponseEntity.ok(conversations);
    }

    /**
     * Belirli konuşmayı okundu işaretle
     *
     * POST /api/chat/conversations/conv-123/mark-read
     */
    @PostMapping("/api/chat/conversations/{conversationId}/mark-read")
    @Transactional
    public ResponseEntity<Map<String, String>> markConversationAsRead(
            @PathVariable String conversationId,
            Principal principal) {

        UserPrincipal user = (UserPrincipal) ((Authentication) principal).getPrincipal();
        conversationService.markConversationAsRead(conversationId, user.getAuthId());

        return ResponseEntity.ok(Map.of("status", "success"));
    }

    /**
     * Konuşmayı arşivle
     *
     * POST /api/chat/conversations/conv-123/archive
     */
    @PostMapping("/api/chat/conversations/{conversationId}/archive")
    @Transactional
    public ResponseEntity<Map<String, String>> archiveConversation(
            @PathVariable String conversationId,
            Principal principal) {

        UserPrincipal user = (UserPrincipal) ((Authentication) principal).getPrincipal();
        conversationService.archiveConversation(conversationId, user.getAuthId());

        return ResponseEntity.ok(Map.of("status", "success", "message", "Konuşma arşivlendi"));
    }

    /**
     * Konuşmayı arşivden çıkar
     *
     * POST /api/chat/conversations/conv-123/unarchive
     */
    @PostMapping("/api/chat/conversations/{conversationId}/unarchive")
    @Transactional
    public ResponseEntity<Map<String, String>> unarchiveConversation(
            @PathVariable String conversationId,
            Principal principal) {

        UserPrincipal user = (UserPrincipal) ((Authentication) principal).getPrincipal();
        conversationService.unarchiveConversation(conversationId, user.getAuthId());

        return ResponseEntity.ok(Map.of("status", "success", "message", "Konuşma geri yüklendi"));
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





    // 👇 UNREAD MESSAGE ENDPOINTS 👇
    // ==========================================

    /**
     * Kullanıcının TOPLAM okunmamış mesaj sayısını getir
     *
     * GET /api/chat/unread-count
     * Response: { "unreadCount": 5 }
     */
    @GetMapping("/api/chat/unread-count")
    public ResponseEntity<Map<String, Integer>> getUnreadCount(Principal principal) {
        UserPrincipal user = (UserPrincipal) ((Authentication) principal).getPrincipal();
        int unreadCount = chatService.getUnreadMessageCount(user.getAuthId());

        return ResponseEntity.ok(Map.of("unreadCount", unreadCount));
    }

    /**
     * Belirli bir sohbetteki okunmamış sayısını getir
     *
     * GET /api/chat/unread-count/user123
     * Response: { "roomId": "user1_user123", "unreadCount": 3 }
     */
    @GetMapping("/api/chat/unread-count/{recipientId}")
    public ResponseEntity<Map<String, Object>> getUnreadCountByRoom(
            @PathVariable String recipientId,
            Principal principal) {

        UserPrincipal user = (UserPrincipal) ((Authentication) principal).getPrincipal();
        String roomId = ChatRoomUtil.getChatRoomId(user.getAuthId(), recipientId);
        int unreadCount = chatService.getUnreadCountByRoom(roomId, user.getAuthId());

        return ResponseEntity.ok(Map.of(
                "roomId", roomId,
                "recipientId", recipientId,
                "unreadCount", unreadCount
        ));
    }

    /**
     * Belirli bir sohbetteki mesajları okundu işaretle
     *
     * POST /api/chat/mark-read/user123
     * Response: { "status": "success", "message": "..." }
     */
    @PostMapping("/api/chat/mark-read/{recipientId}")
    @Transactional
    public ResponseEntity<Map<String, String>> markChatAsRead(
            @PathVariable String recipientId,
            Principal principal) {

        UserPrincipal user = (UserPrincipal) ((Authentication) principal).getPrincipal();
        String currentUserId = user.getAuthId();

        // ✅ 1. Message tablosundaki mesajları okundu yap
        chatService.markChatRoomAsRead(currentUserId, recipientId);

        // ✅ 2. KRİTİK: Conversation tablosundaki sayacı SIFIRLA!
        String conversationId = ChatRoomUtil.getChatRoomId(currentUserId, recipientId);
        conversationService.markConversationAsRead(conversationId, currentUserId);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Sohbet okundu işaretlendi"
        ));
    }

    // 👇 DELETE MESSAGE ENDPOINTS 👇
    // ==========================================

    /**
     * WebSocket: Mesajı sil (Real-time)
     *
     * Client gönderir:
     * {
     *   "destination": "/app/chat.delete",
     *   "messageId": "msg-uuid"
     * }
     *
     * Alıcı receives: /user/queue/message-deleted
     */
    @MessageMapping("/chat.delete")
    public void deleteMessage(@Payload Map<String, String> payload, Principal principal) {
        if (principal == null) return;

        String messageId = payload.get("messageId");
        UserPrincipal user = (UserPrincipal) ((Authentication) principal).getPrincipal();

        try {
            chatService.deleteMessage(messageId, user.getAuthId());
        } catch (Exception e) {
            log.error("Mesaj silme hatası: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * REST: Mesajı sil
     *
     * DELETE /api/chat/messages/msg-uuid
     * Response: { "status": "success", "message": "Mesaj silindi" }
     */
    @DeleteMapping("/api/chat/messages/{messageId}")
    @Transactional
    public ResponseEntity<Map<String, String>> deleteMessageRest(
            @PathVariable String messageId,
            Principal principal) {

        UserPrincipal user = (UserPrincipal) ((Authentication) principal).getPrincipal();

        try {
            chatService.deleteMessage(messageId, user.getAuthId());
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Mesaj başarıyla silindi",
                    "messageId", messageId
            ));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    // 👇 EDIT MESSAGE ENDPOINTS 👇
    // ==========================================

    /**
     * WebSocket: Mesajı düzenle (Real-time)
     *
     * Client gönderir:
     * {
     *   "destination": "/app/chat.edit",
     *   "messageId": "msg-uuid",
     *   "content": "Yeni içerik"
     * }
     *
     * Alıcı receives: /user/queue/message-edited
     */
    @MessageMapping("/chat.edit")
    public void editMessage(@Payload EditMessageRequest request, Principal principal) {
        if (principal == null) return;

        UserPrincipal user = (UserPrincipal) ((Authentication) principal).getPrincipal();

        try {
            chatService.editMessage(request.getMessageId(), request.getContent(), user.getAuthId());
        } catch (Exception e) {
            log.error("Mesaj düzenleme hatası: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * REST: Mesajı düzenle
     *
     * PUT /api/chat/messages/msg-uuid
     * Body: { "content": "Yeni içerik" }
     * Response: ChatMessageResponse
     */
    @PutMapping("/api/chat/messages/{messageId}")
    @Transactional
    public ResponseEntity<ChatMessageResponse> editMessageRest(
            @PathVariable String messageId,
            @RequestBody Map<String, String> payload,
            Principal principal) {

        UserPrincipal user = (UserPrincipal) ((Authentication) principal).getPrincipal();
        String newContent = payload.get("content");

        try {
            ChatMessageResponse response = chatService.editMessage(messageId, newContent, user.getAuthId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Tüm mesajları okundu işaretle
     *
     * POST /api/chat/mark-all-read
     * Response: { "status": "success" }
     */
    @PostMapping("/api/chat/mark-all-read")
    @Transactional
    public ResponseEntity<Map<String, String>> markAllAsRead(Principal principal) {
        UserPrincipal user = (UserPrincipal) ((Authentication) principal).getPrincipal();
        chatService.markAllAsRead(user.getAuthId());

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Tüm mesajlar okundu işaretlendi"
        ));
    }

    /**
     * WebSocket: Belirli mesajı okundu işaretle (Real-time)
     *
     * Client gönderir:
     * {
     *   "destination": "/app/chat.mark-read",
     *   "messageId": "msg-uuid",
     *   "senderId": "sender-id"
     * }
     */
    @MessageMapping("/chat.mark-read")
    public void markMessageAsRead(@Payload Map<String, String> payload, Principal principal) {
        if (principal == null) return;

        String messageId = payload.get("messageId");
        String senderId = payload.get("senderId");

        // Message Entity'i bul
        messageRepository.findById(messageId).ifPresent(msg -> {
            // 1️⃣ Message tablosunu okundu işaretle
            msg.setRead(true);
            msg.setReadAt(LocalDateTime.now());
            messageRepository.save(msg);
            log.debug("✅ Mesaj okundu: {}", messageId);

            // 2️⃣ KRİTİK: Conversation sayacını güncellemek için chat room bilgisini al
            String currentUserId = principal instanceof UserPrincipal ?
                    ((UserPrincipal) principal).getAuthId() :
                    ((org.springframework.security.core.Authentication) principal).getPrincipal().toString();

            String conversationId = ChatRoomUtil.getChatRoomId(currentUserId, msg.getRecipientId());

            // 3️⃣ Conversation'daki sayacı kontrol et ve güncellediyse sıfırla
            conversationRepository.findById(conversationId).ifPresent(conv -> {
                if (msg.getSenderId().equals(conv.getParticipant1Id())) {
                    // Message participant2'den geldi, participant1'in sayacını sıfırla
                    conversationRepository.updateUnreadCountP1(conversationId, 0);
                } else {
                    // Message participant1'den geldi, participant2'nin sayacını sıfırla
                    conversationRepository.updateUnreadCountP2(conversationId, 0);
                }
                log.debug("✅ Conversation sayacı sıfırlandı: {}", conversationId);
            });
        });

        // Gönderici'ye "okundu" bilgisini gönder (Real-time)
        messagingTemplate.convertAndSendToUser(
                senderId,
                "/queue/read-status",
                Map.of(
                        "messageId", messageId,
                        "status", "READ",
                        "readAt", LocalDateTime.now()
                )
        );
    }

    // 👇 MESSAGE SEARCH ENDPOINTS 👇
    // ==========================================

    /**
     * Belirli bir chat room'daki mesajlarda ara
     *
     * GET /api/chat/search?recipientId=user2&q=merhaba&page=0&size=20
     *
     * Query Parameters:
     * - recipientId: Diğer kullanıcının ID'si
     * - q: Aranacak kelime/cümle
     * - page: Sayfa numarası (default: 0)
     * - size: Sayfa başına kayıt (default: 20)
     *
     * Response: Page<ChatMessageResponse>
     */
    @GetMapping("/api/chat/search")
    public ResponseEntity<Page<ChatMessageResponse>> searchMessagesInRoom(
            @RequestParam String recipientId,
            @RequestParam(name = "q") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {

        UserPrincipal user = (UserPrincipal) ((Authentication) principal).getPrincipal();

        try {
            Page<ChatMessageResponse> results = chatService.searchMessagesInRoom(
                    user.getAuthId(),
                    recipientId,
                    query,
                    PageRequest.of(page, size)
            );

            return ResponseEntity.ok(results);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Tüm mesajlarda ara (Global search)
     *
     * GET /api/chat/search-all?q=proje&page=0&size=20
     *
     * Query Parameters:
     * - q: Aranacak kelime/cümle
     * - page: Sayfa numarası (default: 0)
     * - size: Sayfa başına kayıt (default: 20)
     *
     * Response: Page<ChatMessageResponse>
     *
     * Örnek: Kullanıcının tüm sohbetlerindeki "toplantı" içeren mesajları bulur
     */
    @GetMapping("/api/chat/search-all")
    public ResponseEntity<Page<ChatMessageResponse>> searchUserMessages(
            @RequestParam(name = "q") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {

        UserPrincipal user = (UserPrincipal) ((Authentication) principal).getPrincipal();

        try {
            Page<ChatMessageResponse> results = chatService.searchUserMessages(
                    user.getAuthId(),
                    query,
                    PageRequest.of(page, size)
            );

            return ResponseEntity.ok(results);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Belirli bir kişi ile olan konuşmada ara
     *
     * GET /api/chat/search/{partnerId}?q=deneme&page=0&size=20
     *
     * Path Parameters:
     * - partnerId: Diğer kullanıcının ID'si
     *
     * Query Parameters:
     * - q: Aranacak kelime/cümle
     * - page: Sayfa numarası (default: 0)
     * - size: Sayfa başına kayıt (default: 20)
     *
     * Response: Page<ChatMessageResponse>
     */
    @GetMapping("/api/chat/search/{partnerId}")
    public ResponseEntity<Page<ChatMessageResponse>> searchConversationMessages(
            @PathVariable String partnerId,
            @RequestParam(name = "q") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {

        UserPrincipal user = (UserPrincipal) ((Authentication) principal).getPrincipal();

        try {
            Page<ChatMessageResponse> results = chatService.searchConversationMessages(
                    user.getAuthId(),
                    partnerId,
                    query,
                    PageRequest.of(page, size)
            );

            return ResponseEntity.ok(results);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
