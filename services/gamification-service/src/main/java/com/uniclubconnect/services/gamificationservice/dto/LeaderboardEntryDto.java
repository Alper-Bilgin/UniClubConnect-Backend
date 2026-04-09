package com.uniclubconnect.services.gamificationservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LeaderboardEntryDto {
    private String userId;
    private String firstName;
    private String lastName;
    private String profileImageUrl;
    private int totalXp;
    private int currentLevel;
}
