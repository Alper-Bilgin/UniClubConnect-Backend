package com.uniclubconnect.services.notificationservice.client;

import com.uniclubconnect.services.notificationservice.dto.UserProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// name kısmı, profile servisinin application.yml dosyasındaki spring.application.name değeridir
@FeignClient(name = "user-profile-service")
public interface ProfileServiceClient {

    // Senin Profil servisinde permitAll() yaptığın endpoint
    @GetMapping("/api/profiles/user/{authId}")
    UserProfileResponse getUserProfile(@PathVariable("authId") String authId);
}
