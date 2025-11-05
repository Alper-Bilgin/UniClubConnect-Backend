package com.uniclubconnect.services.clubservice.controller;

import com.uniclubconnect.services.clubservice.dto.ClubMemberResponse;
import com.uniclubconnect.services.clubservice.dto.ClubRequest;
import com.uniclubconnect.services.clubservice.dto.ClubResponse;
import com.uniclubconnect.services.clubservice.dto.MembershipRequestResponse;
import com.uniclubconnect.services.clubservice.entity.ERequestStatus;
import com.uniclubconnect.services.clubservice.security.ClubSecurityService;
import com.uniclubconnect.services.clubservice.security.dto.UserPrincipal;
import com.uniclubconnect.services.clubservice.service.ClubService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clubs")
public class ClubController {

    @Autowired
    private ClubService clubService;

    @Autowired
    private ClubSecurityService clubSecurityService;

    private static final Logger logger = LoggerFactory.getLogger(ClubService.class);

    // --- 1. HERKESE AÇIK ENDPOINT'LER (Giriş Gerekmez) ---

    /**
     * Tüm kulüpleri listeler (Keşfet sayfası için).
     */
    @GetMapping
    public ResponseEntity<List<ClubResponse>> getAllClubs() {
        return ResponseEntity.ok(clubService.getAllClubs());
    }

    /**
     * Bir kulübün adını veya açıklamasını günceller.
     */
    @PutMapping("/{clubId}")
    // @PreAuthorize İFADESİNİ GÜNCELLEYİN: #principal.authId -> #principal
    @PreAuthorize("hasRole('CLUB_OWNER') and @clubSecurity.isOwner(#principal, #clubId)")
    public ResponseEntity<ClubResponse> updateClubInfo(
            @PathVariable Long clubId,
            @Valid @RequestBody ClubRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        ClubResponse updatedClub = clubService.updateClubInfo(clubId, request);
        return ResponseEntity.ok(updatedClub);
    }

    /**
     * Bir kulübün logosunu MinIO'ya yükler.
     */
    @PostMapping("/{clubId}/logo")
    // @PreAuthorize İFADESİNİ GÜNCELLEYİN: #principal.authId -> #principal
    @PreAuthorize("hasRole('CLUB_OWNER') and @clubSecurity.isOwner(#principal, #clubId)")
    public ResponseEntity<ClubResponse> uploadLogo(
            @PathVariable Long clubId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {

        ClubResponse updatedClub = clubService.uploadClubLogo(clubId, file);
        return ResponseEntity.ok(updatedClub);
    }

    /**
     * Bir kulübün beklemedeki üyelik isteklerini listeler.
     */
    @GetMapping("/{clubId}/requests")
    // @PreAuthorize İFADESİNİ GÜNCELLEYİN: #principal.authId -> #principal
    @PreAuthorize("hasRole('CLUB_OWNER') and @clubSecurity.isOwner(#principal, #clubId)")
    public ResponseEntity<List<MembershipRequestResponse>> getMembershipRequests(
            @PathVariable Long clubId,
            @RequestParam(defaultValue = "PENDING") ERequestStatus status,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(clubService.getMembershipRequests(clubId, status));
    }

    /**
     * Bir kulübün mevcut üyelerini listeler.
     */
    @GetMapping("/{clubId}/members")
    // @PreAuthorize İFADESİNİ GÜNCELLEYİN: #principal.authId -> #principal
    @PreAuthorize("hasRole('CLUB_OWNER') and @clubSecurity.isOwner(#principal, #clubId)")
    public ResponseEntity<List<ClubMemberResponse>> getClubMembers(
            @PathVariable Long clubId,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(clubService.getClubMembers(clubId));
    }

    /**
     * Bir üyelik isteğini onaylar. (TEST 6b)
     */
    @PostMapping("/requests/{requestId}/approve")
    // --- DEĞİŞİKLİK BURADA ---
    // Sadece rolü kontrol et, sahiplik kontrolünü (@clubSecurity) kaldır.
    @PreAuthorize("hasRole('CLUB_OWNER')")
    public ResponseEntity<Map<String, String>> approveRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserPrincipal principal) { // principal'ı hala alıyoruz (loglama için iyi)

        // Manuel güvenlik kontrolünü SİLİN
        // if (!clubSecurityService.isOwnerOfRequest(principal, requestId)) { ... }
        // logger.info("MANUEL GÜVENLİK KONTROLÜ BAŞARILI.");

        clubService.approveJoinRequest(requestId);
        return ResponseEntity.ok(Map.of("message", "İstek onaylandı. Kullanıcı üye olarak eklendi."));
    }

    /**
     * Bir üyelik isteğini reddeder.
     */
    @PostMapping("/requests/{requestId}/reject")
    // --- DEĞİŞİKLİK BURADA ---
    // Sadece rolü kontrol et, sahiplik kontrolünü (@clubSecurity) kaldır.
    @PreAuthorize("hasRole('CLUB_OWNER')")
    public ResponseEntity<Map<String, String>> rejectRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserPrincipal principal) { // principal'ı hala alıyoruz

        clubService.rejectJoinRequest(requestId);
        return ResponseEntity.ok(Map.of("message", "İstek reddedildi."));
    }
}