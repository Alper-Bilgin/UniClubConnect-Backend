package com.uniclubconnect.services.gamificationservice.engine.rules;

import com.uniclubconnect.services.gamificationservice.dto.EventType;
import com.uniclubconnect.services.gamificationservice.dto.GamificationEvent;
import com.uniclubconnect.services.gamificationservice.engine.BadgeRule;
import com.uniclubconnect.services.gamificationservice.repository.UserBadgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FirstLoginBadgeRule implements BadgeRule {

    private final UserBadgeRepository userBadgeRepository;

    @Override
    public boolean supports(EventType eventType) {
        // Bu kural sadece kullanıcı giriş yaptığında çalışır
        return eventType == EventType.USER_LOGIN;
    }

    @Override
    public boolean checkCondition(GamificationEvent event) {
        // Zaten rozeti yoksa "Evet, rozeti ver" (true) diyoruz.
        return !userBadgeRepository.existsByUserIdAndBadgeId(event.getUserId(), getBadgeId());
    }

    @Override
    public String getBadgeId() {
        return "HELLO_WORLD";
    }
}