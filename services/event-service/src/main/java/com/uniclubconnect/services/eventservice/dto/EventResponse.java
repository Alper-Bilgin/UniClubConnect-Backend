package com.uniclubconnect.services.eventservice.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class EventResponse {
    private Long id;
    private String title;
    private String description;
    private String location;
    private String eventLink;       // Online linki
    private LocalDateTime eventDateTime;
    private String imageUrl;        // MinIO'dan gelen URL
    private Integer totalQuota;     // Null ise sınırsız
    private Long clubId;
    private String clubName;
    private String organizerAuthId;
}