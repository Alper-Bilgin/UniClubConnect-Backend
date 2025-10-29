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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin") // SecurityConfig'de bu yola izin verdik
public class AdminController {

    @Autowired
    private AuthService authService;

    // Bekleyen rol isteklerini listeleme
    @GetMapping("/role-requests/pending")
    @PreAuthorize("hasRole('ADMIN')") // Sadece Admin erişebilir
    public ResponseEntity<List<RoleUpgradeRequestResponse>> getPendingRoleRequests() {
        List<RoleUpgradeRequestResponse> requests = authService.getPendingRoleRequests();
        return ResponseEntity.ok(requests);
    }

    // Rol isteğini onaylama
    @PostMapping("/role-requests/{requestId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    // @AuthenticationPrincipal String adminUserId YERİNE User adminUser KULLANIN
    public ResponseEntity<?> approveRoleRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal User adminUser) {
        if (adminUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("Admin kimliği alınamadı."));
        }
        try {
            // Servise adminin ID'sini gönderin
            RoleUpgradeRequestResponse response = authService.approveRoleRequest(requestId, adminUser.getId());
            return ResponseEntity.ok(response);
        } catch (AuthService.RequestNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    // Rol isteğini reddetme
    @PostMapping("/role-requests/{requestId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    // @AuthenticationPrincipal String adminUserId YERİNE User adminUser KULLANIN
    public ResponseEntity<?> rejectRoleRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal User adminUser) {
        if (adminUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("Admin kimliği alınamadı."));
        }
        try {
            // Servise adminin ID'sini gönderin
            RoleUpgradeRequestResponse response = authService.rejectRoleRequest(requestId, adminUser.getId());
            return ResponseEntity.ok(response);
        } catch (AuthService.RequestNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
}