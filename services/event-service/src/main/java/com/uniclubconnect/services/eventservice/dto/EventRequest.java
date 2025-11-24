package com.uniclubconnect.services.eventservice.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EventRequest {
    @NotBlank
    private String title;
    private String description;
    @NotBlank
    private String location;

    private String eventLink; // Opsiyonel

    @NotNull
    @Future // Geçmişe etkinlik açılamaz
    private LocalDateTime eventDateTime;

    private Integer totalQuota; // Opsiyonel (Null = Sınırsız)

    @NotNull
    private Long clubId; // Hangi kulüp adına?
}