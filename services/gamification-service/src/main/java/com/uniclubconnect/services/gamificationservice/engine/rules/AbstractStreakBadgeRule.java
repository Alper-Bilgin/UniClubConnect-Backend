package com.uniclubconnect.services.gamificationservice.engine.rules;

import com.uniclubconnect.services.gamificationservice.dto.EventType;
import com.uniclubconnect.services.gamificationservice.dto.GamificationEvent;
import com.uniclubconnect.services.gamificationservice.engine.BadgeRule;
import com.uniclubconnect.services.gamificationservice.repository.UserBadgeRepository;
import com.uniclubconnect.services.gamificationservice.service.DailyStreakService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractStreakBadgeRule implements BadgeRule {

    private final UserBadgeRepository userBadgeRepository;
    private final DailyStreakService dailyStreakService;
    private final int targetStreak; // Hedef gün (7, 30, 365 vs.)
    private final String badgeId;   // Rozetin ID'si

    @Override
    public boolean supports(EventType eventType) {
        return eventType == EventType.USER_LOGIN;
    }

    @Override
    public boolean checkCondition(GamificationEvent event) {
        // Eğer rozet zaten varsa false dön
        if (userBadgeRepository.existsByUserIdAndBadgeId(event.getUserId(), badgeId)) {
            return false;
        }
        // Kullanıcının güncel serisi hedefi tutturdu mu?
        return dailyStreakService.getCurrentStreak(event.getUserId()) >= targetStreak;
    }

    @Override
    public String getBadgeId() {
        return badgeId;
    }
}
