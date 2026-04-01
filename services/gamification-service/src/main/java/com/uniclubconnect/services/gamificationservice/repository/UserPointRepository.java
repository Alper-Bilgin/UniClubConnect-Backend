package com.uniclubconnect.services.gamificationservice.repository;

import com.uniclubconnect.services.gamificationservice.model.UserPoint;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserPointRepository extends JpaRepository<UserPoint, String> {}
