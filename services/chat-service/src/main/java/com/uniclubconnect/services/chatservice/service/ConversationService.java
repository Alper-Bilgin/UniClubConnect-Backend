package com.uniclubconnect.services.chatservice.service;

import com.uniclubconnect.services.chatservice.dto.ConversationDTO;
import com.uniclubconnect.services.chatservice.model.Conversation;
import com.uniclubconnect.services.chatservice.model.Message;
import com.uniclubconnect.services.chatservice.repository.ConversationRepository;
import com.uniclubconnect.services.chatservice.util.ChatRoomUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;

    /**
     * Yeni konuşma oluştur veya mevcut olanı son mesaj ile güncelle
     *
     * ChatService.saveMessage() içerisinden çağırılır
     */
    @Transactional
    public void createOrUpdateConversation(String userId1, String userId2, Message lastMessage) {
        String conversationId = ChatRoomUtil.getChatRoomId(userId1, userId2);

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElse(Conversation.builder()
                        .id(conversationId)
                        .participant1Id(userId1)
                        .participant2Id(userId2)
                        .build());

        // Son mesaj detaylarını güncelle
        conversation.setLastMessageId(lastMessage.getId());

        // Mesaj önizlemesi (ilk 100 karakter)
        String preview = lastMessage.getContent().length() > 100
                ? lastMessage.getContent().substring(0, 100) + "..."
                : lastMessage.getContent();
        conversation.setLastMessagePreview(preview);

        conversation.setLastMessageSenderId(lastMessage.getSenderId());
        conversation.setLastMessageTime(lastMessage.getCreatedAt());

        // Okunmamış sayıyı arttır
        if (lastMessage.getSenderId().equals(userId1)) {
            // userId1 gönderici → userId2'nin sayacını arttır
            conversation.setUnreadCountP2(conversation.getUnreadCountP2() + 1);
        } else {
            // userId2 gönderici → userId1'in sayacını arttır
            conversation.setUnreadCountP1(conversation.getUnreadCountP1() + 1);
        }

        conversationRepository.save(conversation);
        log.info("✅ Conversation güncellendi: {}", conversationId);
    }

    /**
     * Kullanıcının konuşma listesini (inbox) getir
     *
     * GET /api/chat/conversations
     */
    public Page<ConversationDTO> getUserConversations(String userId, Pageable pageable) {
        Page<Conversation> conversations = conversationRepository.findUserConversations(userId, pageable);
        return conversations.map(conv -> mapToDTO(conv, userId));
    }

    /**
     * Arşivlenmiş konuşmaları getir
     *
     * GET /api/chat/conversations/archived
     */
    public Page<ConversationDTO> getUserArchivedConversations(String userId, Pageable pageable) {
        Page<Conversation> conversations = conversationRepository.findUserArchivedConversations(userId, pageable);
        return conversations.map(conv -> mapToDTO(conv, userId));
    }

    /**
     * Okunmamış konuşmaları getir
     *
     * GET /api/chat/conversations/unread
     */
    public Page<ConversationDTO> getUnreadConversations(String userId, Pageable pageable) {
        Page<Conversation> conversations = conversationRepository.findUnreadConversations(userId, pageable);
        return conversations.map(conv -> mapToDTO(conv, userId));
    }

    /**
     * Konuşmayı okundu işaretle
     *
     * POST /api/chat/conversations/{conversationId}/mark-read
     */
    @Transactional
    public void markConversationAsRead(String conversationId, String userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        if (conversation.getParticipant1Id().equals(userId)) {
            conversationRepository.updateUnreadCountP1(conversationId, 0);
        } else {
            conversationRepository.updateUnreadCountP2(conversationId, 0);
        }

        log.info("✅ Conversation okundu işaretlendi: {} by {}", conversationId, userId);
    }

    /**
     * Konuşmayı arşivle
     *
     * POST /api/chat/conversations/{conversationId}/archive
     */
    @Transactional
    public void archiveConversation(String conversationId, String userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        if (conversation.getParticipant1Id().equals(userId)) {
            conversationRepository.updateArchiveStatusP1(conversationId, true);
        } else {
            conversationRepository.updateArchiveStatusP2(conversationId, true);
        }

        log.info("✅ Conversation arşivlendi: {} by {}", conversationId, userId);
    }

    /**
     * Konuşmayı arşivden çıkar
     *
     * POST /api/chat/conversations/{conversationId}/unarchive
     */
    @Transactional
    public void unarchiveConversation(String conversationId, String userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        if (conversation.getParticipant1Id().equals(userId)) {
            conversationRepository.updateArchiveStatusP1(conversationId, false);
        } else {
            conversationRepository.updateArchiveStatusP2(conversationId, false);
        }

        log.info("✅ Conversation arşivden çıkarıldı: {} by {}", conversationId, userId);
    }

    /**
     * Entity'i DTO'ya çevir
     *
     * Hangi participant olduğuna göre okunmamış sayı ve arşiv durumunu belirle
     */
    private ConversationDTO mapToDTO(Conversation conversation, String userId) {
        // Diğer participant kim?
        String otherParticipantId = conversation.getParticipant1Id().equals(userId)
                ? conversation.getParticipant2Id()
                : conversation.getParticipant1Id();

        // Bu user'ın okunmamış sayısı kaç?
        int unreadCount = conversation.getParticipant1Id().equals(userId)
                ? conversation.getUnreadCountP1()
                : conversation.getUnreadCountP2();

        // Bu user'ın konuşması arşivli mi?
        boolean isArchived = conversation.getParticipant1Id().equals(userId)
                ? conversation.isArchivedP1()
                : conversation.isArchivedP2();

        return ConversationDTO.builder()
                .conversationId(conversation.getId())
                .otherParticipantId(otherParticipantId)
                .lastMessagePreview(conversation.getLastMessagePreview())
                .lastMessageSenderId(conversation.getLastMessageSenderId())
                .lastMessageTime(conversation.getLastMessageTime())
                .unreadCount(unreadCount)
                .isArchived(isArchived)
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }
}
