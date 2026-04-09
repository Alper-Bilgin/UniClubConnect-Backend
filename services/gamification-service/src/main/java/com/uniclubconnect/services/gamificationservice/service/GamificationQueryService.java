package com.uniclubconnect.services.gamificationservice.service;

import com.uniclubconnect.services.gamificationservice.client.ProfileServiceClient;
import com.uniclubconnect.services.gamificationservice.dto.LeaderboardEntryDto;
import com.uniclubconnect.services.gamificationservice.dto.UserProfileDto;
import com.uniclubconnect.services.gamificationservice.model.DailyStreak;
import com.uniclubconnect.services.gamificationservice.model.UserBadge;
import com.uniclubconnect.services.gamificationservice.model.UserPoint;
import com.uniclubconnect.services.gamificationservice.repository.DailyStreakRepository;
import com.uniclubconnect.services.gamificationservice.repository.UserBadgeRepository;
import com.uniclubconnect.services.gamificationservice.repository.UserPointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GamificationQueryService {

    private final UserPointRepository userPointRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final DailyStreakRepository dailyStreakRepository;

    // YENİ EKLENEN FEIGN CLIENT
    private final ProfileServiceClient profileServiceClient;

    public UserPoint getUserPoints(String userId) {
        return userPointRepository.findById(userId)
                .orElse(UserPoint.builder().userId(userId).totalXp(0).currentLevel(1).build());
    }

    public List<UserBadge> getUserBadges(String userId) {
        return userBadgeRepository.findByUserId(userId);
    }

    public DailyStreak getUserStreak(String userId) {
        return dailyStreakRepository.findById(userId)
                .orElse(DailyStreak.builder().userId(userId).currentStreak(0).longestStreak(0).build());
    }

    // YENİ EKLENEN LİDERLİK TABLOSU MANTIĞI
    public List<LeaderboardEntryDto> getLeaderboard() {
        // 1. Veritabanından en yüksek XP'ye sahip ilk 10 kişiyi çek
        List<UserPoint> topUsers = userPointRepository.findTop10ByOrderByTotalXpDesc();

        // 2. Bu kişilerin ID'lerini isim/resim verileriyle eşleştir
        return topUsers.stream().map(userPoint -> {
            String firstName = "Bilinmeyen";
            String lastName = "Kullanıcı";
            String imageUrl = null;

            try {
                UserProfileDto profile = profileServiceClient.getProfileByAuthId(userPoint.getUserId());
                if (profile != null) {
                    firstName = profile.getFirstName();
                    lastName = profile.getLastName();
                    imageUrl = profile.getProfileImageUrl();
                }
            } catch (Exception e) {
                // Profil servisi o an meşgulse veya kapalıysa sistem çökmesin, logla geç
                log.warn("Kullanıcı profili çekilemedi: {}", userPoint.getUserId());
            }

            // Temiz DTO'yu oluştur ve listeye ekle
            return LeaderboardEntryDto.builder()
                    .userId(userPoint.getUserId())
                    .firstName(firstName)
                    .lastName(lastName)
                    .profileImageUrl(imageUrl)
                    .totalXp(userPoint.getTotalXp())
                    .currentLevel(userPoint.getCurrentLevel())
                    .build();

        }).collect(Collectors.toList());
    }
}