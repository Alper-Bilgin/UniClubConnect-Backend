package com.uniclubconnect.services.gamificationservice.dto;

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
    private String userId;           // Olayı gerçekleştiren kişi
    private EventType eventType;     // Olayın tipi (Ne yaptı?)
    private String referenceId;      // Hangi post? Hangi event? (Opsiyonel)
    private Map<String, Object> metadata; // Ekstra veriler (Örn: Kulüp ID'si)
    private LocalDateTime timestamp;
}
