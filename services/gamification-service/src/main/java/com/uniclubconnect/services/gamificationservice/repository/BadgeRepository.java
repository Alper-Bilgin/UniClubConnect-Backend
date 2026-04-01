package com.uniclubconnect.services.gamificationservice.repository;

import com.uniclubconnect.services.gamificationservice.model.Badge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BadgeRepository extends JpaRepository<Badge, String> {}
