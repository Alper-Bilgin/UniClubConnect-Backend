package com.uniclubconnect.services.profileservice.controller;

import com.uniclubconnect.services.profileservice.dto.UpdateProfileRequest;
import com.uniclubconnect.services.profileservice.dto.UserProfileResponse;
import com.uniclubconnect.services.profileservice.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    @Autowired
    private UserProfileService userProfileService;

    // Kendi profilini getir
    // @AuthenticationPrincipal, AuthTokenFilter'da Principal olarak set ettiğimiz 'authId'yi alır.
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile(@AuthenticationPrincipal String authId) {
        try {
            UserProfileResponse profile = userProfileService.getProfileByAuthId(authId);
            return ResponseEntity.ok(profile);
        } catch (UserProfileService.ProfileNotFoundException e) {
            // Normalde bu olmamalı (token geçerliyse profili vardır), ama kontrol edelim.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    // Kendi profilini güncelle
    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateCurrentUserProfile(
            @AuthenticationPrincipal String authId,
            @Valid @RequestBody UpdateProfileRequest request) {
        try {
            UserProfileResponse updatedProfile = userProfileService.updateProfile(authId, request);
            return ResponseEntity.ok(updatedProfile);
        } catch (UserProfileService.ProfileNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    // Kendi profil resmini yükle
    @PostMapping("/me/image")
    public ResponseEntity<UserProfileResponse> uploadProfileImage(
            @AuthenticationPrincipal String authId,
            @RequestParam("file") MultipartFile file) { // Form-data olarak 'file' beklenir
        try {
            // Basit dosya tipi kontrolü (isteğe bağlı)
            if (file.isEmpty() || !isImageFile(file.getContentType())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Geçersiz resim dosyası.");
            }

            UserProfileResponse updatedProfile = userProfileService.uploadProfileImage(authId, file);
            return ResponseEntity.ok(updatedProfile);
        } catch (UserProfileService.ProfileNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Dosya yüklenirken hata: " + e.getMessage());
        }
    }

    // Basit resim tipi kontrolü
    private boolean isImageFile(String contentType) {
        return contentType != null && (contentType.equals("image/jpeg") || contentType.equals("image/png") || contentType.equals("image/gif"));
    }

    // Başka bir kullanıcının profilini getir (Post Service'in Feign Client'ı ve Frontend için)
    @GetMapping("/user/{authId}")
    public ResponseEntity<UserProfileResponse> getUserProfileByAuthId(@PathVariable String authId) {
        try {
            // Zaten mevcut olan getProfileByAuthId servisini kullanıyoruz
            UserProfileResponse profile = userProfileService.getProfileByAuthId(authId);
            return ResponseEntity.ok(profile);
        } catch (UserProfileService.ProfileNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Kullanıcı profili bulunamadı: " + authId);
        }
    }

}