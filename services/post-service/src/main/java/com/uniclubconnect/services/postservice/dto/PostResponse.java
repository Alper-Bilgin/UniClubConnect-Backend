package com.uniclubconnect.services.postservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class PostResponse {
    private String id;
    private String userId;
    private String content;
    private String imageUrl;

    // --- YENİ EKLENEN KISIM ---
    private String authorName; // Örn: "Alper Bilgin"
    private String authorProfileImage; // Profil resmi URL'si

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}