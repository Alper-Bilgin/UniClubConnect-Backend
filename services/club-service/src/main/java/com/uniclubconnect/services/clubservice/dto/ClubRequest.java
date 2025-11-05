package com.uniclubconnect.services.clubservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClubRequest {
    @NotBlank
    @Size(min = 3, max = 100)
    private String name;

    @Size(max = 2000)
    private String description;
}