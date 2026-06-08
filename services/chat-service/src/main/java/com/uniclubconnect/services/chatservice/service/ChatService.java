package com.uniclubconnect.services.chatservice.service;

import com.uniclubconnect.services.chatservice.config.RabbitMQConfig;
import com.uniclubconnect.services.chatservice.dto.ChatMessageRequest;
import com.uniclubconnect.services.chatservice.dto.ChatMessageResponse;
import com.uniclubconnect.services.chatservice.dto.UnreadMessageEvent;
import com.uniclubconnect.services.chatservice.model.ChatRoom;
import com.uniclubconnect.services.chatservice.model.ChatType;
import com.uniclubconnect.services.chatservice.model.Message;
import com.uniclubconnect.services.chatservice.model.MessageStatus;
import com.uniclubconnect.services.chatservice.repository.ChatRoomRepository;
import com.uniclubconnect.services.chatservice.repository.MessageRepository;
import com.uniclubconnect.services.chatservice.util.ChatRoomUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final ConversationService conversationService;
    @Transactional
    public ChatMessageResponse saveMessage(String senderId, ChatMessageRequest request) {

        // ==========================================
        // 1. RATE LIMITING (Redis Fail-Safe)
        // ==========================================
        try {
            String rateKey = "rate:user:" + senderId;
            Long count = redisTemplate.opsForValue().increment(rateKey);

            if (count != null && count == 1) {
                redisTemplate.expire(rateKey, java.time.Duration.ofSeconds(1));
            }

            if (count != null && count > 5) {
                log.warn("Rate limit aşıldı! Kullanıcı: {}", senderId);
                throw new RuntimeException("Çok hızlı mesaj gönderiyorsunuz, lütfen yavaşlayın!");
            }
        } catch (Exception e) {
            // Redis down olursa sistemi durdurma (High Availability)
            log.error("Rate limiting atlandı (Redis hatası): {}", e.getMessage());
        }

        // ==========================================
        // 2. VALIDATION (Veri Doğrulama)
        // ==========================================
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Mesaj içeriği boş olamaz!");
        }
        if (request.getContent().length() > 2000) {
            throw new IllegalArgumentException("Mesaj boyutu çok büyük (Max 2000 karakter).");
        }
        if (request.getClientMessageId() == null || request.getClientMessageId().isBlank()) {
            throw new IllegalArgumentException("clientMessageId zorunludur! (Idempotency için)");
        }

        // ==========================================
        // 3. DUPLICATE CHECK (Mükerrer İstek Engeli)
        // ==========================================
        Optional<Message> existingMessage = messageRepository.findByClientMessageId(request.getClientMessageId());
        if (existingMessage.isPresent()) {
            log.info("Mükerrer mesaj yakalandı, kayıt atlandı: {}", request.getClientMessageId());
            return mapToResponse(existingMessage.get());
        }

        // ==========================================
        // 4. CHAT ROOM (Race-Condition Korumalı)
        // ==========================================
        String roomId = ChatRoomUtil.getChatRoomId(senderId, request.getRecipientId());

        if (!chatRoomRepository.existsById(roomId)) {
            try {
                chatRoomRepository.save(
                        ChatRoom.builder()
                                .id(roomId)
                                .type(ChatType.DIRECT)
                                .build()
                );
            } catch (DataIntegrityViolationException e) {
                log.info("Oda zaten başka thread tarafından oluşturulmuş: {}", roomId);
            }
        }

        // ==========================================
        // 5. ONLINE STATUS CHECK
        // ==========================================
        boolean isRecipientOnline = false;
        try {
            isRecipientOnline = Boolean.TRUE.equals(redisTemplate.hasKey("online:user:" + request.getRecipientId()));
        } catch (Exception e) {
            log.warn("Online check yapılamadı (Redis down olabilir). Status SENT olarak ayarlanacak.");
        }

        MessageStatus status = isRecipientOnline ? MessageStatus.DELIVERED : MessageStatus.SENT;

        // ==========================================
        // 6. SAVE MESSAGE (Double Safety)
        // ==========================================
        Message message = Message.builder()
                .clientMessageId(request.getClientMessageId())
                .chatRoomId(roomId)
                .senderId(senderId)
                .recipientId(request.getRecipientId())
                .content(request.getContent())
                .status(status)
                .build();

        Message savedMessage;
        try {
            savedMessage = messageRepository.save(message);
        } catch (DataIntegrityViolationException e) {
            // Thread'ler yarışırsa ve aynı clientMessageId ile DB'ye insert atmaya çalışırlarsa
            log.warn("Duplicate insert yakalandı (race): {}", request.getClientMessageId());
            return messageRepository.findByClientMessageId(request.getClientMessageId())
                    .map(this::mapToResponse)
                    .orElseThrow(() -> e);
        }

        /// ==========================================
        // 7. ASYNC EVENT (RabbitMQ)
        // ==========================================
        if (!isRecipientOnline) {
            log.info("Offline kullanıcı, notification event gönderilecek: {}", request.getRecipientId());

            // Mailde çok uzun görünmesin diye mesajın ilk 50 karakterini "Önizleme" yapıyoruz
            String preview = request.getContent().length() > 50
                    ? request.getContent().substring(0, 50) + "..."
                    : request.getContent();

            // Gönderilecek Event objesini dolduruyoruz
            UnreadMessageEvent event = new UnreadMessageEvent(
                    savedMessage.getId(),
                    senderId,
                    request.getRecipientId(),
                    preview,
                    savedMessage.getCreatedAt()
            );

            // RabbitMQ'ya JSON formatında fırlatıyoruz!
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CHAT_EXCHANGE,
                    RabbitMQConfig.UNREAD_MESSAGE_ROUTING_KEY,
                    event
            );
            log.info("Event RabbitMQ'ya başarıyla fırlatıldı!");
        }
        // ✅ YENİ: Conversation'ı güncelle (her zaman)
        conversationService.createOrUpdateConversation(
                senderId,
                request.getRecipientId(),
                savedMessage
        );
        return mapToResponse(savedMessage);
    }

    // ==========================================
    // STATUS UPDATE (Delivered / Read)
    // ==========================================
    @Transactional
    public void updateMessageStatus(String messageId, MessageStatus status) {
        messageRepository.findById(messageId).ifPresent(msg -> {
            msg.setStatus(status);
            messageRepository.save(msg);
        });
    }

    // ==========================================
    // PAGINATION HISTORY
    // ==========================================
    public Page<ChatMessageResponse> getChatHistory(String senderId, String recipientId, Pageable pageable) {
        String roomId = ChatRoomUtil.getChatRoomId(senderId, recipientId);

        // 👇 DÜZELTİLEN SATIR: Metot adı Repository'deki ile birebir aynı yapıldı 👇
        Page<Message> messages = messageRepository.findByChatRoomIdOrderByCreatedAtDescIdDesc(roomId, pageable);

        return messages.map(this::mapToResponse);
    }


    // ==========================================
    // HELPER: MAP TO DTO (Eksik Olan Metot)
    // ==========================================
    private ChatMessageResponse mapToResponse(Message msg) {

        // ✅ YENİ: SİLİNMİŞ MESAJ KONTROLÜ
        String displayContent = msg.isDeleted() ? "🚫 Bu mesaj silindi." : msg.getContent();

        return ChatMessageResponse.builder()
                .id(msg.getId())
                .clientMessageId(msg.getClientMessageId())
                .senderId(msg.getSenderId())
                .recipientId(msg.getRecipientId())
                .content(displayContent)  // ✅ Maskelenmiş içerik
                .status(msg.getStatus().name())
                .timestamp(msg.getCreatedAt())
                .isEdited(msg.isEdited())
                .editedAt(msg.getEditedAt())
                .build();
    }



    // ✅ UNREAD MESSAGE OPERATIONS
    // ==========================================

    /**
     * Kullanıcının TOPLAM okunmamış mesaj sayısını döndürür
     *
     * Örnek: Frontend inbox badge'te gösterir
     */
    public int getUnreadMessageCount(String userId) {
        try {
            return messageRepository.countUnreadMessages(userId);
        } catch (Exception e) {
            log.error("Unread count alınamadı: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Belirli bir chat room'daki okunmamış mesaj sayısını döndürür
     *
     * Örnek: Konuşma listesinde her konuşmanın yanında badge
     */
    public int getUnreadCountByRoom(String roomId, String userId) {
        try {
            return messageRepository.countUnreadByChatRoom(roomId, userId);
        } catch (Exception e) {
            log.error("Room unread count alınamadı: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Belirli bir sohbetteki tüm mesajları "okundu" işaretle
     *
     * Kullanım: Kullanıcı sohbeti açtığında çağır
     */
    @Transactional
    public void markChatRoomAsRead(String userId, String otherUserId) {
        String roomId = ChatRoomUtil.getChatRoomId(userId, otherUserId);
        LocalDateTime now = LocalDateTime.now();

        messageRepository.markChatRoomAsRead(roomId, userId, now);
        log.info("✅ Chat room okundu işaretlendi: {} by {}", roomId, userId);

        // WebSocket: Alıcıya "bu sohbetteki mesajlar okundu" bilgisini gönder
        messagingTemplate.convertAndSendToUser(
                otherUserId,
                "/queue/read-status",
                Map.of(
                        "roomId", roomId,
                        "markedAsRead", true,
                        "markedBy", userId,
                        "timestamp", now
                )
        );
    }

    /**
     * Tüm okunmamış mesajları "okundu" işaretle
     *
     * Kullanım: "Tümünü okundu işaretle" butonu
     */
    @Transactional
    public void markAllAsRead(String userId) {
        LocalDateTime now = LocalDateTime.now();
        messageRepository.markAllAsRead(userId, now);
        log.info("✅ Tüm mesajlar okundu işaretlendi: {}", userId);
    }

    // ✅ DELETE MESSAGE
    // ==========================================

    /**
     * Mesajı sil (soft delete)
     *
     * Güvenlik: Sadece gönderici silebilir
     * WebSocket'e broadcast et
     */
    @Transactional
    public void deleteMessage(String messageId, String userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Mesaj bulunamadı"));

        // 🔒 Güvenlik: Sadece gönderici silebilir
        if (!message.getSenderId().equals(userId)) {
            throw new RuntimeException("Başka birinin mesajını silemezsin!");
        }

        // Soft delete (deletedAt'ı set et)
        messageRepository.softDeleteMessage(messageId, LocalDateTime.now());

        // 📱 WebSocket: Alıcıya "bu mesaj silindi" bildir (real-time)
        messagingTemplate.convertAndSendToUser(
                message.getRecipientId(),
                "/queue/message-deleted",
                Map.of(
                        "messageId", messageId,
                        "deletedAt", LocalDateTime.now(),
                        "message", "Mesaj gönderici tarafından silindi"
                )
        );

        log.info("✅ Mesaj silindi: {} by {}", messageId, userId);
    }

    // ✅ EDIT MESSAGE
    // ==========================================

    /**
     * Mesajı düzenle
     *
     * Güvenlik: Sadece gönderici düzenleyebilir
     * Silinen mesajı düzenleyemez
     * WebSocket'e broadcast et
     */
    @Transactional
    public ChatMessageResponse editMessage(String messageId, String newContent, String userId) {

        // 1️⃣ VALIDATION
        if (newContent == null || newContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Yeni mesaj içeriği boş olamaz!");
        }
        if (newContent.length() > 2000) {
            throw new IllegalArgumentException("Mesaj boyutu çok büyük (Max 2000 karakter).");
        }

        // 2️⃣ MESSAGE'I BUL
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Mesaj bulunamadı"));

        // 3️⃣ GÜVENLİK: Sadece gönderici düzenleyebilir
        if (!message.getSenderId().equals(userId)) {
            throw new RuntimeException("Başka birinin mesajını düzenleyemezsin!");
        }

        // 4️⃣ GÜVENLİK: Silinen mesaj düzenlenemez
        if (message.getDeletedAt() != null) {
            throw new IllegalArgumentException("Silinen mesaj düzenlenemez!");
        }

        // 5️⃣ DÜZENLE
        message.setContent(newContent);
        message.setEdited(true);
        message.setEditedAt(LocalDateTime.now());

        Message updatedMessage = messageRepository.save(message);
        ChatMessageResponse response = mapToResponse(updatedMessage);

        // 6️⃣ WebSocket: Hem göndericiye hem alıcıya gönder (real-time)
        messagingTemplate.convertAndSendToUser(
                message.getRecipientId(),
                "/queue/message-edited",
                response
        );

        messagingTemplate.convertAndSendToUser(
                userId,
                "/queue/message-edited",
                response
        );

        log.info("✅ Mesaj düzenlendi: {} by {}", messageId, userId);
        return response;
    }

    // ✅ MESSAGE SEARCH
    // ==========================================

    /**
     * Belirli bir chat room içinde mesaj ara
     *
     * Kullanım: User A ile User B arasındaki sohbette "merhaba" ara
     */
    public Page<ChatMessageResponse> searchMessagesInRoom(
            String senderId,
            String recipientId,
            String query,
            Pageable pageable) {

        // ✅ VALIDATION
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Arama terimi boş olamaz!");
        }

        if (query.length() > 100) {
            throw new IllegalArgumentException("Arama terimi çok uzun (Max 100 karakter).");
        }

        // Chat room ID'sini oluştur
        String roomId = ChatRoomUtil.getChatRoomId(senderId, recipientId);

        // Arama yap
        Page<Message> messages = messageRepository.searchMessagesInRoom(
                roomId,
                query,
                pageable
        );

        log.info("✅ Chat room'da arama yapıldı: {} in {} - Query: {}", senderId, roomId, query);
        return messages.map(this::mapToResponse);
    }

    /**
     * Kullanıcının TÜM mesajlarında ara (global search)
     *
     * Kullanım: Tüm sohbetlerdeki mesajlar arasında "önemli" ara
     */
    public Page<ChatMessageResponse> searchUserMessages(
            String userId,
            String query,
            Pageable pageable) {

        // ✅ VALIDATION
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Arama terimi boş olamaz!");
        }

        if (query.length() > 100) {
            throw new IllegalArgumentException("Arama terimi çok uzun (Max 100 karakter).");
        }

        // Arama yap
        Page<Message> messages = messageRepository.searchUserMessages(
                userId,
                query,
                pageable
        );

        log.info("✅ Global arama yapıldı: {} - Query: {} - Bulundu: {} sonuç",
                userId, query, messages.getTotalElements());
        return messages.map(this::mapToResponse);
    }

    /**
     * Belirli bir kişi ile olan konuşmada mesaj ara
     *
     * Kullanım: User A ile User B arasındaki sohbette "deneme" ara
     */
    public Page<ChatMessageResponse> searchConversationMessages(
            String userId,
            String partnerId,
            String query,
            Pageable pageable) {

        // ✅ VALIDATION
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Arama terimi boş olamaz!");
        }

        if (query.length() > 100) {
            throw new IllegalArgumentException("Arama terimi çok uzun (Max 100 karakter).");
        }

        if (userId.equals(partnerId)) {
            throw new IllegalArgumentException("Aynı kullanıcı ile arama yapamazsınız!");
        }

        // Arama yap
        Page<Message> messages = messageRepository.searchConversationMessages(
                userId,
                partnerId,
                query,
                pageable
        );

        log.info("✅ Konuşma araması yapıldı: {} ↔️ {} - Query: {} - Bulundu: {} sonuç",
                userId, partnerId, query, messages.getTotalElements());
        return messages.map(this::mapToResponse);
    }
}