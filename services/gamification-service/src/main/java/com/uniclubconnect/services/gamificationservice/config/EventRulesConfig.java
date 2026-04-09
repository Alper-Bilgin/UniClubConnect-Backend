package com.uniclubconnect.services.gamificationservice.config;

import com.uniclubconnect.services.gamificationservice.engine.rules.AbstractEventBadgeRule;
import com.uniclubconnect.services.gamificationservice.repository.UserBadgeRepository;
import com.uniclubconnect.services.gamificationservice.repository.UserPointRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventRulesConfig {

    @Bean public AbstractEventBadgeRule event1Rule(UserBadgeRepository b, UserPointRepository p) { return new AbstractEventBadgeRule(b, p, 1, "EVENT_1") {}; }
    @Bean public AbstractEventBadgeRule event5Rule(UserBadgeRepository b, UserPointRepository p) { return new AbstractEventBadgeRule(b, p, 5, "EVENT_5") {}; }
    @Bean public AbstractEventBadgeRule event10Rule(UserBadgeRepository b, UserPointRepository p) { return new AbstractEventBadgeRule(b, p, 10, "EVENT_10") {}; }
    @Bean public AbstractEventBadgeRule event50Rule(UserBadgeRepository b, UserPointRepository p) { return new AbstractEventBadgeRule(b, p, 50, "EVENT_50") {}; }
}
