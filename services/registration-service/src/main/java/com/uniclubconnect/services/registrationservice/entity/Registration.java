package com.uniclubconnect.services.registrationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "registrations", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"eventId", "userAuthId"}) // Çifte kayıt engelleme
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long eventId;

    // Kullanıcıya göstermek için etkinlik adını da kaydedelim (Performans için)
    private String eventTitle;
    private LocalDateTime eventDate;
    private String eventLocation;

    @Column(nullable = false)
    private String userAuthId;

    @Column(nullable = false)
    private String userEmail;

    private String userName; // Bilet kontrolünde "Merhaba Ahmet" demek için

    @Column(nullable = false, unique = true)
    private String ticketCode; // Örn: "f47ac10b-58cc-4372-a567-0e02b2c3d479"

    @Enumerated(EnumType.STRING)
    private ERegistrationStatus status;

    @CreationTimestamp
    private LocalDateTime registrationDate;
}
