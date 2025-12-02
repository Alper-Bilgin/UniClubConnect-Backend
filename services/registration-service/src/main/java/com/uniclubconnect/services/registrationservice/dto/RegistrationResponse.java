package com.uniclubconnect.services.registrationservice.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class RegistrationResponse {
    private String ticketCode;
    private String eventName;
    private LocalDateTime eventDate;
    private String eventLocation;
    private String userName;
    private String userEmail;
    private LocalDateTime registrationDate;
    private String status; // CONFIRMED / CANCELLED
}