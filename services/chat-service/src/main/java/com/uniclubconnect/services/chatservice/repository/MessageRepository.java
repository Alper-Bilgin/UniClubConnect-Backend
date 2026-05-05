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
}
