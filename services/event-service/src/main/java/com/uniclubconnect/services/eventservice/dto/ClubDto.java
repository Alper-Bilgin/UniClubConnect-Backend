package com.uniclubconnect.services.eventservice.dto;

import lombok.Data;

@Data
public class ClubDto {
    private Long id;
    private String name;
    // Diğer alanlara (logo, description) şimdilik gerek yok, sadece isim lazım.
}