package com.uniclubconnect.services.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TicketCreatedEvent implements Serializable {
    private String email;
    private String userName;
    private String eventTitle;
    private String ticketCode;
    private LocalDateTime eventDate;
    private String location;
}