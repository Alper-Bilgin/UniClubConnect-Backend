package com.uniclubconnect.services.profileservice.service;

import com.uniclubconnect.services.profileservice.dto.UpdateProfileRequest;
import com.uniclubconnect.services.profileservice.dto.UserCreatedEvent;
import com.uniclubconnect.services.profileservice.dto.UserProfileResponse;
import com.uniclubconnect.services.profileservice.entity.UserProfile;
import com.uniclubconnect.services.profileservice.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor // Autowired yerine Constructor Injection
public class UserProfileService {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileService.class);

    private final UserProfileRepository userProfileRepository;
    private final MinioService minioService; // <-- Yeni MinioServisimizi kullanıyoruz

    // RabbitMQ'dan gelen mesajla profil oluşturma
    @Transactional
    public void createProfileFromEvent(UserCreatedEvent event) {
        if (userProfileRepository.existsByAuthId(event.getAuthId())) {
            logger.warn("Profil zaten mevcut (authId: {}). Mesaj tekrarı göz ardı ediliyor.", event.getAuthId());
            return;
        }

        UserProfile newProfile = UserProfile.builder()
                .authId(event.getAuthId())
                .email(event.getEmail())
                .firstName(event.getFirstName())
                .lastName(event.getLastName())
                .totalPoints(0L)
                .build();

        userProfileRepository.save(newProfile);
        logger.info("Yeni profil başarıyla oluşturuldu. AuthId: {}", event.getAuthId());
    }

    // Kendi profilini getir
    public UserProfileResponse getProfileByAuthId(String authId) {
        UserProfile profile = userProfileRepository.findByAuthId(authId)
                .orElseThrow(() -> new ProfileNotFoundException("Profil bulunamadı: " + authId));
        return mapToResponseDTO(profile);
    }

    // Profil güncelle
    @Transactional
    public UserProfileResponse updateProfile(String authId, UpdateProfileRequest request) {
        UserProfile profile = userProfileRepository.findByAuthId(authId)
                .orElseThrow(() -> new ProfileNotFoundException("Güncellenecek profil bulunamadı: " + authId));

        if (request.getFirstName() != null) profile.setFirstName(request.getFirstName());
        if (request.getLastName() != null) profile.setLastName(request.getLastName());
        if (request.getDepartment() != null) profile.setDepartment(request.getDepartment());

        UserProfile updatedProfile = userProfileRepository.save(profile);
        logger.info("Profil güncellendi. AuthId: {}", authId);
        return mapToResponseDTO(updatedProfile);
    }

    // 🔥 PROFİL RESMİ YÜKLEME (GÜNCELLENDİ) 🔥
    @Transactional
    public UserProfileResponse uploadProfileImage(String authId, MultipartFile file) {
        UserProfile profile = userProfileRepository.findByAuthId(authId)
                .orElseThrow(() -> new ProfileNotFoundException("Resim yüklenecek profil bulunamadı: " + authId));

        // MinioService ile dosyayı yükle (Otomatik Public, kod tekrarı yok)
        String fileName = minioService.uploadFile(file);

        // Veritabanına sadece dosya adını kaydet, URL'i değil!
        profile.setProfileImageUrl(fileName);

        UserProfile updatedProfile = userProfileRepository.save(profile);
        logger.info("Profil resmi güncellendi. AuthId: {}", authId);

        return mapToResponseDTO(updatedProfile);
    }

    // 🔥 MAPPER (URL DÖNÜŞÜMÜ BURADA) 🔥
    private UserProfileResponse mapToResponseDTO(UserProfile profile) {
        String fullImageUrl = null;

        // Veritabanından gelen sadece dosya adı ise, tam URL'e çeviriyoruz
        if (profile.getProfileImageUrl() != null && !profile.getProfileImageUrl().isEmpty()) {
            // Eğer veritabanında eski tam URL kaldıysa çift eklemeyi önle
            if (profile.getProfileImageUrl().startsWith("http")) {
                fullImageUrl = profile.getProfileImageUrl();
            } else {
                fullImageUrl = minioService.getFileUrl(profile.getProfileImageUrl());
            }
        }

        return UserProfileResponse.builder()
                .authId(profile.getAuthId())
                .email(profile.getEmail())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .department(profile.getDepartment())
                .profileImageUrl(fullImageUrl) // http://localhost:9000/uniclub-profiles/resim.png
                .totalPoints(profile.getTotalPoints())
                .build();
    }

    // Exception Sınıfı
    public static class ProfileNotFoundException extends RuntimeException {
        public ProfileNotFoundException(String message) {
            super(message);
        }
    }

    // 🔥 YENİ: Sistemdeki tüm kullanıcı profillerini getirir 🔥
    public java.util.List<UserProfileResponse> getAllProfiles() {
        java.util.List<UserProfile> allProfiles = userProfileRepository.findAll();

        // Veritabanından gelen tüm entity'leri, mapToResponseDTO yardımıyla DTO'ya çevir (MinIO URL'leri dahil)
        return allProfiles.stream()
                .map(this::mapToResponseDTO)
                .collect(java.util.stream.Collectors.toList());
    }
}