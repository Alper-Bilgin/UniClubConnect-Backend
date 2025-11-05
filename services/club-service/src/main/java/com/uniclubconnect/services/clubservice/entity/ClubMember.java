package com.uniclubconnect.services.clubservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "club_members", uniqueConstraints = {
        // Bir kullanıcı bir kulübe sadece 1 kez üye olabilir
        @UniqueConstraint(columnNames = {"club_id", "userAuthId"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @Column(nullable = false)
    private String userAuthId; // Üyenin UUID'si

    @CreationTimestamp
    private LocalDateTime joinDate;
}