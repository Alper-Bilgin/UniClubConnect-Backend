package com.uniclubconnect.services.gamificationservice.engine;

import com.uniclubconnect.services.gamificationservice.dto.GamificationEvent;

public interface BadgeRule {
    // Bu kural gelen event tipini destekliyor mu?
    boolean supports(GamificationEvent event);

    // Rozet kazanıldı mı?
    boolean checkAndAward(GamificationEvent event);
}
