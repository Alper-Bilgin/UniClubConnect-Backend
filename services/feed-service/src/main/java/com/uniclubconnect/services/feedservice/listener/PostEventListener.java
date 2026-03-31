package com.uniclubconnect.services.feedservice.listener;

import com.uniclubconnect.services.feedservice.dto.PostEvent;
import com.uniclubconnect.services.feedservice.dto.PostEventType;
import com.uniclubconnect.services.feedservice.service.FeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostEventListener {

    private final FeedService feedService;

    @RabbitListener(
            queues = "${feed.rabbitmq.queue.post-event-queue}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handlePostEvent(PostEvent event) {

        // 🔹 Mesaj geldiğinde logla
        log.info("RabbitMQ mesaj alındı | EventType: {} | PostId: {}",
                event.getEventType(), event.getPostId());

        try {
            if (event.getEventType() == PostEventType.POST_CREATED) {
                feedService.handlePostCreated(event);
            }
            else if (event.getEventType() == PostEventType.POST_DELETED) {
                feedService.handlePostDeleted(event);
            }

        } catch (Exception e) {
            // 🔹 Profesyonel error log
            log.error("Feed işlenirken hata oluştu | PostId: {}", event.getPostId(), e);
        }
    }
}