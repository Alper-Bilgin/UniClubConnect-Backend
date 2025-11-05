package com.uniclubconnect.services.clubservice.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ClubEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(ClubEventPublisher.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${spring.rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${spring.rabbitmq.routingkey.user_joined_club}")
    private String userJoinedClubRoutingKey;

    public void publishUserJoinedClub(UserJoinedClubEvent event) {
        try {
            logger.info("Kullanıcı kulübe katıldı olayı yayınlanıyor: {} -> ClubId {}", event.getUserAuthId(), event.getClubId());
            rabbitTemplate.convertAndSend(exchangeName, userJoinedClubRoutingKey, event);
        } catch (Exception e) {
            logger.error("RabbitMQ'ya 'user.joined.club' olayı gönderilemedi: {}", e.getMessage());
        }
    }
}