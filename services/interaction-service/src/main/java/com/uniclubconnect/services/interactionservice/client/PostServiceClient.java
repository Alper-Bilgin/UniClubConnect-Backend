package com.uniclubconnect.services.interactionservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "post-service", path = "/api/posts")
public interface PostServiceClient {
    // Sadece var olup olmadığını kontrol etmek için basit bir GET yeterli
    // Eğer hata dönerse (404) Feign Exception fırlatır, onu yakalarız.
    @GetMapping("/{postId}")
    Object getPostById(@PathVariable("postId") String postId);
}