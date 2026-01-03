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
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.security.access.AccessDeniedException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // Autowired yerine Constructor Injection
public class ClubService {

    private static final Logger logger = LoggerFactory.getLogger(ClubService.class);

    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final MembershipRequestRepository membershipRequestRepository;
    private final ClubEventPublisher clubEventPublisher;
    private final MinioService minioService; // <-- Yeni MinioServisimizi kullanıyoruz

    // --- Herkese Açık Metotlar ---

    public List<ClubResponse> getAllClubs() {
        return clubRepository.findAll().stream()
                .map(this::mapToClubResponse)
                .collect(Collectors.toList());
    }

    public List<ClubResponse> searchClubs(String keyword) {
        return clubRepository.findByNameContainingIgnoreCase(keyword).stream()
                .map(this::mapToClubResponse)
                .collect(Collectors.toList());
    }

    public ClubResponse getClubById(Long clubId) {
        return clubRepository.findById(clubId)
                .map(this::mapToClubResponse)
                .orElseThrow(() -> new ClubNotFoundException("Kulüp bulunamadı: " + clubId));
    }

    public ClubResponse getMyClub(String ownerAuthId) {
        Club club = clubRepository.findByOwnerAuthId(ownerAuthId)
                .orElseThrow(() -> new RuntimeException("Sahip olduğunuz bir kulüp bulunamadı."));

        return mapToClubResponse(club);
    }

    // --- Kullanıcı Metotları (USER rolü gerektirir) ---

