package com.uniclubconnect.services.eventservice.service;

import com.uniclubconnect.services.eventservice.client.ClubServiceClient;
import com.uniclubconnect.services.eventservice.dto.EventRequest;
import com.uniclubconnect.services.eventservice.dto.EventResponse;
import com.uniclubconnect.services.eventservice.entity.Event;
import com.uniclubconnect.services.eventservice.repository.EventRepository;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EventService {

    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ClubServiceClient clubClient; // Feign Client (Club Service ile konuşur)

    @Autowired
    private StringRedisTemplate redisTemplate; // Redis Client

    @Autowired
    private MinioClient minioClient; // MinIO Client

    @Value("${minio.bucketName}")
    private String bucketName;

    @Value("${minio.url}")
    private String minioUrl;

    // --- 1. ETKİNLİK OLUŞTURMA ---
    @Transactional
    public EventResponse createEvent(EventRequest request, String organizerId) {
        // A) Güvenlik Kontrolü: Kullanıcı bu kulübün sahibi mi? (Club Service'e sor)
        boolean isOwner = clubClient.isUserOwnerOfClub(request.getClubId(), organizerId);
        if (!isOwner) {
            throw new AccessDeniedException("Bu kulüp adına etkinlik oluşturma yetkiniz yok.");
        }

        // B) Entity Oluşturma ve Kaydetme (PostgreSQL)
        Event newEvent = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .eventLink(request.getEventLink())
                .eventDateTime(request.getEventDateTime())
                .totalQuota(request.getTotalQuota()) // Null gelebilir (Sınırsız)
                .clubId(request.getClubId())
                .organizerAuthId(organizerId)
                .build();

        Event savedEvent = eventRepository.save(newEvent);

        // C) Kontenjanı Redis'e Yazma (Eğer sınırlıysa)
        if (request.getTotalQuota() != null) {
            // Key: "event:101:quota", Value: "50"
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

        // Sadece oluşturan kişi resim yükleyebilir
        if (!event.getOrganizerAuthId().equals(userId)) {
            throw new AccessDeniedException("Bu etkinliğe resim yükleme yetkiniz yok.");
        }

        // MinIO Bucket Kontrolü
        createBucketIfNotExists(bucketName);

        // Dosya Adı Oluşturma (benzersiz olması için UUID ekle)
        String fileExtension = getFileExtension(file.getOriginalFilename());
        String objectName = "events/" + eventId + "-" + UUID.randomUUID() + "." + fileExtension;

        try {
            // MinIO'ya Yükle
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());

            // URL Oluştur ve DB'ye Kaydet
            String imageUrl = minioUrl + "/" + bucketName + "/" + objectName;
            event.setImageUrl(imageUrl);
            Event updatedEvent = eventRepository.save(event);

            return mapToEventResponse(updatedEvent);

        } catch (Exception e) {
            throw new RuntimeException("Resim yüklenirken hata oluştu: " + e.getMessage());
        }
    }

    // --- 3. LİSTELEME METOTLARI ---
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

    // --- YARDIMCI METOTLAR ---
    private EventResponse mapToEventResponse(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .location(event.getLocation())
                .eventLink(event.getEventLink())
                .eventDateTime(event.getEventDateTime())
                .imageUrl(event.getImageUrl())
                .totalQuota(event.getTotalQuota())
                .clubId(event.getClubId())
                .organizerAuthId(event.getOrganizerAuthId())
                .build();
    }

    private void createBucketIfNotExists(String bucketName) {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("MinIO bucket hatası", e);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) return "";
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}