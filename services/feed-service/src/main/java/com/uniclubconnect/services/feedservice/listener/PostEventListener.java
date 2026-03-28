package com.uniclubconnect.services.feedservice.listener;

import com.uniclubconnect.services.feedservice.dto.PostEvent;
import com.uniclubconnect.services.feedservice.dto.PostEventType;
import com.uniclubconnect.services.feedservice.service.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostEventListener {

    private final FeedService feedService;

    @RabbitListener(queues = "${feed.rabbitmq.queue.post-event-queue}")
    public void handlePostEvent(PostEvent event) {
        try {
            if (event.getEventType() == PostEventType.POST_CREATED) {
                feedService.handlePostCreated(event);
            } else if (event.getEventType() == PostEventType.POST_DELETED) {
                feedService.handlePostDeleted(event);
            }
        } catch (Exception e) {
            System.err.println("Feed işlenirken hata: " + e.getMessage());
        }
    }
}
