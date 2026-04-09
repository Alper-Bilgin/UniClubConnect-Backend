package com.uniclubconnect.services.gamificationservice.service;

import com.uniclubconnect.services.gamificationservice.dto.GamificationEvent;
import com.uniclubconnect.services.gamificationservice.engine.BadgeRule;
import com.uniclubconnect.services.gamificationservice.model.UserBadge;
import com.uniclubconnect.services.gamificationservice.model.UserPoint;
import com.uniclubconnect.services.gamificationservice.repository.BadgeRepository;
import com.uniclubconnect.services.gamificationservice.repository.UserBadgeRepository;
import com.uniclubconnect.services.gamificationservice.repository.UserPointRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GamificationEngine {

    private final List<BadgeRule> rules;
    private final UserPointRepository userPointRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final BadgeRepository badgeRepository;

    private final DailyStreakService dailyStreakService;

    @Transactional // Puan ekleme ve rozet verme işlemleri tek bir veritabanı işleminde (Transaction) yapılsın
    public void processEvent(GamificationEvent event) {
        String userId = event.getUserId();

        // YENİ EKLENEN HİLE KORUMASI
        if (event.getEventType() == com.uniclubconnect.services.gamificationservice.dto.EventType.USER_LOGIN) {
            boolean isValidLogin = dailyStreakService.processLogin(userId);
            if (!isValidLogin) {
                log.info("Kullanıcı {} bugün zaten giriş yapmış. İşlem (XP/Rozet) iptal edildi.", userId);
                return;
            }
        }

        // 1. HER İŞLEM İÇİN STANDART PUAN EKLE (+10 XP)
        UserPoint userPoint = userPointRepository.findById(userId)
                .orElse(UserPoint.builder().userId(userId).totalXp(0).currentLevel(1).postCount(0).likeCount(0).commentCount(0).build());

        userPoint.setTotalXp(userPoint.getTotalXp() + 10);
        userPoint.setCurrentLevel((userPoint.getTotalXp() / 100) + 1);

        // 👇 TEMİZLENMİŞ SAYAÇ ARTIRMA MANTIĞI 👇
        switch (event.getEventType()) {
            case POST_CREATED -> userPoint.setPostCount(userPoint.getPostCount() + 1);
            case POST_LIKED -> userPoint.setLikeCount(userPoint.getLikeCount() + 1);
            case COMMENT_ADDED -> userPoint.setCommentCount(userPoint.getCommentCount() + 1);
        }

        // Değişiklikleri Kaydet
        userPointRepository.save(userPoint);

        // 2. KURAL MOTORUNU ÇALIŞTIR VE ROZETLERİ DAĞIT
        for (BadgeRule rule : rules) {
            if (rule.supports(event.getEventType())) {
                if (rule.checkCondition(event)) {
                    awardBadge(userId, rule.getBadgeId(), userPoint);
                }
            }
        }
    }

    private void awardBadge(String userId, String badgeId, UserPoint userPoint) {
        // Eğer kullanıcıda bu rozet ZATEN YOKSA rozeti ver
        if (!userBadgeRepository.existsByUserIdAndBadgeId(userId, badgeId)) {
            badgeRepository.findById(badgeId).ifPresent(badge -> {

                // Rozeti Kaydet
                userBadgeRepository.save(UserBadge.builder()
                        .userId(userId)
                        .badge(badge)
                        .earnedAt(LocalDateTime.now())
                        .build());

                // Rozetin Ekstra Puanını Ekle
                userPoint.setTotalXp(userPoint.getTotalXp() + badge.getXpReward());
                userPointRepository.save(userPoint);

                log.info("🏆 TEBRİKLER! Kullanıcı: {} | Kazanılan Rozet: {} | Ekstra Puan: +{}",
                        userId, badge.getName(), badge.getXpReward());
            });
        }
    }
}
