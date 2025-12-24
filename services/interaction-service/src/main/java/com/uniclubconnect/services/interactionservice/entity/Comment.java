package com.uniclubconnect.services.interactionservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId; // Yorumu yapan

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false)
    private String targetId; // Post ID veya Event ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ETargetType targetType; // POST mu EVENT mi?

    @CreationTimestamp
    private LocalDateTime createdAt;
}