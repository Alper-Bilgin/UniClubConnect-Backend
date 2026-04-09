package com.uniclubconnect.services.gamificationservice.service;

import com.uniclubconnect.services.gamificationservice.model.DailyStreak;
import com.uniclubconnect.services.gamificationservice.model.UserBadge;
import com.uniclubconnect.services.gamificationservice.model.UserPoint;
import com.uniclubconnect.services.gamificationservice.repository.DailyStreakRepository;
import com.uniclubconnect.services.gamificationservice.repository.UserBadgeRepository;
import com.uniclubconnect.services.gamificationservice.repository.UserPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GamificationQueryService {

    private final UserPointRepository userPointRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final DailyStreakRepository dailyStreakRepository;

    public UserPoint getUserPoints(String userId) {
        // Eğer kullanıcı henüz puan kazanmadıysa, default 0 puanla boş bir nesne dön
        return userPointRepository.findById(userId)
                .orElse(UserPoint.builder().userId(userId).totalXp(0).currentLevel(1).build());
    }

    public List<UserBadge> getUserBadges(String userId) {
        return userBadgeRepository.findAll(); // TODO: Repository'ye findByUserId metodu eklenecek
    }

    public DailyStreak getUserStreak(String userId) {
        return dailyStreakRepository.findById(userId)
                .orElse(DailyStreak.builder().userId(userId).currentStreak(0).longestStreak(0).build());
    }

    public List<UserPoint> getLeaderboard() {
        return userPointRepository.findTop10ByOrderByTotalXpDesc();
    }
}
