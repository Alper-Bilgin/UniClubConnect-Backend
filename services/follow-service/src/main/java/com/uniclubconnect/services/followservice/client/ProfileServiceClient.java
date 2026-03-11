package com.uniclubconnect.services.followservice.client;

import com.uniclubconnect.services.followservice.dto.UserProfileDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-profile-service")
public interface ProfileServiceClient {
    @GetMapping("/api/profiles/user/{authId}")
    UserProfileDto getProfileByAuthId(@PathVariable("authId") String authId);
}
