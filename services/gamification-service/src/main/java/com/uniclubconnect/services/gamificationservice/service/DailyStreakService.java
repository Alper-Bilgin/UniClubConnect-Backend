package com.uniclubconnect.services.gamificationservice.service;

import com.uniclubconnect.services.gamificationservice.model.DailyStreak;
import com.uniclubconnect.services.gamificationservice.repository.DailyStreakRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DailyStreakService {

    private final DailyStreakRepository dailyStreakRepository;

    // 1. GİRİŞ KONTROLÜ VE SERİ GÜNCELLEME
    public boolean processLogin(String userId) {
        DailyStreak streak = dailyStreakRepository.findById(userId)
                .orElse(DailyStreak.builder().userId(userId).currentStreak(0).longestStreak(0).build());

        LocalDate today = LocalDate.now();
        LocalDate lastLogin = streak.getLastLoginDate();

        // Senaryo A: Bugün zaten girmiş (Hile Koruması - XP Verme)
        if (lastLogin != null && lastLogin.isEqual(today)) {
            return false;
        }

        // Senaryo B: Dün girmiş (Seriyi Artır)
        if (lastLogin != null && lastLogin.isEqual(today.minusDays(1))) {
            streak.setCurrentStreak(streak.getCurrentStreak() + 1);
        }
        // Senaryo C: Seri bozulmuş veya ilk defa giriyor (Seriyi Sıfırla)
        else {
            streak.setCurrentStreak(1);
        }

        // En uzun seriyi rekor olarak kaydet
        if (streak.getCurrentStreak() > streak.getLongestStreak()) {
            streak.setLongestStreak(streak.getCurrentStreak());
        }

        streak.setLastLoginDate(today);
        dailyStreakRepository.save(streak);

        return true; // Başarılı, XP ve rozet verilebilir
    }

    // 2. KULLANICININ KAÇINCI GÜNÜNDE OLDUĞUNU DÖNEN FONKSİYON (Kurallar için)
    public int getCurrentStreak(String userId) {
        return dailyStreakRepository.findById(userId)
                .map(DailyStreak::getCurrentStreak)
                .orElse(0);
    }
}
