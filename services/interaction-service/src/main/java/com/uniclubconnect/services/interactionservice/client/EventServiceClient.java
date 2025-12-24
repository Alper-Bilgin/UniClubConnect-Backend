package com.uniclubconnect.services.interactionservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "event-service", path = "/api/events")
public interface EventServiceClient {
    @GetMapping("/{id}")
    Object getEventById(@PathVariable("id") Long id);
}
