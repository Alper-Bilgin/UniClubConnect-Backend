package com.uniclubconnect.services.clubservice.service;

import com.uniclubconnect.services.clubservice.dto.ClubMemberResponse;
import com.uniclubconnect.services.clubservice.dto.ClubRequest;
import com.uniclubconnect.services.clubservice.dto.ClubResponse;
import com.uniclubconnect.services.clubservice.dto.MembershipRequestResponse;
import com.uniclubconnect.services.clubservice.entity.Club;
import com.uniclubconnect.services.clubservice.entity.ClubMember;
import com.uniclubconnect.services.clubservice.entity.ERequestStatus;
import com.uniclubconnect.services.clubservice.entity.MembershipRequest;
import com.uniclubconnect.services.clubservice.event.ClubEventPublisher;
import com.uniclubconnect.services.clubservice.event.UserJoinedClubEvent;
import com.uniclubconnect.services.clubservice.exception.AlreadyMemberException;
import com.uniclubconnect.services.clubservice.exception.ClubNotFoundException;
import com.uniclubconnect.services.clubservice.exception.RequestNotFoundException;
import com.uniclubconnect.services.clubservice.repository.ClubMemberRepository;
import com.uniclubconnect.services.clubservice.repository.ClubRepository;
import com.uniclubconnect.services.clubservice.repository.MembershipRequestRepository;
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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ClubService {

    private static final Logger logger = LoggerFactory.getLogger(ClubService.class);

    @Autowired private ClubRepository clubRepository;
    @Autowired private ClubMemberRepository clubMemberRepository;
    @Autowired private MembershipRequestRepository membershipRequestRepository;
    @Autowired private ClubEventPublisher clubEventPublisher;
    @Autowired private MinioClient minioClient;

    @Value("${minio.bucketName}")
    private String bucketName;
    @Value("${minio.url}")
    private String minioUrl;

    // --- Herkese Açık Metotlar ---

    public List<ClubResponse> getAllClubs() {
        return clubRepository.findAll().stream()
                .map(this::mapToClubResponse)
                .collect(Collectors.toList());
    }

    public ClubResponse getClubById(Long clubId) {
        return clubRepository.findById(clubId)
                .map(this::mapToClubResponse)
                .orElseThrow(() -> new ClubNotFoundException("Kulüp bulunamadı: " + clubId));
    }

    // --- Kullanıcı Metotları (USER rolü gerektirir) ---

    @Transactional
    public void requestToJoinClub(Long clubId, String userAuthId, String userEmail) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ClubNotFoundException("Kulüp bulunamadı: " + clubId));

        // 1. Zaten üye mi?
        if (clubMemberRepository.existsByClubIdAndUserAuthId(clubId, userAuthId)) {
            throw new AlreadyMemberException("Bu kulübe zaten üyesiniz.");
        }

        // 2. Bekleyen bir isteği var mı?
        if (membershipRequestRepository.existsByClubIdAndUserAuthIdAndStatus(clubId, userAuthId, ERequestStatus.PENDING)) {
            throw new IllegalStateException("Bu kulüp için zaten beklemede olan bir isteğiniz var.");
        }

        MembershipRequest newRequest = MembershipRequest.builder()
                .club(club)
                .userAuthId(userAuthId)
                .userEmail(userEmail) // Adminin görmesi için
                .status(ERequestStatus.PENDING)
                .build();

        membershipRequestRepository.save(newRequest);
        logger.info("Yeni üyelik isteği alındı: {} -> {}", userEmail, club.getName());
    }

    // --- Kulüp Sahibi Metotları (CLUB_OWNER rolü gerektirir) ---

    @Transactional
    public ClubResponse createClub(ClubRequest request, String ownerAuthId) {
        // Kulüp adı benzersiz mi?
        clubRepository.findByName(request.getName()).ifPresent(c -> {
            throw new IllegalStateException("Bu kulüp adı zaten alınmış.");
        });

        Club newClub = Club.builder()
                .name(request.getName())
                .description(request.getDescription())
                .ownerAuthId(ownerAuthId) // Kulübü oluşturan kişi sahibi olur
                .build();

        Club savedClub = clubRepository.save(newClub);

        // Kulüp sahibi otomatik olarak kendi kulübüne üye olmalıdır
        ClubMember ownerAsMember = ClubMember.builder()
                .club(savedClub)
                .userAuthId(ownerAuthId)
                .build();
        clubMemberRepository.save(ownerAsMember);

        logger.info("Yeni kulüp oluşturuldu: {} (Sahip: {})", savedClub.getName(), ownerAuthId);
        return mapToClubResponse(savedClub);
    }

    @Transactional
    public void approveJoinRequest(Long requestId) {
        MembershipRequest request = membershipRequestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException("İstek bulunamadı: " + requestId));

        if (request.getStatus() != ERequestStatus.PENDING) {
            throw new IllegalStateException("İstek zaten sonuçlandırılmış.");
        }

        // 1. İsteği onayla
        request.setStatus(ERequestStatus.APPROVED);
        membershipRequestRepository.save(request);

        // 2. Kullanıcıyı üye olarak ekle
        ClubMember newMember = ClubMember.builder()
                .club(request.getClub())
                .userAuthId(request.getUserAuthId())
                .build();
        clubMemberRepository.save(newMember);

        // 3. RabbitMQ'ya olay yayınla (Gamification için)
        clubEventPublisher.publishUserJoinedClub(
                new UserJoinedClubEvent(
                        request.getUserAuthId(),
                        request.getClub().getId(),
                        request.getClub().getName()
                )
        );
        logger.info("Üyelik isteği onaylandı: {} -> {}", request.getUserEmail(), request.getClub().getName());
    }

    @Transactional
    public void rejectJoinRequest(Long requestId) {
        MembershipRequest request = membershipRequestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException("İstek bulunamadı: " + requestId));

        if (request.getStatus() != ERequestStatus.PENDING) {
            throw new IllegalStateException("İstek zaten sonuçlandırılmış.");
        }

        request.setStatus(ERequestStatus.REJECTED);
        membershipRequestRepository.save(request);
        logger.info("Üyelik isteği reddedildi: {}", request.getUserEmail());
    }

    // Logo yükleme
    @Transactional
    public ClubResponse uploadClubLogo(Long clubId, MultipartFile file) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ClubNotFoundException("Kulüp bulunamadı: " + clubId));

        createBucketIfNotExists(bucketName);

        String objectName = "logos/" + clubId + "-" + UUID.randomUUID() + "." + getFileExtension(file.getOriginalFilename());

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
            logger.info("Kulüp logosu MinIO'ya yüklendi: {}", objectName);
        } catch (Exception e) {
            throw new RuntimeException("MinIO dosya yükleme hatası.", e);
        }

        String logoUrl = minioUrl + "/" + bucketName + "/" + objectName;
        club.setLogoUrl(logoUrl);
        Club updatedClub = clubRepository.save(club);

        return mapToClubResponse(updatedClub);
    }


    // --- Yardımcı Metotlar ---
    private ClubResponse mapToClubResponse(Club club) {
        return ClubResponse.builder()
                .id(club.getId())
                .name(club.getName())
                .description(club.getDescription())
                .logoUrl(club.getLogoUrl())
                .ownerAuthId(club.getOwnerAuthId())
                .build();
    }

    // Bu metotları user-profile-service'ten kopyalayın
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

    /**
     * Kulüp bilgilerini (ad, açıklama) günceller.
     * Bu metot, logoyu VEYA sahibi GÜNCELLEMEZ.
     */
    @Transactional
    public ClubResponse updateClubInfo(Long clubId, ClubRequest request) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ClubNotFoundException("Kulüp bulunamadı: " + clubId));

        // Kulüp adı benzersiz olmalı (eğer değiştiriliyorsa)
        if (request.getName() != null && !request.getName().equals(club.getName())) {
            clubRepository.findByName(request.getName()).ifPresent(c -> {
                throw new IllegalStateException("Bu kulüp adı zaten alınmış.");
            });
            club.setName(request.getName());
        }

        if (request.getDescription() != null) {
            club.setDescription(request.getDescription());
        }

        Club updatedClub = clubRepository.save(club);
        logger.info("Kulüp bilgileri güncellendi: {}", updatedClub.getName());
        return mapToClubResponse(updatedClub);
    }

    /**
     * Bir kulübün üyelik isteklerini durumuna göre (örn: PENDING) listeler.
     */
    public List<MembershipRequestResponse> getMembershipRequests(Long clubId, ERequestStatus status) {
        // Kulübün var olup olmadığını kontrol et (isteğe bağlı ama iyi pratik)
        if (!clubRepository.existsById(clubId)) {
            throw new ClubNotFoundException("Kulüp bulunamadı: " + clubId);
        }

        return membershipRequestRepository.findByClubIdAndStatus(clubId, status).stream()
                .map(this::mapToMembershipRequestResponse) // DTO'ya çevir
                .collect(Collectors.toList());
    }

    /**
     * Bir kulübün mevcut üyelerini listeler.
     */
    public List<ClubMemberResponse> getClubMembers(Long clubId) {
        if (!clubRepository.existsById(clubId)) {
            throw new ClubNotFoundException("Kulüp bulunamadı: " + clubId);
        }

        return clubMemberRepository.findByClubId(clubId).stream()
                .map(this::mapToClubMemberResponse) // DTO'ya çevir
                .collect(Collectors.toList());
    }


    // --- YARDIMCI DTO DÖNÜŞTÜRME METOTLARI ---
    // (Bunları da sınıfın en altına ekleyin)

    private MembershipRequestResponse mapToMembershipRequestResponse(MembershipRequest request) {
        return MembershipRequestResponse.builder()
                .id(request.getId())
                .clubId(request.getClub().getId())
                .userAuthId(request.getUserAuthId())
                .userEmail(request.getUserEmail())
                .status(request.getStatus())
                .requestDate(request.getRequestDate())
                .build();
    }

    private ClubMemberResponse mapToClubMemberResponse(ClubMember member) {
        return ClubMemberResponse.builder()
                .id(member.getId())
                .userAuthId(member.getUserAuthId())
                .joinDate(member.getJoinDate())
                // TODO: İleride burada user-profile-service'i Feign ile çağırıp
                // üyenin adını/soyadını/profil resmini de bu DTO'ya ekleyebiliriz.
                .build();
    }
}