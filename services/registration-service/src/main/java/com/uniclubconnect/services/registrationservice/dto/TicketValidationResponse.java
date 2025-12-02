package com.uniclubconnect.services.registrationservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TicketValidationResponse {
    private boolean valid;
    private String message; // "Giriş Başarılı" veya "Bilet İptal Edilmiş"
    private String userName;
    private String eventTitle;
    private String ticketCode;
}