package com.uniclubconnect.services.gamificationservice.service;

import com.uniclubconnect.services.gamificationservice.dto.GamificationEvent;
import com.uniclubconnect.services.gamificationservice.engine.BadgeRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GamificationEngine {

    // Spring, BadgeRule'u implemente eden tüm sınıfları buraya otomatik doldurur!
    private final List<BadgeRule> rules;

    public void processEvent(GamificationEvent event) {
        log.info("Gamification olayı işleniyor... Tip: {}, Kullanıcı: {}", event.getEventType(), event.getUserId());

        // 1. (Gelecekte buraya XP/Puan ekleme mantığı yazacağız)

        // 2. Rozet Kurallarını Çalıştır
        int awardedBadges = 0;
        for (BadgeRule rule : rules) {
            if (rule.supports(event)) {
                boolean awarded = rule.checkAndAward(event);
                if (awarded) awardedBadges++;
            }
        }

        if (awardedBadges > 0) {
            log.info("Kullanıcı {} için {} yeni rozet kazanıldı!", event.getUserId(), awardedBadges);
        }
    }
}
