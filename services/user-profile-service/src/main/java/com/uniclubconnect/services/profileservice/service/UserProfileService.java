package com.uniclubconnect.services.profileservice.service;

import com.uniclubconnect.services.profileservice.dto.UpdateProfileRequest;
import com.uniclubconnect.services.profileservice.dto.UserCreatedEvent;
import com.uniclubconnect.services.profileservice.dto.UserProfileResponse;
import com.uniclubconnect.services.profileservice.entity.UserProfile;
import com.uniclubconnect.services.profileservice.repository.UserProfileRepository;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class UserProfileService {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileService.class);

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucketName}")
    private String bucketName;

    @Value("${minio.url}") // MinIO URL'sini alalım (dosya URL'si oluşturmak için)
    private String minioUrl;

    @Transactional
    public void createProfileFromEvent(UserCreatedEvent event) {
        // Bu kullanıcı için zaten bir profil oluşturulmuş mu? (Mesaj tekrarı durumunda)
        if (userProfileRepository.existsByAuthId(event.getAuthId())) {
            logger.warn("Profil zaten mevcut (authId: {}). Mesaj tekrarı göz ardı ediliyor.", event.getAuthId());
            return;
        }

        // Gelen event DTO'sunu, veritabanı Entity'sine dönüştür
        UserProfile newProfile = UserProfile.builder()
                .authId(event.getAuthId())
                .email(event.getEmail())
                .firstName(event.getFirstName())
                .lastName(event.getLastName())
                .totalPoints(0L) // Başlangıç puanı
                .build();

        userProfileRepository.save(newProfile);

        logger.info("Yeni profil başarıyla oluşturuldu. AuthId: {}", event.getAuthId());
    }


    // Kendi profilini getir (authId ile)
    public UserProfileResponse getProfileByAuthId(String authId) {
        UserProfile profile = userProfileRepository.findByAuthId(authId)
                .orElseThrow(() -> new ProfileNotFoundException("Profil bulunamadı: " + authId));
        return mapToResponseDTO(profile);
    }

    // Profil güncelle (authId ile)
    @Transactional
    public UserProfileResponse updateProfile(String authId, UpdateProfileRequest request) {
        UserProfile profile = userProfileRepository.findByAuthId(authId)
                .orElseThrow(() -> new ProfileNotFoundException("Güncellenecek profil bulunamadı: " + authId));

        // Sadece DTO'da gelen ve null olmayan alanları güncelle
        if (request.getFirstName() != null) {
            profile.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            profile.setLastName(request.getLastName());
        }
        if (request.getDepartment() != null) {
            profile.setDepartment(request.getDepartment());
        }

        UserProfile updatedProfile = userProfileRepository.save(profile);
        logger.info("Profil güncellendi. AuthId: {}", authId);
        return mapToResponseDTO(updatedProfile);
    }

    // Profil resmi yükle (authId ve resim dosyası ile)
    @Transactional
    public UserProfileResponse uploadProfileImage(String authId, MultipartFile file) {
        UserProfile profile = userProfileRepository.findByAuthId(authId)
                .orElseThrow(() -> new ProfileNotFoundException("Resim yüklenecek profil bulunamadı: " + authId));

        // 1. MinIO'da bucket var mı kontrol et, yoksa oluştur
        createBucketIfNotExists(bucketName);

        // 2. Benzersiz bir dosya adı oluştur (örn: authId + uuid + dosya uzantısı)
        String fileExtension = getFileExtension(file.getOriginalFilename());
        String objectName = "profile-images/" + authId + "-" + UUID.randomUUID() + "." + fileExtension;

        // 3. Dosyayı MinIO'ya yükle
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
            logger.info("Dosya MinIO'ya yüklendi: {}", objectName);
        } catch (Exception e) {
            logger.error("MinIO dosya yükleme hatası: {}", e.getMessage());
            throw new RuntimeException("Dosya yüklenemedi.", e);
        }

        // 4. Dosyanın URL'sini oluştur ve veritabanına kaydet
        // MinIO URL formatı: <minio-url>/<bucket-name>/<object-name>
        String imageUrl = minioUrl + "/" + bucketName + "/" + objectName;
        profile.setProfileImageUrl(imageUrl);
        UserProfile updatedProfile = userProfileRepository.save(profile);

        logger.info("Profil resmi URL'si güncellendi. AuthId: {}", authId);
        return mapToResponseDTO(updatedProfile);
    }

    // --- Yardımcı Metotlar ---
    private UserProfileResponse mapToResponseDTO(UserProfile profile) {
        return UserProfileResponse.builder()
                .authId(profile.getAuthId())
                .email(profile.getEmail())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .department(profile.getDepartment())
                .profileImageUrl(profile.getProfileImageUrl())
                .totalPoints(profile.getTotalPoints())
                .build();
    }

    private void createBucketIfNotExists(String bucketName) {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                logger.info("MinIO bucket oluşturuldu: {}", bucketName);
            }
        } catch (Exception e) {
            throw new RuntimeException("MinIO bucket kontrol/oluşturma hatası.", e);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return ""; // Uzantı yoksa boş dön
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    // Basit bir Exception sınıfı (ayrı bir dosyaya da koyabilirsiniz)
    public static class ProfileNotFoundException extends RuntimeException {
        public ProfileNotFoundException(String message) {
            super(message);
        }
    }
}