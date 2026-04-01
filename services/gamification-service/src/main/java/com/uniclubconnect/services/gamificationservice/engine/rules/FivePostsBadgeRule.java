package com.uniclubconnect.services.gamificationservice.engine.rules;

import com.uniclubconnect.services.gamificationservice.dto.EventType;
import com.uniclubconnect.services.gamificationservice.dto.GamificationEvent;
import com.uniclubconnect.services.gamificationservice.engine.BadgeRule;
import com.uniclubconnect.services.gamificationservice.model.UserPoint;
import com.uniclubconnect.services.gamificationservice.repository.UserBadgeRepository;
import com.uniclubconnect.services.gamificationservice.repository.UserPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FivePostsBadgeRule implements BadgeRule {

    private final UserBadgeRepository userBadgeRepository;
    private final UserPointRepository userPointRepository;

    @Override
    public boolean supports(EventType eventType) {
        // Bu kural sadece post atıldığında kontrol edilecek
        return eventType == EventType.POST_CREATED;
    }

    @Override
    public boolean checkCondition(GamificationEvent event) {
        String userId = event.getUserId();

        // Kullanıcıda bu rozet zaten varsa false dön (tekrar verme)
        if (userBadgeRepository.existsByUserIdAndBadgeId(userId, getBadgeId())) {
            return false;
        }

        // Kullanıcının post sayısını kontrol et
        UserPoint userPoint = userPointRepository.findById(userId).orElse(null);

        // 5 veya daha fazla post atmışsa rozeti HAK ETTİ! (true dön)
        return userPoint != null && userPoint.getPostCount() >= 5;
    }

    @Override
    public String getBadgeId() {
        return "FIVE_POSTS";
    }
}
