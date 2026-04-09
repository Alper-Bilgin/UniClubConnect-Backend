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

        // 🔥 STREAK ROZETLERİ

        if (!badgeRepository.existsById("STREAK_7")) {
            badgeRepository.save(Badge.builder()
                    .id("STREAK_7")
                    .name("1 Haftalık Ateş 🔥")
                    .description("7 gün boyunca aralıksız giriş yaptın!")
                    .xpReward(100)
                    .build());
        }

        if (!badgeRepository.existsById("STREAK_30")) {
            badgeRepository.save(Badge.builder()
                    .id("STREAK_30")
                    .name("Aylık Müdavim 🗓️")
                    .description("30 gün boyunca aralıksız bizimlesin!")
                    .xpReward(300)
                    .build());
        }

        if (!badgeRepository.existsById("STREAK_60")) {
            badgeRepository.save(Badge.builder()
                    .id("STREAK_60")
                    .name("Alışkanlık Canavarı 👹")
                    .description("60 gündür buralardasın, harika!")
                    .xpReward(600)
                    .build());
        }

        if (!badgeRepository.existsById("STREAK_90")) {
            badgeRepository.save(Badge.builder()
                    .id("STREAK_90")
                    .name("Çeyrek Asır (Gibi) ⏳")
                    .description("90 günlük inanılmaz bir seri!")
                    .xpReward(1000)
                    .build());
        }

        if (!badgeRepository.existsById("STREAK_100")) {
            badgeRepository.save(Badge.builder()
                    .id("STREAK_100")
                    .name("Dalya! 💯")
                    .description("100 GÜN! Sen bir efsanesin!")
                    .xpReward(1500)
                    .build());
        }

        if (!badgeRepository.existsById("STREAK_365")) {
            badgeRepository.save(Badge.builder()
                    .id("STREAK_365")
                    .name("YILIN BAŞKANI 👑")
                    .description("1 TAM YIL boyunca aralıksız giriş yaptın!")
                    .xpReward(5000)
                    .build());
        }

        System.out.println("✅ Rozetler kontrol edildi ve eksikler eklendi!");
    }
}