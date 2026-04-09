package com.uniclubconnect.services.registrationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GamificationEvent {
    private String userId;
    private EventType eventType;
    private String referenceId;
    private Map<String, Object> metadata;
    private LocalDateTime timestamp;
}
