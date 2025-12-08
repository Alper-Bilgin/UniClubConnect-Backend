package com.uniclubconnect.services.authservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreatedEvent implements Serializable {
    private String authId;
    private String email;
    private String firstName;
    private String lastName;
    private String verificationCode;
}