package com.uniclubconnect.services.feedservice.client;

import com.uniclubconnect.services.feedservice.dto.PostResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "post-service")
public interface PostClient {
    @PostMapping("/api/posts/batch")
    List<PostResponse> getPostsByIds(@RequestBody List<String> ids);
}
