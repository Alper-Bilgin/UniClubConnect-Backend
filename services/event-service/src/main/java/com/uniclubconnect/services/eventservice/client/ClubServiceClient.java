package com.uniclubconnect.services.eventservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// "club-service" -> Eureka'daki isim
@FeignClient(name = "club-service")
public interface ClubServiceClient {
    // TODO
    // Club Service'e bu endpoint'i henüz eklemedik,
    // Event Service'i bitirince oraya gidip ekleyeceğiz.
    @GetMapping("/api/clubs/{clubId}/is-owner/{authId}")
    boolean isUserOwnerOfClub(@PathVariable("clubId") Long clubId, @PathVariable("authId") String authId);
}