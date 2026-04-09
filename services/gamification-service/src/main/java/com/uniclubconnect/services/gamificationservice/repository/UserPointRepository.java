package com.uniclubconnect.services.gamificationservice.repository;

import com.uniclubconnect.services.gamificationservice.model.UserPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserPointRepository extends JpaRepository<UserPoint, String> {
    List<UserPoint> findTop10ByOrderByTotalXpDesc();
}
