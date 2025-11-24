package com.uniclubconnect.services.eventservice.controller;

import com.uniclubconnect.services.eventservice.dto.EventRequest;
import com.uniclubconnect.services.eventservice.dto.EventResponse;
import com.uniclubconnect.services.eventservice.security.dto.UserPrincipal; // club-service'ten kopyaladığımız DTO
import com.uniclubconnect.services.eventservice.service.EventService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    @Autowired
    private EventService eventService;

    // --- HERKESE AÇIK (PUBLIC) ---

    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(eventService.getEventById(id));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    // --- KULÜP SAHİBİ (PROTECTED) ---

    @PostMapping
    @PreAuthorize("hasRole('CLUB_OWNER')")
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody EventRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            // principal.getAuthId() ile kullanıcının kimliğini servise gönderiyoruz
            EventResponse createdEvent = eventService.createEvent(request, principal.getAuthId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdEvent);
        } catch (Exception e) {
            // AccessDenied veya diğer hatalar için
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/{id}/image")
    @PreAuthorize("hasRole('CLUB_OWNER')")
    public ResponseEntity<EventResponse> uploadEventImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            EventResponse updatedEvent = eventService.uploadEventImage(id, file, principal.getAuthId());
            return ResponseEntity.ok(updatedEvent);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}