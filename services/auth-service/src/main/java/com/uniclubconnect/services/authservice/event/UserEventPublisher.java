package com.uniclubconnect.services.authservice.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class UserEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(UserEventPublisher.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.routingkey.user_created}")
    private String userCreatedRoutingKey;

    public void publishUserCreated(UserCreatedEvent event) {
        try {
            logger.info("Kullanıcı oluşturuldu olayı yayınlanıyor: {}", event.getAuthId());
            rabbitTemplate.convertAndSend(exchangeName, userCreatedRoutingKey, event);
        } catch (Exception e) {
            logger.error("RabbitMQ'ya 'user.created' olayı gönderilemedi: {}", e.getMessage());
            // Burada hata yönetimi yapılabilir (örn: tekrar deneme mekanizması)
        }
    }
}