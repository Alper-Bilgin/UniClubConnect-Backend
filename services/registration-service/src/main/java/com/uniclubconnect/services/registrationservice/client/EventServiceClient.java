package com.uniclubconnect.services.registrationservice.client;

import com.uniclubconnect.services.registrationservice.dto.EventDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "event-service")
public interface EventServiceClient {

    // Etkinlik detaylarını getir
    @GetMapping("/api/events/{id}")
    EventDto getEventById(@PathVariable("id") Long id);
}