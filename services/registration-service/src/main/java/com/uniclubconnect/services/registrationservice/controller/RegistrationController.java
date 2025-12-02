package com.uniclubconnect.services.registrationservice.controller;

import com.uniclubconnect.services.registrationservice.dto.RegistrationResponse;
import com.uniclubconnect.services.registrationservice.dto.TicketValidationResponse;
import com.uniclubconnect.services.registrationservice.entity.Registration;
import com.uniclubconnect.services.registrationservice.security.dto.UserPrincipal;
import com.uniclubconnect.services.registrationservice.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/registrations")
public class RegistrationController {

    @Autowired
    private RegistrationService registrationService;

    // 1. KAYIT OL (USER)
    @PostMapping("/{eventId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<RegistrationResponse> register(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            Registration reg = registrationService.registerForEvent(eventId, principal.getAuthId(), principal.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(reg));
        } catch (IllegalStateException e) {
            // Kontenjan dolu veya mükerrer kayıt
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    // 2. BİLETLERİM (USER) - Kullanıcının kendi biletleri
    @GetMapping("/my-tickets")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<RegistrationResponse>> getMyTickets(@AuthenticationPrincipal UserPrincipal principal) {
        List<Registration> myRegistrations = registrationService.getMyRegistrations(principal.getAuthId());

        List<RegistrationResponse> response = myRegistrations.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // 3. BİLET İPTAL ET (USER) - Kendi biletini iptal etme
    @DeleteMapping("/{ticketCode}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> cancelMyTicket(
            @PathVariable String ticketCode,
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            registrationService.cancelTicket(ticketCode, principal.getAuthId());
            return ResponseEntity.ok("Bilet başarıyla iptal edildi.");
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    // 4. ETKİNLİK KATILIMCILARI (CLUB_OWNER) - Sadece kendi etkinliği ise
    @GetMapping("/event/{eventId}")
    @PreAuthorize("hasRole('CLUB_OWNER')")
    public ResponseEntity<List<RegistrationResponse>> getEventRegistrations(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            List<Registration> registrations = registrationService.getEventRegistrations(eventId, principal.getAuthId());
            List<RegistrationResponse> response = registrations.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // Yetkisiz erişim veya etkinlik bulunamadı
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
    }

    // 5. BİLET KONTROL / QR OKUMA (CLUB_OWNER)
    @PostMapping("/validate/{ticketCode}")
    // @PreAuthorize("hasRole('CLUB_OWNER')") // İsteğe bağlı, public de olabilir (mobil app için)
    public ResponseEntity<TicketValidationResponse> validateTicket(
            @PathVariable String ticketCode,
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            TicketValidationResponse response = registrationService.validateTicket(ticketCode, principal.getAuthId());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // Bilet bulunamadı veya yetkisiz
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    TicketValidationResponse.builder()
                            .valid(false)
                            .message(e.getMessage())
                            .build()
            );
        }
    }

    // --- Yardımcı Metot: Entity -> DTO Dönüşümü ---
    private RegistrationResponse mapToResponse(Registration reg) {
        return RegistrationResponse.builder()
                .ticketCode(reg.getTicketCode())
                .eventName(reg.getEventTitle()) // DB'ye kaydetmiştik
                .eventDate(reg.getEventDate())
                .eventLocation(reg.getEventLocation())
                .userName(reg.getUserName())
                .userEmail(reg.getUserEmail())
                .registrationDate(reg.getRegistrationDate())
                .status(reg.getStatus().name())
                .build();
    }
}