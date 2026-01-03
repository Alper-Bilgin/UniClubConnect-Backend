package com.uniclubconnect.services.clubservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClubResponse {
    private Long id;
    private String name;
    private String description;
    private String logoUrl;
    private String ownerAuthId; // Sahip ID'si
    private long memberCount;
}