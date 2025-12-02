package com.uniclubconnect.services.registrationservice.security.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.io.Serializable;
import java.util.List;

// Token'dan okuduğumuz ve @AuthenticationPrincipal ile alacağımız nesne
@Getter
@AllArgsConstructor
public class UserPrincipal implements Serializable {
    private String authId;
    private String email;
    private List<String> roles; // Token'dan gelen roller
}
