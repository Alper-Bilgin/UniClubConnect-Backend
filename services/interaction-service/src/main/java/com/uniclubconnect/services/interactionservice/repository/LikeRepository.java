package com.uniclubconnect.services.interactionservice.repository;

import com.uniclubconnect.services.interactionservice.entity.ETargetType;
import com.uniclubconnect.services.interactionservice.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {
    Optional<Like> findByUserIdAndTargetIdAndTargetType(String userId, String targetId, ETargetType targetType);
    long countByTargetIdAndTargetType(String targetId, ETargetType targetType);
}
