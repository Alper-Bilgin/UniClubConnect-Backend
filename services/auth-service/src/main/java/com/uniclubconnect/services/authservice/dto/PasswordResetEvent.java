package com.uniclubconnect.services.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetEvent implements java.io.Serializable {
    private String email;
    private String resetCode;
    private String firstName;
}
