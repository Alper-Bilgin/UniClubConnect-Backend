package com.uniclubconnect.services.gamificationservice.listener;

import com.uniclubconnect.services.gamificationservice.dto.GamificationEvent;
import com.uniclubconnect.services.gamificationservice.service.GamificationEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GamificationEventListener {

    private final GamificationEngine gamificationEngine;

    @RabbitListener(
            queues = "${gamification.rabbitmq.queue}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleEvent(GamificationEvent event) {
        if (event == null || event.getEventType() == null) {
            log.warn("Geçersiz veya boş gamification olayı alındı!");
            return;
        }

        try {
            gamificationEngine.processEvent(event);
        } catch (Exception e) {
            log.error("Olay işlenirken kritik hata! Event: {}", event.getEventType(), e);
            throw e; // Hatalı mesajları kaybetmemek (DLQ için) fırlatıyoruz
        }
    }
}
