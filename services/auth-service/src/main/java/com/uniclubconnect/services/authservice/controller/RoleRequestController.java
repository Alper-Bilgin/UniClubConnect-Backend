package com.uniclubconnect.services.authservice.controller;

import com.uniclubconnect.services.authservice.dto.MessageResponse;
import com.uniclubconnect.services.authservice.dto.RoleUpgradeRequestResponse;
import com.uniclubconnect.services.authservice.entity.User;
import com.uniclubconnect.services.authservice.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/requests") // SecurityConfig'de bu yola izin verdik
public class RoleRequestController {

    @Autowired
    private AuthService authService;

    @PostMapping("/club-owner-role")
    @PreAuthorize("hasRole('USER')")
    // @AuthenticationPrincipal String userId YERİNE User user KULLANIN
    public ResponseEntity<?> requestClubOwnerRole(@AuthenticationPrincipal User user) {
        if (user == null) {
            // Token geçerliyse bu olmamalı, ama kontrol edelim
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("Kullanıcı kimliği alınamadı."));
        }
        try {
            // Servise kullanıcının ID'sini (String/UUID) gönderin
            RoleUpgradeRequestResponse response = authService.requestClubOwnerRole(user.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        } catch (AuthService.UserNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping("/my-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCurrentUserRoleRequestStatus(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        RoleUpgradeRequestResponse response = authService.getCurrentUserRoleRequestStatus(user.getId());

        if (response == null) {
            // 404 yerine 204 (No Content) dönelim.
            // Bu "İstek başarılı ama gösterecek veri yok" demektir.
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }
}