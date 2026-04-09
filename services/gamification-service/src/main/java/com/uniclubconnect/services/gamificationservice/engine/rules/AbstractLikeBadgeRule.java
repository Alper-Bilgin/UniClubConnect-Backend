package com.uniclubconnect.services.gamificationservice.engine.rules;

import com.uniclubconnect.services.gamificationservice.dto.EventType;
import com.uniclubconnect.services.gamificationservice.dto.GamificationEvent;
import com.uniclubconnect.services.gamificationservice.engine.BadgeRule;
import com.uniclubconnect.services.gamificationservice.model.UserPoint;
import com.uniclubconnect.services.gamificationservice.repository.UserBadgeRepository;
import com.uniclubconnect.services.gamificationservice.repository.UserPointRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractLikeBadgeRule implements BadgeRule {

    private final UserBadgeRepository userBadgeRepository;
    private final UserPointRepository userPointRepository;
    private final int targetCount;
    private final String badgeId;

    @Override
    public boolean supports(EventType eventType) {
        return eventType == EventType.POST_LIKED;
    }

    @Override
    public boolean checkCondition(GamificationEvent event) {
        if (userBadgeRepository.existsByUserIdAndBadgeId(event.getUserId(), badgeId)) {
            return false;
        }
        UserPoint userPoint = userPointRepository.findById(event.getUserId()).orElse(null);
        return userPoint != null && userPoint.getLikeCount() >= targetCount;
    }

    @Override
    public String getBadgeId() {
        return badgeId;
    }
}
