package com.uniclubconnect.services.postservice.client;

import com.uniclubconnect.services.postservice.dto.UserProfileDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Dikkat: Burada Profile Service'ten gelecek küçük bir DTO'ya ihtiyacımız var
@FeignClient(name = "user-profile-service")
public interface ProfileServiceClient {

    // Profile Service'te bu endpoint'i varsayıyoruz (yoksa eklemeliyiz)
    @GetMapping("/api/profiles/user/{authId}")
    UserProfileDto getProfileByAuthId(@PathVariable("authId") String authId);
}