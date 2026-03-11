package com.uniclubconnect.services.notificationservice.listener;

import com.uniclubconnect.services.notificationservice.client.ProfileServiceClient;
import com.uniclubconnect.services.notificationservice.dto.FollowEvent;
import com.uniclubconnect.services.notificationservice.dto.TicketCreatedEvent;
import com.uniclubconnect.services.notificationservice.dto.UserCreatedEvent;
import com.uniclubconnect.services.notificationservice.dto.UserProfileResponse;
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

    @Autowired
    private ProfileServiceClient profileServiceClient;

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
        variables.put("date", event.getEventDate());

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

    // 3. Takip İstekleri ve Onayları Dinleyicisi
    @RabbitListener(queues = "${notification.rabbitmq.queue.follow-email}")
    public void handleFollowEvent(FollowEvent event) {
        try {
            if ("FOLLOW_REQUESTED".equals(event.getType())) {

                // İsteği ALAN kişinin bilgilerini çek (Mail ona gidecek)
                UserProfileResponse targetUser = profileServiceClient.getUserProfile(event.getFollowingId());
                // İsteği ATAN kişinin adını çek
                UserProfileResponse actorUser = profileServiceClient.getUserProfile(event.getFollowerId());

                if (targetUser.getEmail() != null) {
                    Map<String, Object> variables = new HashMap<>();
                    variables.put("targetName", targetUser.getFirstName() + " " + targetUser.getLastName());
                    variables.put("actorName", actorUser.getFirstName() + " " + actorUser.getLastName());

                    emailService.sendHtmlEmail(
                            targetUser.getEmail(),
                            "Yeni Bir Takip İsteğin Var!",
                            "follow-request-template", // Thymeleaf şablon adı
                            variables,
                            "FOLLOW_REQUEST"
                    );
                }

            } else if ("FOLLOW_ACCEPTED".equals(event.getType())) {

                // İsteği ATAN kişinin bilgilerini çek (Mail ona gidecek, kabul edildiğini öğrenecek)
                UserProfileResponse followerUser = profileServiceClient.getUserProfile(event.getFollowerId());
                // Onaylayan kişinin adını çek
                UserProfileResponse acceptedUser = profileServiceClient.getUserProfile(event.getFollowingId());

                if (followerUser.getEmail() != null) {
                    Map<String, Object> variables = new HashMap<>();
                    variables.put("followerName", followerUser.getFirstName() + " " + followerUser.getLastName());
                    variables.put("acceptedName", acceptedUser.getFirstName() + " " + acceptedUser.getLastName());

                    emailService.sendHtmlEmail(
                            followerUser.getEmail(),
                            "Takip İsteğin Kabul Edildi!",
                            "follow-accepted-template",
                            variables,
                            "FOLLOW_ACCEPTED"
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("Follow event işlenirken hata oluştu: " + e.getMessage());
        }
    }
}