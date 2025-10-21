package com.uniclubconnect.services.profileservice.listener;

import com.uniclubconnect.services.profileservice.dto.UserCreatedEvent;
import com.uniclubconnect.services.profileservice.service.UserProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserCreatedListener {

    private static final Logger logger = LoggerFactory.getLogger(UserCreatedListener.class);

    @Autowired
    private UserProfileService userProfileService;

    // auth-service'in mesaj attığı kuyruğun adını dinle
    @RabbitListener(queues = "profile_user_created_queue")
    public void handleUserCreatedEvent(UserCreatedEvent event) {
        logger.info("Alınan 'user.created' olayı: AuthId {}", event.getAuthId());
        try {
            userProfileService.createProfileFromEvent(event);
        } catch (Exception e) {
            logger.error("Profil oluşturulurken hata (AuthId: {}): {}", event.getAuthId(), e.getMessage());
            // Hata yönetimi (örn: ölü mektup kuyruğu - Dead Letter Queue)
        }
    }
}