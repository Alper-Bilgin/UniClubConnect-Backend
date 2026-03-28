package com.uniclubconnect.services.feedservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostResponse {
    private String id;
    private String userId;
    private String content;
    private String imageUrl;
    private String authorName;
    private String authorProfileImage;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
