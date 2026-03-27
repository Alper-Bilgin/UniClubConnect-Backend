package com.uniclubconnect.services.feedservice.client;

import com.uniclubconnect.services.feedservice.dto.CustomPageImpl;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "follow-service")
public interface FollowServiceClient {

    // Postu atan kişinin takipçilerini (SADECE ACCEPTED OLANLARI) çekmek için
    // Dikkat: Follow service'teki getFollowers endpointini kullanıyoruz.
    // Sayfalama olduğu için Spring'in Page yapısına uygun bir DTO (veya CustomPageImpl) kullanman gerekebilir.
    // Şimdilik en basit haliyle sayfa boyutunu 1000 yapıp herkesi çekiyoruz (Milyonluk sistemde bu sayfalama ile yapılır).
    @GetMapping("/api/follows/{userId}/followers?page=0&size=1000")
    CustomPageImpl<String> getFollowers(@PathVariable("userId") String userId);
}
