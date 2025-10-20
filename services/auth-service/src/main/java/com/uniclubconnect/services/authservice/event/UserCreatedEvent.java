package com.uniclubconnect.services.authservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// Bu, profile-service'in ihtiyaç duyacağı bilgileri içeren DTO'dur.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreatedEvent implements Serializable {
    private String authId; // Yeni kullanıcının UUID'si
    private String email;
    private String firstName;
    private String lastName;
}