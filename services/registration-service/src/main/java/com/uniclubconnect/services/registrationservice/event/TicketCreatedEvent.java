package com.uniclubconnect.services.registrationservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class TicketCreatedEvent implements Serializable {
    private String email;
    private String userName;
    private String eventTitle;
    private String ticketCode;

    private String eventDate;
    // -------------------------

    private String location;
}