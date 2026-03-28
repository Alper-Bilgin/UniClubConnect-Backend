package com.uniclubconnect.services.feedservice.client;

import com.uniclubconnect.services.feedservice.dto.FollowUserDto;
import com.uniclubconnect.services.feedservice.dto.PageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "follow-service")
public interface FollowClient {

    @GetMapping("/api/follows/{userId}/followers")
    PageResponse<FollowUserDto> getFollowers(
            @PathVariable("userId") String userId,
            @RequestParam("page") int page,
            @RequestParam("size") int size
    );
}
