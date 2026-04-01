package com.uniclubconnect.services.gamificationservice.repository;

import com.uniclubconnect.services.gamificationservice.model.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserBadgeRepository extends JpaRepository<UserBadge, String> {
    List<UserBadge> findByUserId(String userId);
}
