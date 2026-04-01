package com.uniclubconnect.services.gamificationservice.engine;

import com.uniclubconnect.services.gamificationservice.dto.EventType;
import com.uniclubconnect.services.gamificationservice.dto.GamificationEvent;

public interface BadgeRule {
    boolean supports(EventType eventType); // Bu kural hangi olayı dinliyor?
    boolean checkCondition(GamificationEvent event); // Kullanıcı rozeti hak etti mi?
    String getBadgeId(); // Hangi rozeti vereceğiz? (Örn: "HELLO_WORLD")
}
