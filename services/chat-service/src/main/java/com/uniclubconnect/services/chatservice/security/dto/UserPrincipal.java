package com.uniclubconnect.services.chatservice.security.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.security.Principal; // YENİ EKLENDİ
import java.util.List;

@Getter
@AllArgsConstructor
public class UserPrincipal implements Serializable, Principal { // Principal EKLENDİ

    private String authId;
    private String email;
    private List<String> roles;

    // 👇 YENİ EKLENDİ: Spring WebSocket'in seni doğru adreste bulmasını sağlar 👇
    @Override
    public String getName() {
        return this.authId;
    }
}
