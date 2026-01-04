package com.uniclubconnect.services.eventservice.service;

import com.uniclubconnect.services.eventservice.client.ClubServiceClient;
import com.uniclubconnect.services.eventservice.dto.ClubDto;
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
    private final MinioService minioService;

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

        String fileName = minioService.uploadFile(file);
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

    // --- 4. ETKİNLİK GÜNCELLEME ---
    @Transactional
    public EventResponse updateEvent(Long eventId, EventRequest request, String userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Etkinlik bulunamadı: " + eventId));

        if (!event.getOrganizerAuthId().equals(userId)) {
            throw new AccessDeniedException("Bu etkinliği düzenleme yetkiniz yok.");
        }

        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setLocation(request.getLocation());
        event.setEventLink(request.getEventLink());
        event.setEventDateTime(request.getEventDateTime());

        // Kontenjan değiştiyse Redis'i güncelle
        if (request.getTotalQuota() != null && !request.getTotalQuota().equals(event.getTotalQuota())) {
            event.setTotalQuota(request.getTotalQuota());
            String redisKey = "event:" + event.getId() + ":quota";
            redisTemplate.opsForValue().set(redisKey, String.valueOf(request.getTotalQuota()));
        }

        Event updatedEvent = eventRepository.save(event);
        logger.info("Etkinlik güncellendi: {}", eventId);
        return mapToEventResponse(updatedEvent);
    }

    // --- 5. ETKİNLİK SİLME ---
    @Transactional
    public void deleteEvent(Long eventId, String userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Etkinlik bulunamadı: " + eventId));

        if (!event.getOrganizerAuthId().equals(userId)) {
            throw new AccessDeniedException("Bu etkinliği silme yetkiniz yok.");
        }

        redisTemplate.delete("event:" + eventId + ":quota");
        eventRepository.delete(event);
        logger.info("Etkinlik silindi: {}", eventId);
    }

    // --- 6. KULÜBE GÖRE ETKİNLİKLERİ GETİR ---
    public List<EventResponse> getEventsByClubId(Long clubId) {
        return eventRepository.findByClubId(clubId).stream()
                .map(this::mapToEventResponse)
                .collect(Collectors.toList());
    }

    // --- MAPPER (TEK VE SON SÜRÜM) ---
    private EventResponse mapToEventResponse(Event event) {
        String fullImageUrl = null;
        if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
            fullImageUrl = minioService.getFileUrl(event.getImageUrl());
        }

        // --- YENİ KISIM: Feign Client ile Kulüp İsmini Çekme ---
        String fetchedClubName = "Bilinmeyen Kulüp";
        try {
            ClubDto clubDto = clubClient.getClubById(event.getClubId());
            if (clubDto != null) {
                fetchedClubName = clubDto.getName();
            }
        } catch (Exception e) {
            logger.warn("Club servisine ulaşılamadı. EventId: {}, Hata: {}", event.getId(), e.getMessage());
        }
        // ---------------------------

        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .location(event.getLocation())
                .eventLink(event.getEventLink())
                .eventDateTime(event.getEventDateTime())
                .imageUrl(fullImageUrl)
                .totalQuota(event.getTotalQuota())
                .clubId(event.getClubId())
                .clubName(fetchedClubName) // <-- Kulüp ismini ekliyoruz
                .organizerAuthId(event.getOrganizerAuthId())
                .build();
    }
}