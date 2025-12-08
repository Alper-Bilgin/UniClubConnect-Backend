package com.uniclubconnect.services.notificationservice.repository;

import com.uniclubconnect.services.notificationservice.entity.SentEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SentEmailRepository extends JpaRepository<SentEmail, Long> {
}