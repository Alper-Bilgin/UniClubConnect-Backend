package com.uniclubconnect.services.gamificationservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "daily_streaks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyStreak {
    @Id
    private String userId;

    @Builder.Default
    private int currentStreak = 0; // Şu anki aralıksız giriş günü

    @Builder.Default
    private int longestStreak = 0; // Bugüne kadarki en iyi serisi

    private LocalDate lastLoginDate; // Son giriş yaptığı gün
}
