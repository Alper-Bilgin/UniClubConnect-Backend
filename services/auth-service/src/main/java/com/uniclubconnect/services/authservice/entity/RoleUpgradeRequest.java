package com.uniclubconnect.services.authservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "role_upgrade_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleUpgradeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // İsteği yapan kullanıcı
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User requestingUser;

    // İstenen rol (Şimdilik sabit, gelecekte dinamik olabilir)
    // Bu örnekte bu alanı kullanmayacağız ama ileride gerekebilir.
    // @ManyToOne
    // @JoinColumn(name = "role_id")
    // private Role requestedRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ERoleRequestStatus status;

    // İsteği inceleyen admin (onay/red durumunda dolar)
    private String reviewedByAdminId; // Admin'in authId'si

    @CreationTimestamp
    private LocalDateTime requestDate;

    @UpdateTimestamp
    private LocalDateTime resolutionDate;
}