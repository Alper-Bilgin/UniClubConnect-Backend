package com.uniclubconnect.services.followservice.dto;

import lombok.Data;

// Profile Service'ten gelecek cevabı karşılayacak sınıf
@Data
public class UserProfileDto {
    private String firstName;
    private String lastName;
    private String profileImageUrl;
    private String department;
}
