package com.uniclubconnect.services.chatservice.repository;

import com.uniclubconnect.services.chatservice.model.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {

    /**
     * Kullanıcının arşivlenmemiş konuşmalarını son mesaj zamanına göre getir
     */
    @Query(value = "SELECT c FROM Conversation c WHERE " +
            "(c.participant1Id = :userId AND c.isArchivedP1 = false) OR " +
            "(c.participant2Id = :userId AND c.isArchivedP2 = false) " +
            "ORDER BY c.lastMessageTime DESC NULLS LAST")
    Page<Conversation> findUserConversations(@Param("userId") String userId, Pageable pageable);

    /**
     * Kullanıcının arşivlenmiş konuşmalarını getir
     */
    @Query(value = "SELECT c FROM Conversation c WHERE " +
            "(c.participant1Id = :userId AND c.isArchivedP1 = true) OR " +
            "(c.participant2Id = :userId AND c.isArchivedP2 = true) " +
            "ORDER BY c.lastMessageTime DESC NULLS LAST")
    Page<Conversation> findUserArchivedConversations(@Param("userId") String userId, Pageable pageable);

    /**
     * Kullanıcının okunmamış konuşmalarını getir
     */
    @Query(value = "SELECT c FROM Conversation c WHERE " +
            "((c.participant1Id = :userId AND c.unreadCountP1 > 0 AND c.isArchivedP1 = false) OR " +
            "(c.participant2Id = :userId AND c.unreadCountP2 > 0 AND c.isArchivedP2 = false)) " +
            "ORDER BY c.lastMessageTime DESC")
    Page<Conversation> findUnreadConversations(@Param("userId") String userId, Pageable pageable);

    /**
     * Participant1 için okunmamış sayıyı güncelle
     */
    @Modifying
    @Query("UPDATE Conversation c SET c.unreadCountP1 = :count WHERE c.id = :conversationId")
    void updateUnreadCountP1(@Param("conversationId") String conversationId, @Param("count") int count);

    /**
     * Participant2 için okunmamış sayıyı güncelle
     */
    @Modifying
    @Query("UPDATE Conversation c SET c.unreadCountP2 = :count WHERE c.id = :conversationId")
    void updateUnreadCountP2(@Param("conversationId") String conversationId, @Param("count") int count);

    /**
     * Participant1 için arşiv durumunu güncelle
     */
    @Modifying
    @Query("UPDATE Conversation c SET c.isArchivedP1 = :archived WHERE c.id = :conversationId")
    void updateArchiveStatusP1(@Param("conversationId") String conversationId, @Param("archived") boolean archived);

    /**
     * Participant2 için arşiv durumunu güncelle
     */
    @Modifying
    @Query("UPDATE Conversation c SET c.isArchivedP2 = :archived WHERE c.id = :conversationId")
    void updateArchiveStatusP2(@Param("conversationId") String conversationId, @Param("archived") boolean archived);
}