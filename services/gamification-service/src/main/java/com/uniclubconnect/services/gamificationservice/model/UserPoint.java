package com.uniclubconnect.services.gamificationservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_points")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPoint {

    @Id
    private String userId; // Auth/Profile servisindeki ID ile aynı olacak

    @Builder.Default
    private int totalXp = 0; // Toplam deneyim puanı

    @Builder.Default
    private int currentLevel = 1; // Kullanıcının seviyesi (Level)

    @Builder.Default
    private int postCount = 0;

    @Builder.Default
    private int likeCount = 0; // Toplam yaptığı beğeni

    @Builder.Default
    private int commentCount = 0; // Toplam yaptığı yorum
}