    @Transactional
    public void requestToJoinClub(Long clubId, String userAuthId, String userEmail) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ClubNotFoundException("Kulüp bulunamadı: " + clubId));


        if (club.getOwnerAuthId().equals(userAuthId)) {
            throw new IllegalStateException("Kendi kulübünüze üyelik isteği gönderemezsiniz.");
        }

        if (clubMemberRepository.existsByClubIdAndUserAuthId(clubId, userAuthId)) {
            throw new AlreadyMemberException("Bu kulübe zaten üyesiniz.");
        }

        if (membershipRequestRepository.existsByClubIdAndUserAuthIdAndStatus(clubId, userAuthId, ERequestStatus.PENDING)) {
            throw new IllegalStateException("Bu kulüp için zaten beklemede olan bir isteğiniz var.");
        }

        MembershipRequest newRequest = MembershipRequest.builder()
                .club(club)
                .userAuthId(userAuthId)
                .userEmail(userEmail)
                .status(ERequestStatus.PENDING)
                .build();

        membershipRequestRepository.save(newRequest);
        logger.info("Yeni üyelik isteği alındı: {} -> {}", userEmail, club.getName());
    }

    // --- Kulüp Sahibi Metotları (CLUB_OWNER rolü gerektirir) ---

    @Transactional
    public ClubResponse createClub(ClubRequest request, String ownerAuthId) {
        clubRepository.findByName(request.getName()).ifPresent(c -> {
            throw new IllegalStateException("Bu kulüp adı zaten alınmış.");
        });

        Club newClub = Club.builder()
                .name(request.getName())
                .description(request.getDescription())
                .ownerAuthId(ownerAuthId)
                .build();

        Club savedClub = clubRepository.save(newClub);

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

        request.setStatus(ERequestStatus.APPROVED);
        membershipRequestRepository.save(request);

        ClubMember newMember = ClubMember.builder()
                .club(request.getClub())
                .userAuthId(request.getUserAuthId())
                .build();
        clubMemberRepository.save(newMember);

        // RabbitMQ mesajı gönder
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

    // 🔥 LOGO YÜKLEME (GÜNCELLENDİ) 🔥
    @Transactional
    public ClubResponse uploadClubLogo(Long clubId, MultipartFile file) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ClubNotFoundException("Kulüp bulunamadı: " + clubId));

        // MinioService ile dosyayı yükle (Kod tekrarı yok!)
        String fileName = minioService.uploadFile(file);

        // Sadece dosya adını kaydet, URL'i değil!
        club.setLogoUrl(fileName);
        Club updatedClub = clubRepository.save(club);

        return mapToClubResponse(updatedClub);
    }

    @Transactional
    public ClubResponse updateClubInfo(Long clubId, ClubRequest request) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ClubNotFoundException("Kulüp bulunamadı: " + clubId));

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
        return mapToClubResponse(updatedClub);
    }

    // klüpten ayrılma işlemi
    @Transactional
    public void leaveClub(Long clubId, String userAuthId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ClubNotFoundException("Kulüp bulunamadı."));

        // Kulüp sahibi çıkamaz!
        if (club.getOwnerAuthId().equals(userAuthId)) {
            throw new IllegalStateException("Kulüp sahibi kulüpten ayrılamaz. Önce kulübü devretmelisiniz.");
        }

        // Üye değilse hata ver
        if (!clubMemberRepository.existsByClubIdAndUserAuthId(clubId, userAuthId)) {
            throw new IllegalStateException("Zaten bu kulübün üyesi değilsiniz.");
        }

        clubMemberRepository.deleteByClubIdAndUserAuthId(clubId, userAuthId);
        logger.info("Kullanıcı kulüpten ayrıldı: {} -> ClubId: {}", userAuthId, clubId);
    }

    // --- Klüpten atma işlemi ---
    @Transactional
    public void kickMember(Long clubId, String targetAuthId, String ownerAuthId) {
        // 1. Kulübü bul
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ClubNotFoundException("Kulüp bulunamadı."));

        // 2. İşlemi yapan kişi gerçekten sahip mi? (Service katmanında double-check)
        if (!club.getOwnerAuthId().equals(ownerAuthId)) {
            throw new AccessDeniedException("Bu işlem için yetkiniz yok.");
        }

        // 3. Atılacak kişi gerçekten üye mi?
        if (!clubMemberRepository.existsByClubIdAndUserAuthId(clubId, targetAuthId)) {
            throw new IllegalStateException("Kullanıcı bu kulübün üyesi değil.");
        }

        // 4. Kendini atamazsın
        if (targetAuthId.equals(ownerAuthId)) {
            throw new IllegalStateException("Kendinizi kulüpten atamazsınız.");
        }

        clubMemberRepository.deleteByClubIdAndUserAuthId(clubId, targetAuthId);
        logger.info("Üye kulüpten atıldı. Atan: {}, Atılan: {}, ClubId: {}", ownerAuthId, targetAuthId, clubId);
    }

    // --- LİSTELEME ---
    public List<MembershipRequestResponse> getMembershipRequests(Long clubId, ERequestStatus status) {
        if (!clubRepository.existsById(clubId)) {
            throw new ClubNotFoundException("Kulüp bulunamadı: " + clubId);
        }
        return membershipRequestRepository.findByClubIdAndStatus(clubId, status).stream()
                .map(this::mapToMembershipRequestResponse)
                .collect(Collectors.toList());
    }

    public List<ClubMemberResponse> getClubMembers(Long clubId) {
        if (!clubRepository.existsById(clubId)) {
            throw new ClubNotFoundException("Kulüp bulunamadı: " + clubId);
        }
        return clubMemberRepository.findByClubId(clubId).stream()
                .map(this::mapToClubMemberResponse)
                .collect(Collectors.toList());
    }

    // 🔥 MAPPER (URL DÖNÜŞÜMÜ BURADA) 🔥
    private ClubResponse mapToClubResponse(Club club) {
        String fullLogoUrl = null;

        // Veritabanından gelen sadece dosya adı ise, tam URL'e çeviriyoruz
        if (club.getLogoUrl() != null && !club.getLogoUrl().isEmpty()) {
            // Eğer veritabanında eski tam URL kaldıysa çift eklemeyi önle
            if (club.getLogoUrl().startsWith("http")) {
                fullLogoUrl = club.getLogoUrl();
            } else {
                fullLogoUrl = minioService.getFileUrl(club.getLogoUrl());
            }
        }

        long count = clubMemberRepository.countByClubId(club.getId());

        return ClubResponse.builder()
                .id(club.getId())
                .name(club.getName())
                .description(club.getDescription())
                .logoUrl(fullLogoUrl) // http://localhost:9000/uniclub-logos/resim.png
                .ownerAuthId(club.getOwnerAuthId())
                .memberCount(count)
                .build();
    }

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
                .build();
    }
}