package com.uniclubconnect.services.chatservice.repository;

import com.uniclubconnect.services.chatservice.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {

    // YENİ SIRALAMA MANTIĞI: Önce tarihe göre, eğer tarih aynıysa ID'ye göre sırala
    Page<Message> findByChatRoomIdOrderByCreatedAtDescIdDesc(String chatRoomId, Pageable pageable);

    // MÜKERRER MESAJ KONTROLÜ İÇİN
    Optional<Message> findByClientMessageId(String clientMessageId);


    // ✅ YENİ: UNREAD MESSAGE QUERIES
    // ==========================================

    /**
     * Kullanıcının TOPLAM okunmamış mesaj sayısı
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.recipientId = :recipientId AND m.isRead = false AND m.isDeleted = false")
    int countUnreadMessages(@Param("recipientId") String recipientId);

    /**
     * Belirli chat room'daki okunmamış mesaj sayısı
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.chatRoomId = :roomId AND m.recipientId = :recipientId AND m.isRead = false AND m.isDeleted = false")
    int countUnreadByChatRoom(@Param("roomId") String roomId, @Param("recipientId") String recipientId);

    /**
     * Belirli sohbetteki tüm mesajları okundu işaretle
     */
    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isRead = true, m.readAt = :readAt WHERE m.chatRoomId = :roomId AND m.recipientId = :recipientId AND m.isRead = false")
    void markChatRoomAsRead(@Param("roomId") String roomId, @Param("recipientId") String recipientId, @Param("readAt") LocalDateTime readAt);

    /**
     * Tüm okunmamış mesajları okundu işaretle
     */
    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isRead = true, m.readAt = :readAt WHERE m.recipientId = :recipientId AND m.isRead = false AND m.isDeleted = false")
    void markAllAsRead(@Param("recipientId") String recipientId, @Param("readAt") LocalDateTime readAt);

    // ✅ YENİ: SOFT DELETE
    // ==========================================

    /**
     * Mesajı soft delete (deletedAt'ı set et)
     *
     * Hard delete yerine soft delete kullanıyoruz:
     * - Veri kaybı yok
     * - Audit trail kalıyor
     * - Reporting için kullanışlı
     */
    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.deletedAt = :now, m.isDeleted = true WHERE m.id = :messageId")
    void softDeleteMessage(@Param("messageId") String messageId, @Param("now") LocalDateTime now);

    // ✅ YENİ: MESSAGE SEARCH
    // ==========================================

    /**
     * Belirli bir chat room içinde mesaj ara
     *
     * Örnek: User A ile User B arasındaki mesajlar içinde "merhaba" ara
     * Case-insensitive ve deleted mesajları exclude et
     */
    @Query("SELECT m FROM Message m WHERE " +
            "m.chatRoomId = :roomId AND " +
            "LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%')) AND " +
            "m.deletedAt IS NULL " +
            "ORDER BY m.createdAt DESC")
    Page<Message> searchMessagesInRoom(
            @Param("roomId") String roomId,
            @Param("query") String query,
            Pageable pageable
    );

    /**
     * Kullanıcının TÜM mesajlarında ara (global search)
     *
     * Örnek: User A'nın gönderdiği veya aldığı tüm mesajlarda "proje" ara
     * Case-insensitive ve deleted mesajları exclude et
     */
    @Query("SELECT m FROM Message m WHERE " +
            "(m.senderId = :userId OR m.recipientId = :userId) AND " +
            "LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%')) AND " +
            "m.deletedAt IS NULL " +
            "ORDER BY m.createdAt DESC")
    Page<Message> searchUserMessages(
            @Param("userId") String userId,
            @Param("query") String query,
            Pageable pageable
    );

    /**
     * Belirli bir partner ile olan tüm mesajlarda ara
     * (chat room olmadan kullanışlı olabilir)
     */
    @Query("SELECT m FROM Message m WHERE " +
            "((m.senderId = :userId AND m.recipientId = :partnerId) OR " +
            "(m.senderId = :partnerId AND m.recipientId = :userId)) AND " +
            "LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%')) AND " +
            "m.deletedAt IS NULL " +
            "ORDER BY m.createdAt DESC")
    Page<Message> searchConversationMessages(
            @Param("userId") String userId,
            @Param("partnerId") String partnerId,
            @Param("query") String query,
            Pageable pageable
    );
}
