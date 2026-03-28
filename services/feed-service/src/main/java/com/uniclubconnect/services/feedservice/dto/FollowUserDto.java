package com.uniclubconnect.services.feedservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // Sadece ID'yi al, gerisini umursama
public class FollowUserDto {
    private String id;
}
