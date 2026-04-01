package com.uniclubconnect.services.gamificationservice.repository;

import com.uniclubconnect.services.gamificationservice.model.DailyStreak;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyStreakRepository extends JpaRepository<DailyStreak, String> {}
