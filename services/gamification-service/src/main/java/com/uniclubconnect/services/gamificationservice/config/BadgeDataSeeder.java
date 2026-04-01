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

        // HELLO_WORLD
        if (!badgeRepository.existsById("HELLO_WORLD")) {
            badgeRepository.save(Badge.builder()
                    .id("HELLO_WORLD")
                    .name("Merhaba Dünyalı 👽")
                    .description("UniClub Connect evrenine ilk adımını attın!")
                    .xpReward(50)
                    .build());
        }

        // FIRST_POST
        if (!badgeRepository.existsById("FIRST_POST")) {
            badgeRepository.save(Badge.builder()
                    .id("FIRST_POST")
                    .name("İlk Söz 🎤")
                    .description("Sistemde ilk gönderini paylaştın, sesini duyurdun!")
                    .xpReward(100)
                    .build());
        }

        // FIVE_POSTS
        if (!badgeRepository.existsById("FIVE_POSTS")) {
            badgeRepository.save(Badge.builder()
                    .id("FIVE_POSTS")
                    .name("Seri Üretim 🏭")
                    .description("Sistemde 5 gönderiye ulaştın, hız kesmeden devam ediyorsun!")
                    .xpReward(200)
                    .build());
        }

        System.out.println("✅ Rozetler kontrol edildi ve eksikler eklendi!");
    }
}