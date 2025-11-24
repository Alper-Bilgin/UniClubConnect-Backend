package com.uniclubconnect.services.eventservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private String location; // "Konferans Salonu" veya "Online"

    private String eventLink; // "youtube.com/..." (Boş olabilir)

    @Column(nullable = false)
    private LocalDateTime eventDateTime;

    private String imageUrl; // MinIO URL

    // Null ise = Sınırsız Kontenjan
    // Sayı ise = Sınırlı Kontenjan
    private Integer totalQuota;

    @Column(nullable = false)
    private Long clubId;

    @Column(nullable = false)
    private String organizerAuthId; // Kulüp Sahibi ID

    @CreationTimestamp
    private LocalDateTime createdDate;
}
