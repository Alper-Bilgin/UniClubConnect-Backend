package com.uniclubconnect.services.postservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostEvent implements Serializable {
    private String eventId; // UUID
    private String postId;
    private String authorId; // Postu atanın ID'si
    private String eventType; // "POST_CREATED" veya "POST_DELETED"
    private LocalDateTime timestamp;
}
