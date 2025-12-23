package com.uniclubconnect.services.eventservice.service;

import com.uniclubconnect.services.eventservice.client.ClubServiceClient;
import com.uniclubconnect.services.eventservice.dto.EventRequest;
import com.uniclubconnect.services.eventservice.dto.EventResponse;
import com.uniclubconnect.services.eventservice.entity.Event;
import com.uniclubconnect.services.eventservice.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // Autowired yerine Constructor Injection (Best Practice)
public class EventService {

    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    private final EventRepository eventRepository;
    private final ClubServiceClient clubClient;
    private final StringRedisTemplate redisTemplate;
    private final MinioService minioService; // Artık kendi servisimizi kullanıyoruz

    // --- 1. ETKİNLİK OLUŞTURMA ---
    @Transactional
    public EventResponse createEvent(EventRequest request, String organizerId) {
        // A) Güvenlik: Kullanıcı kulüp sahibi mi?
        boolean isOwner = clubClient.isUserOwnerOfClub(request.getClubId(), organizerId);
        if (!isOwner) {
            throw new AccessDeniedException("Bu kulüp adına etkinlik oluşturma yetkiniz yok.");
        }

        // B) Kaydetme
        Event newEvent = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .eventLink(request.getEventLink())
                .eventDateTime(request.getEventDateTime())
                .totalQuota(request.getTotalQuota())
                .clubId(request.getClubId())
                .organizerAuthId(organizerId)
                .build();

        Event savedEvent = eventRepository.save(newEvent);

        // C) Redis Kontenjan
        if (request.getTotalQuota() != null) {
            String redisKey = "event:" + savedEvent.getId() + ":quota";
            redisTemplate.opsForValue().set(redisKey, String.valueOf(request.getTotalQuota()));
            logger.info("Redis kontenjanı ayarlandı. Key: {}, Değer: {}", redisKey, request.getTotalQuota());
        }

        return mapToEventResponse(savedEvent);
    }

    // --- 2. RESİM YÜKLEME ---
    @Transactional
    public EventResponse uploadEventImage(Long eventId, MultipartFile file, String userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Etkinlik bulunamadı: " + eventId));

        if (!event.getOrganizerAuthId().equals(userId)) {
            throw new AccessDeniedException("Bu etkinliğe resim yükleme yetkiniz yok.");
        }

        // MinioService içindeki metodu kullanarak yükle
        // Bu metod sadece dosya ismini döner (örn: 17665..._resim.png)
        String fileName = minioService.uploadFile(file);

        // DÜZELTME: Veritabanına sadece dosya adını kaydediyoruz.
        event.setImageUrl(fileName);

        Event updatedEvent = eventRepository.save(event);

        return mapToEventResponse(updatedEvent);
    }

    // --- 3. LİSTELEME ---
    public List<EventResponse> getAllEvents() {
        return eventRepository.findAll().stream()
                .map(this::mapToEventResponse)
                .collect(Collectors.toList());
    }

    public EventResponse getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Etkinlik bulunamadı: " + id));
        return mapToEventResponse(event);
    }

    // --- MAPPER ---
    private EventResponse mapToEventResponse(Event event) {
        // Frontend için tam URL oluşturuyoruz
        String fullImageUrl = null;
        if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
            fullImageUrl = minioService.getFileUrl(event.getImageUrl());
        }

        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .location(event.getLocation())
                .eventLink(event.getEventLink())
                .eventDateTime(event.getEventDateTime())
                .imageUrl(fullImageUrl) // Tam URL (http://localhost:9000/uniclub-events/...)
                .totalQuota(event.getTotalQuota())
                .clubId(event.getClubId())
                .organizerAuthId(event.getOrganizerAuthId())
                .build();
    }
}