package com.uniclubconnect.services.gamificationservice.config;

import com.uniclubconnect.services.gamificationservice.model.Badge;
import com.uniclubconnect.services.gamificationservice.repository.BadgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BadgeDataSeeder implements CommandLineRunner {

    private final BadgeRepository badgeRepository;

    @Override
    public void run(String... args) {
        // Eğer veritabanında hiç rozet yoksa, başlangıç rozetlerini ekle
        if (badgeRepository.count() == 0) {
            badgeRepository.save(Badge.builder()
                    .id("HELLO_WORLD")
                    .name("Merhaba Dünyalı 👽")
                    .description("UniClub Connect evrenine ilk adımını attın!")
                    .xpReward(50) // Bu rozeti kazanana ekstra 50 XP
                    .build());

            badgeRepository.save(Badge.builder()
                    .id("FIRST_POST")
                    .name("İlk Söz 🎤")
                    .description("Sistemde ilk gönderini paylaştın, sesini duyurdun!")
                    .xpReward(100)
                    .build());

            System.out.println("✅ Başlangıç rozetleri (HELLO_WORLD, FIRST_POST) veritabanına başarıyla eklendi!");
        }
    }
}
