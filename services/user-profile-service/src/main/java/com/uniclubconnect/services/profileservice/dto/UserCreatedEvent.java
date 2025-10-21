package com.uniclubconnect.services.profileservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// auth-service'ten gelen JSON'un kalıbı
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreatedEvent implements Serializable {
    private String authId;
    private String email;
    private String firstName;
    private String lastName;
}