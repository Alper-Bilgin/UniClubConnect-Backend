package com.uniclubconnect.services.interactionservice.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CommentResponse {
    private Long id;
    private String userId;
    private String content;
    private String targetId;
    private String targetType;

    // Yorum yapanın bilgileri
    private String authorName;
    private String authorProfileImage;

    private LocalDateTime createdAt;
}
