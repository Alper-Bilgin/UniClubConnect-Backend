package com.uniclubconnect.services.chatservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoom {
    @Id
    private String id; // 1-1 için "user1Id_user2Id", Gruplar için UUID

    @Enumerated(EnumType.STRING)
    private ChatType type;

    private String name; // Grup adı ise dolu olur

    @CreationTimestamp
    private LocalDateTime createdAt;
}
