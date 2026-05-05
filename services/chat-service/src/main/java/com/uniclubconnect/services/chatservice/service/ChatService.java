package com.uniclubconnect.services.chatservice.service;

import com.uniclubconnect.services.chatservice.dto.ChatMessageRequest;
import com.uniclubconnect.services.chatservice.dto.ChatMessageResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;

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

        // ==========================================
        // 7. ASYNC EVENT (RabbitMQ)
        // ==========================================
        if (!isRecipientOnline) {
            log.info("Offline kullanıcı, notification event gönderilecek: {}", request.getRecipientId());
            // TODO: rabbitTemplate.convertAndSend(...) eklemesi yapılacak
        }

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
        return ChatMessageResponse.builder()
                .id(msg.getId())
                .clientMessageId(msg.getClientMessageId())
                .senderId(msg.getSenderId())
                .recipientId(msg.getRecipientId())
                .content(msg.getContent())
                .status(msg.getStatus().name())
                .timestamp(msg.getCreatedAt())
                .build();
    }
}