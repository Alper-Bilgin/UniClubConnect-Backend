package com.uniclubconnect.services.chatservice.model;

import jakarta.persistence.*;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversations", indexes = {
        @Index(name = "idx_conv_p1", columnList = "participant_1_id"),
        @Index(name = "idx_conv_p2", columnList = "participant_2_id"),
        @Index(name = "idx_conv_last_msg_time", columnList = "last_message_time DESC")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    private String id;

    @Column(name = "participant_1_id", nullable = false)
    private String participant1Id;

    @Column(name = "participant_2_id", nullable = false)
    private String participant2Id;

    private String lastMessageId;

    @Column(columnDefinition = "VARCHAR(255)")
    private String lastMessagePreview;

    private String lastMessageSenderId;

    @Column(name = "last_message_time")
    private LocalDateTime lastMessageTime;

    @Builder.Default
    @Column(name = "unread_count_p1")
    private int unreadCountP1 = 0;

    @Builder.Default
    @Column(name = "unread_count_p2")
    private int unreadCountP2 = 0;

    @Builder.Default
    @Column(name = "is_archived_p1")
    private boolean isArchivedP1 = false;

    @Builder.Default
    @Column(name = "is_archived_p2")
    private boolean isArchivedP2 = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
