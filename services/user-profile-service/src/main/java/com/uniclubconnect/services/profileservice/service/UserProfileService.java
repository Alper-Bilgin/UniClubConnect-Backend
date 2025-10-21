package com.uniclubconnect.services.profileservice.service;

import com.uniclubconnect.services.profileservice.dto.UserCreatedEvent;
import com.uniclubconnect.services.profileservice.entity.UserProfile;
import com.uniclubconnect.services.profileservice.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileService.class);

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Transactional
    public void createProfileFromEvent(UserCreatedEvent event) {
        // Bu kullanıcı için zaten bir profil oluşturulmuş mu? (Mesaj tekrarı durumunda)
        if (userProfileRepository.existsByAuthId(event.getAuthId())) {
            logger.warn("Profil zaten mevcut (authId: {}). Mesaj tekrarı göz ardı ediliyor.", event.getAuthId());
            return;
        }

        // Gelen event DTO'sunu, veritabanı Entity'sine dönüştür
        UserProfile newProfile = UserProfile.builder()
                .authId(event.getAuthId())
                .email(event.getEmail())
                .firstName(event.getFirstName())
                .lastName(event.getLastName())
                .totalPoints(0L) // Başlangıç puanı
                .build();

        userProfileRepository.save(newProfile);

        logger.info("Yeni profil başarıyla oluşturuldu. AuthId: {}", event.getAuthId());
    }
}