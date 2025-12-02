package com.uniclubconnect.services.registrationservice.service;

import com.uniclubconnect.services.registrationservice.client.EventServiceClient;
import com.uniclubconnect.services.registrationservice.dto.EventDto;
import com.uniclubconnect.services.registrationservice.dto.TicketValidationResponse;
import com.uniclubconnect.services.registrationservice.entity.ERegistrationStatus;
import com.uniclubconnect.services.registrationservice.entity.Registration;
import com.uniclubconnect.services.registrationservice.event.TicketCreatedEvent;
import com.uniclubconnect.services.registrationservice.repository.RegistrationRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class RegistrationService {

    @Autowired private RegistrationRepository registrationRepository;
    @Autowired private EventServiceClient eventServiceClient;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private RabbitTemplate rabbitTemplate;

    @Value("${spring.rabbitmq.exchange.name}")
    private String exchangeName;

    // DİKKAT: Burası da 'spring.' ile başlamalı
    @Value("${spring.rabbitmq.routingkey.ticket_created}")
    private String ticketCreatedKey;

    // 1. KAYIT OLMA (Stok Düşürmeli)
    @Transactional
    public Registration registerForEvent(Long eventId, String userAuthId, String userEmail) {
        // A) Etkinlik Var mı?
        EventDto event = eventServiceClient.getEventById(eventId); // Hata varsa Feign fırlatır

        // B) Zaten kayıtlı mı?
        if (registrationRepository.existsByEventIdAndUserAuthIdAndStatus(eventId, userAuthId, ERegistrationStatus.CONFIRMED)) {
            throw new IllegalStateException("Bu etkinliğe zaten kaydınız var.");
        }

        // C) REDIS İLE STOK KONTROLÜ (Race Condition Koruması)
        if (event.getTotalQuota() != null) { // Sınırlı kontenjan ise
            String redisKey = "event:" + eventId + ":quota";

            // Atomik olarak azalt
            Long remainingStock = redisTemplate.opsForValue().decrement(redisKey);

            if (remainingStock != null && remainingStock < 0) {
                // Stok bitti, işlemi geri al (artır) ve hata fırlat
                redisTemplate.opsForValue().increment(redisKey);
                throw new IllegalStateException("Etkinlik kontenjanı doldu.");
            }
        }

        // D) Kaydı Oluştur
        Registration registration = Registration.builder()
                .eventId(eventId)
                .eventTitle(event.getTitle())
                .eventDate(event.getEventDateTime())
                .eventLocation(event.getLocation())
                .userAuthId(userAuthId)
                .userEmail(userEmail)
                .userName("Kullanıcı") // TODO: user-profile-service'ten isim çekilebilir
                .ticketCode(UUID.randomUUID().toString()) // Benzersiz Bilet Kodu
                .status(ERegistrationStatus.CONFIRMED)
                .build();

        Registration savedRegistration = registrationRepository.save(registration);

        // E) RabbitMQ Bildirimi
        TicketCreatedEvent eventMessage = new TicketCreatedEvent(
                userEmail, "Kullanıcı", event.getTitle(),
                savedRegistration.getTicketCode(), event.getEventDateTime(), event.getLocation());

        try {
            rabbitTemplate.convertAndSend(exchangeName, ticketCreatedKey, eventMessage);
        } catch (Exception e) {
            // Logla ama işlemi durdurma
            System.err.println("RabbitMQ hatası: " + e.getMessage());
        }

        return savedRegistration;
    }

    // 2. BİLET DOĞRULAMA (Laptop/QR Kontrolü)
    public TicketValidationResponse validateTicket(String ticketCode, String requesterId) {
        Registration registration = registrationRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new RuntimeException("Bilet bulunamadı: " + ticketCode));

        // Güvenlik: Sadece etkinliğin sahibi kontrol edebilir
        EventDto event = eventServiceClient.getEventById(registration.getEventId());
        if (!event.getOrganizerAuthId().equals(requesterId)) {
            throw new RuntimeException("Bu bileti kontrol etme yetkiniz yok. (Etkinlik Sahibi Değilsiniz)");
        }

        if (registration.getStatus() == ERegistrationStatus.CANCELLED) {
            return TicketValidationResponse.builder()
                    .valid(false)
                    .message("Bu bilet İPTAL edilmiş.")
                    .build();
        }

        return TicketValidationResponse.builder()
                .valid(true)
                .message("Giriş Başarılı ✅")
                .userName(registration.getUserName())
                .eventTitle(registration.getEventTitle())
                .ticketCode(registration.getTicketCode())
                .build();
    }

    // 3. KULLANICININ BİLETLERİNİ LİSTELE
    public List<Registration> getMyRegistrations(String userAuthId) {
        return registrationRepository.findByUserAuthId(userAuthId);
    }

    // 4. BİLET İPTAL ET (Kullanıcı Kendi Biletini)
    @Transactional
    public void cancelTicket(String ticketCode, String userAuthId) {
        Registration registration = registrationRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new RuntimeException("Bilet bulunamadı."));

        if (!registration.getUserAuthId().equals(userAuthId)) {
            throw new RuntimeException("Bu bileti iptal etme yetkiniz yok.");
        }

        if (registration.getStatus() == ERegistrationStatus.CANCELLED) {
            throw new RuntimeException("Bilet zaten iptal edilmiş.");
        }

        // Durumu güncelle
        registration.setStatus(ERegistrationStatus.CANCELLED);
        registrationRepository.save(registration);

        // Redis'teki stoğu geri artır (Sınırlıysa)
        // Event bilgisini tekrar çekmemiz gerekebilir veya EventService'e sormadan
        // güvenli bir şekilde artırma stratejisi (Redis anahtarı varsa artır)
        String redisKey = "event:" + registration.getEventId() + ":quota";
        if (redisTemplate.hasKey(redisKey)) {
            redisTemplate.opsForValue().increment(redisKey);
        }
    }

    // 5. ETKİNLİK KATILIMCILARINI LİSTELE (Kulüp Sahibi İçin)
    public List<Registration> getEventRegistrations(Long eventId, String ownerId) {
        // Önce yetki kontrolü (Event Service üzerinden)
        EventDto event = eventServiceClient.getEventById(eventId);

        if (!event.getOrganizerAuthId().equals(ownerId)) {
            throw new RuntimeException("Bu etkinliğin katılımcılarını görme yetkiniz yok.");
        }

        return registrationRepository.findByEventId(eventId);
    }
}