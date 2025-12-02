package com.uniclubconnect.services.registrationservice.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EventDto {
    private Long id;
    private String title;
    private LocalDateTime eventDateTime;
    private String location;
    private Integer totalQuota; // Null olabilir
    private String organizerAuthId; // Kulüp sahibi kim? (Yetki kontrolü için)
}