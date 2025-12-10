package com.uniclubconnect.services.notificationservice.listener;

import com.uniclubconnect.services.notificationservice.dto.TicketCreatedEvent;
import com.uniclubconnect.services.notificationservice.dto.UserCreatedEvent;
import com.uniclubconnect.services.notificationservice.service.EmailService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class NotificationListener {

    @Autowired
    private EmailService emailService;

    // 1. Hoşgeldin Maili Dinleyicisi
    @RabbitListener(queues = "${notification.rabbitmq.queue.welcome-email}")
    public void handleUserCreated(UserCreatedEvent event) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", event.getFirstName());
        variables.put("code", event.getVerificationCode());

        emailService.sendHtmlEmail(
                event.getEmail(),
                "UniClub'a Hoş Geldin!",
                "welcome-template",
                variables,
                "WELCOME"
        );
    }

    // 2. Bilet Maili Dinleyicisi
    @RabbitListener(queues = "${notification.rabbitmq.queue.ticket-email}")
    public void handleTicketCreated(TicketCreatedEvent event) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", event.getUserName());
        variables.put("eventName", event.getEventTitle());
        variables.put("ticketCode", event.getTicketCode());
        variables.put("location", event.getLocation());
        variables.put("date", event.getEventDate().toString());

        // Basit bir QR API'si kullanıyoruz
        String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=" + event.getTicketCode();
        variables.put("qrCodeUrl", qrUrl);

        emailService.sendHtmlEmail(
                event.getEmail(),
                "Etkinlik Biletiniz: " + event.getEventTitle(),
                "ticket-template",
                variables,
                "TICKET"
        );
    }
}