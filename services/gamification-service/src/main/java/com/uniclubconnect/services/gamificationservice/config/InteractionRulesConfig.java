package com.uniclubconnect.services.gamificationservice.config;

import com.uniclubconnect.services.gamificationservice.engine.rules.AbstractCommentBadgeRule;
import com.uniclubconnect.services.gamificationservice.engine.rules.AbstractLikeBadgeRule;
import com.uniclubconnect.services.gamificationservice.repository.UserBadgeRepository;
import com.uniclubconnect.services.gamificationservice.repository.UserPointRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InteractionRulesConfig {

    // BEĞENİ KURALLARI (1, 5, 10, 50, 100, 500, 1000)
    @Bean public AbstractLikeBadgeRule like1Rule(UserBadgeRepository b, UserPointRepository p) { return new AbstractLikeBadgeRule(b, p, 1, "LIKE_1") {}; }
    @Bean public AbstractLikeBadgeRule like5Rule(UserBadgeRepository b, UserPointRepository p) { return new AbstractLikeBadgeRule(b, p, 5, "LIKE_5") {}; }
    @Bean public AbstractLikeBadgeRule like10Rule(UserBadgeRepository b, UserPointRepository p) { return new AbstractLikeBadgeRule(b, p, 10, "LIKE_10") {}; }
    @Bean public AbstractLikeBadgeRule like50Rule(UserBadgeRepository b, UserPointRepository p) { return new AbstractLikeBadgeRule(b, p, 50, "LIKE_50") {}; }
    @Bean public AbstractLikeBadgeRule like100Rule(UserBadgeRepository b, UserPointRepository p) { return new AbstractLikeBadgeRule(b, p, 100, "LIKE_100") {}; }
    @Bean public AbstractLikeBadgeRule like500Rule(UserBadgeRepository b, UserPointRepository p) { return new AbstractLikeBadgeRule(b, p, 500, "LIKE_500") {}; }
    @Bean public AbstractLikeBadgeRule like1000Rule(UserBadgeRepository b, UserPointRepository p) { return new AbstractLikeBadgeRule(b, p, 1000, "LIKE_1000") {}; }

    // YORUM KURALLARI (1, 5, 10, 50, 100, 500, 1000)
    @Bean public AbstractCommentBadgeRule comment1Rule(UserBadgeRepository b, UserPointRepository p) { return new AbstractCommentBadgeRule(b, p, 1, "COMMENT_1") {}; }
    @Bean public AbstractCommentBadgeRule comment5Rule(UserBadgeRepository b, UserPointRepository p) { return new AbstractCommentBadgeRule(b, p, 5, "COMMENT_5") {}; }
    @Bean public AbstractCommentBadgeRule comment10Rule(UserBadgeRepository b, UserPointRepository p) { return new AbstractCommentBadgeRule(b, p, 10, "COMMENT_10") {}; }
    @Bean public AbstractCommentBadgeRule comment50Rule(UserBadgeRepository b, UserPointRepository p) { return new AbstractCommentBadgeRule(b, p, 50, "COMMENT_50") {}; }
    @Bean public AbstractCommentBadgeRule comment100Rule(UserBadgeRepository b, UserPointRepository p) { return new AbstractCommentBadgeRule(b, p, 100, "COMMENT_100") {}; }
    @Bean public AbstractCommentBadgeRule comment500Rule(UserBadgeRepository b, UserPointRepository p) { return new AbstractCommentBadgeRule(b, p, 500, "COMMENT_500") {}; }
    @Bean public AbstractCommentBadgeRule comment1000Rule(UserBadgeRepository b, UserPointRepository p) { return new AbstractCommentBadgeRule(b, p, 1000, "COMMENT_1000") {}; }
}
