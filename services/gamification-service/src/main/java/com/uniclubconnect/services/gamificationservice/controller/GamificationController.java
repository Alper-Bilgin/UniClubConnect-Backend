package com.uniclubconnect.services.gamificationservice.controller;

import com.uniclubconnect.services.gamificationservice.model.DailyStreak;
import com.uniclubconnect.services.gamificationservice.model.UserBadge;
import com.uniclubconnect.services.gamificationservice.model.UserPoint;
import com.uniclubconnect.services.gamificationservice.service.GamificationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gamification")
@RequiredArgsConstructor
public class GamificationController {

    private final GamificationQueryService queryService;

    // 1. Kullanıcının Puanı ve Seviyesi
    @GetMapping("/{userId}/points")
    public ResponseEntity<UserPoint> getUserPoints(@PathVariable String userId) {
        return ResponseEntity.ok(queryService.getUserPoints(userId));
    }

    // 2. Kullanıcının Kazandığı Rozetler
    @GetMapping("/{userId}/badges")
    public ResponseEntity<List<UserBadge>> getUserBadges(@PathVariable String userId) {
        return ResponseEntity.ok(queryService.getUserBadges(userId));
    }

    // 3. Ham Streak Verisi (Alternatif)
    @GetMapping("/{userId}/streak")
    public ResponseEntity<DailyStreak> getUserStreak(@PathVariable String userId) {
        return ResponseEntity.ok(queryService.getUserStreak(userId));
    }

    // 4. Hepsi Bir Arada (Özet Ekranı İçin)
    @GetMapping("/{userId}/summary")
    public ResponseEntity<Map<String, Object>> getUserSummary(@PathVariable String userId) {
        return ResponseEntity.ok(Map.of(
                "points", queryService.getUserPoints(userId),
                "streak", queryService.getUserStreak(userId),
                "badges", queryService.getUserBadges(userId)
        ));
    }

    // 👇 YENİ EKLENENLER 👇

    // 5. Liderlik Tablosu (Top 10)
    @GetMapping("/leaderboard")
    public ResponseEntity<List<UserPoint>> getLeaderboard() {
        return ResponseEntity.ok(queryService.getLeaderboard());
    }

    // 6. Belirli bir kullanıcının güncel seri durumu (Daha formatlı)
    @GetMapping("/{userId}/streak-info")
    public ResponseEntity<Map<String, Object>> getStreakInfo(@PathVariable String userId) {
        DailyStreak streak = queryService.getUserStreak(userId);
        return ResponseEntity.ok(Map.of(
                "currentStreak", streak.getCurrentStreak(),
                "longestStreak", streak.getLongestStreak(),
                "lastLogin", streak.getLastLoginDate() != null ? streak.getLastLoginDate() : "Hiç giriş yapılmadı"
        ));
    }
}