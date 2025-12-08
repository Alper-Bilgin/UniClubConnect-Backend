package com.uniclubconnect.services.notificationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "sent_emails")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SentEmail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String toEmail;
    private String subject;

    @Column(length = 2000) // İçeriğin bir kısmını saklamak için
    private String contentPreview;

    private String messageType; // "WELCOME", "TICKET"

    @CreationTimestamp
    private LocalDateTime sentAt;

    private String status; // "SENT", "ERROR"
    private String errorMessage; // Hata varsa detayını tut
}