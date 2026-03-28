package com.uniclubconnect.services.feedservice.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PostEvent implements Serializable {
    private String eventId;
    private String postId;
    private String authorId;
    private PostEventType eventType; // Enum eklendi
    private LocalDateTime timestamp;
}
