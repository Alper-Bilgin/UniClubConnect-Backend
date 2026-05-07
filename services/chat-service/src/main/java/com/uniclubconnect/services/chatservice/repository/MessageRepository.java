package com.uniclubconnect.services.chatservice.repository;

import com.uniclubconnect.services.chatservice.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {

    // YENİ SIRALAMA MANTIĞI: Önce tarihe göre, eğer tarih aynıysa ID'ye göre sırala
    Page<Message> findByChatRoomIdOrderByCreatedAtDescIdDesc(String chatRoomId, Pageable pageable);

    // MÜKERRER MESAJ KONTROLÜ İÇİN
    Optional<Message> findByClientMessageId(String clientMessageId);

    @org.springframework.data.jpa.repository.Query(
            "SELECT m FROM Message m " +
                    "WHERE (m.senderId = :userId OR m.recipientId = :userId) " +
                    "AND m.createdAt = (SELECT MAX(m2.createdAt) FROM Message m2 WHERE m2.chatRoomId = m.chatRoomId) " +
                    "ORDER BY m.createdAt DESC"
    )
    java.util.List<Message> findLatestMessagesForUserChats(@org.springframework.data.repository.query.Param("userId") String userId);
}
