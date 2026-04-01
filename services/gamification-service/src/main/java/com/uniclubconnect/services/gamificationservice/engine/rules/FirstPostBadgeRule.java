package com.uniclubconnect.services.gamificationservice.engine.rules;

import com.uniclubconnect.services.gamificationservice.dto.EventType;
import com.uniclubconnect.services.gamificationservice.dto.GamificationEvent;
import com.uniclubconnect.services.gamificationservice.engine.BadgeRule;
import com.uniclubconnect.services.gamificationservice.repository.UserBadgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FirstPostBadgeRule implements BadgeRule {

    private final UserBadgeRepository userBadgeRepository;

    @Override
    public boolean supports(EventType eventType) {
        // Bu kural sadece yeni bir post atıldığında çalışır
        return eventType == EventType.POST_CREATED;
    }

    @Override
    public boolean checkCondition(GamificationEvent event) {
        return !userBadgeRepository.existsByUserIdAndBadgeId(event.getUserId(), getBadgeId());
    }

    @Override
    public String getBadgeId() {
        return "FIRST_POST";
    }
}
