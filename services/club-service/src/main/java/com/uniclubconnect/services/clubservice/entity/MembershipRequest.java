package com.uniclubconnect.services.clubservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "membership_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @Column(nullable = false)
    private String userAuthId; // İstek atan kullanıcının UUID'si

    @Column(nullable = false)
    private String userEmail; // Kolaylık için email alanı

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ERequestStatus status;

    @CreationTimestamp
    private LocalDateTime requestDate;
}