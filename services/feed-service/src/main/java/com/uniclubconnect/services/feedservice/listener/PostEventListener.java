package com.uniclubconnect.services.feedservice.listener;

import com.uniclubconnect.services.feedservice.dto.PostEvent;
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
            if ("POST_CREATED".equals(event.getEventType())) {
                feedService.pushPostToFeeds(event);
            } else if ("POST_DELETED".equals(event.getEventType())) {
                feedService.removePostFromFeeds(event);
            }
        } catch (Exception e) {
            System.err.println("Feed güncellenirken hata: " + e.getMessage());
        }
    }
}
