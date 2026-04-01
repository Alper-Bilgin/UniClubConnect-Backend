package com.uniclubconnect.services.gamificationservice.model;
import jakarta.persistence.Id;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.Table;
@Entity
@Table(name = "badges")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class Badge {
    @Id
    private String id; // Örn: "FIRST_POST", "LOGIN_STREAK_7"

    private String name; // Örn: "İlk Söz"
    private String description; // Örn: "Sistemde ilk gönderini paylaştın!"
    private String iconUrl; // Rozetin görseli
    private int xpReward; // Bu rozeti kazanana verilecek ekstra puan
}
