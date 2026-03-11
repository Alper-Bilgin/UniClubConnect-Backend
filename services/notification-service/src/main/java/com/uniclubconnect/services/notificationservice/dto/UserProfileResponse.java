package com.uniclubconnect.services.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private String authId;
    private String email;
    private String firstName;
    private String lastName;
}
