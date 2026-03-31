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

        //  Null & invalid event guard
        if (event == null || event.getEventType() == null) {
            log.warn("invalid_event_received event={}", event);
            return;
        }

        //  Structured logging (production-friendly)
        log.info("event_received type={} postId={} authorId={}",
                event.getEventType(),
                event.getPostId(),
                event.getAuthorId());

        try {
            if (event.getEventType() == PostEventType.POST_CREATED) {

                feedService.handlePostCreated(event);

            } else if (event.getEventType() == PostEventType.POST_DELETED) {

                feedService.handlePostDeleted(event);

            } else {
                //  Unknown event tipi (silent fail önlenir)
                log.warn("unknown_event_type type={} eventId={}",
                        event.getEventType(),
                        event.getEventId());
            }

        } catch (Exception e) {
            //  Full context error log
            log.error("feed_processing_error eventId={} postId={} authorId={}",
                    event.getEventId(),
                    event.getPostId(),
                    event.getAuthorId(),
                    e);

            //  Retry / DLQ mekanizması için exception tekrar fırlatılır
            throw e;
        }
    }
}