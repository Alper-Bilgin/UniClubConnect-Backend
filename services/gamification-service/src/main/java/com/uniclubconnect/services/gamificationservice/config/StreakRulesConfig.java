package com.uniclubconnect.services.gamificationservice.config;

import com.uniclubconnect.services.gamificationservice.engine.rules.AbstractStreakBadgeRule;
import com.uniclubconnect.services.gamificationservice.repository.UserBadgeRepository;
import com.uniclubconnect.services.gamificationservice.service.DailyStreakService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StreakRulesConfig {

    @Bean public AbstractStreakBadgeRule streak7Rule(UserBadgeRepository r, DailyStreakService s) {
        return new AbstractStreakBadgeRule(r, s, 7, "STREAK_7") {};
    }

    @Bean
    public AbstractStreakBadgeRule streak30Rule(UserBadgeRepository r, DailyStreakService s) {
        return new AbstractStreakBadgeRule(r, s, 30, "STREAK_30") {};
    }

    @Bean public AbstractStreakBadgeRule streak60Rule(UserBadgeRepository r, DailyStreakService s) {
        return new AbstractStreakBadgeRule(r, s, 60, "STREAK_60") {};
    }

    @Bean public AbstractStreakBadgeRule streak90Rule(UserBadgeRepository r, DailyStreakService s) {
        return new AbstractStreakBadgeRule(r, s, 90, "STREAK_90") {};
    }

    @Bean public AbstractStreakBadgeRule streak100Rule(UserBadgeRepository r, DailyStreakService s) {
        return new AbstractStreakBadgeRule(r, s, 100, "STREAK_100") {};
    }

    @Bean public AbstractStreakBadgeRule streak365Rule(UserBadgeRepository r, DailyStreakService s) {
        return new AbstractStreakBadgeRule(r, s, 365, "STREAK_365") {};
    }
}
