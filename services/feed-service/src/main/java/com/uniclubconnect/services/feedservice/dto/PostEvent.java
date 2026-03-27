package com.uniclubconnect.services.feedservice.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PostEvent implements Serializable {
    private String eventId;
    private String postId;
    private String authorId;
    private String eventType; // "POST_CREATED" veya "POST_DELETED"
    private LocalDateTime timestamp;
}
