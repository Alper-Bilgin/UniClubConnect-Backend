package com.uniclubconnect.services.profileservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileResponse {
    private String authId;
    private String email;
    private String firstName;
    private String lastName;
    private String department;
    private String profileImageUrl;
    private Long totalPoints;
}