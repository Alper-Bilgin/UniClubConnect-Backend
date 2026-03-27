package com.uniclubconnect.services.feedservice.security.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;

@Getter
@AllArgsConstructor
public class UserPrincipal implements Serializable {
    private String authId;
    private String email;
    private List<String> roles;
}
