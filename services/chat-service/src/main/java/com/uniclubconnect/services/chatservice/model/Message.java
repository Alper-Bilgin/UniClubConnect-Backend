package com.uniclubconnect.services.chatservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String chatRoomId;
    private String senderId;
    private String recipientId; // Eğer birebir mesaj ise

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private MessageStatus status;

    @Column(unique = true, nullable = false)
    private String clientMessageId; // YENİ

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Builder.Default
    private boolean isDeleted = false;
}
