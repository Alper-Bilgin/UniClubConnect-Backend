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

    @Transactional // Puan ekleme ve rozet verme işlemleri tek bir veritabanı işleminde (Transaction) yapılsın
    public void processEvent(GamificationEvent event) {
        String userId = event.getUserId();

        // 1. HER İŞLEM İÇİN STANDART PUAN EKLE (+10 XP)
        UserPoint userPoint = userPointRepository.findById(userId)
                .orElse(UserPoint.builder().userId(userId).totalXp(0).currentLevel(1).postCount(0).build());

        userPoint.setTotalXp(userPoint.getTotalXp() + 10);
        userPoint.setCurrentLevel((userPoint.getTotalXp() / 100) + 1);

        // 👇 YENİ: Eğer post atıldıysa sayacı 1 artır 👇
        if (event.getEventType() == com.uniclubconnect.services.gamificationservice.dto.EventType.POST_CREATED) {
            userPoint.setPostCount(userPoint.getPostCount() + 1);
        }

        userPointRepository.save(userPoint); // Kaydet

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
