package com.uniclubconnect.services.followservice.service;

import com.uniclubconnect.services.followservice.client.ProfileServiceClient;
import com.uniclubconnect.services.followservice.config.RabbitMQConfig;
import com.uniclubconnect.services.followservice.dto.FollowEvent;
import com.uniclubconnect.services.followservice.dto.FollowUserDto;
import com.uniclubconnect.services.followservice.dto.UserProfileDto;
import com.uniclubconnect.services.followservice.dto.UserRecommendationProjection;
import com.uniclubconnect.services.followservice.model.Follow;
import com.uniclubconnect.services.followservice.model.FollowSetting;
import com.uniclubconnect.services.followservice.model.FollowStatus;
import com.uniclubconnect.services.followservice.repository.FollowRepository;
import com.uniclubconnect.services.followservice.repository.FollowSettingRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository repository;
    private final FollowSettingRepository settingRepository;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate redis;
    private final ProfileServiceClient profileServiceClient;

    private static final String FOLLOWER_COUNT = "user:follower_count:";
    private static final String FOLLOWING_COUNT = "user:following_count:";

    // --- 1. MADDE: GİZLİLİK DURUMUNU OKUMA ---
    public boolean isUserPrivate(String userId) {
        return settingRepository.findById(userId)
                .map(FollowSetting::isPrivate)
                .orElse(false);
    }

    // --- TAKİP ETME ---
    @Transactional
    public void followUser(String followerId, String targetId) {

        if (followerId.equals(targetId))
            throw new IllegalArgumentException("Kendinizi takip edemezsiniz");

        if (repository.existsByFollowerIdAndFollowingId(followerId, targetId)) {
            return;
        }

        boolean isTargetPrivate = isUserPrivate(targetId);

        FollowStatus status = isTargetPrivate
                ? FollowStatus.PENDING
                : FollowStatus.ACCEPTED;

        Follow follow = Follow.builder()
                .followerId(followerId)
                .followingId(targetId)
                .status(status)
                .build();

        repository.save(follow);

        if (status == FollowStatus.ACCEPTED) {
            redis.delete(FOLLOWER_COUNT + targetId);
            redis.delete(FOLLOWING_COUNT + followerId);
        }

        String eventType = status == FollowStatus.ACCEPTED
                ? "FOLLOW_CREATED"
                : "FOLLOW_REQUESTED";

        publishEvent(followerId, targetId, eventType);
    }

    // --- TAKİPTEN ÇIKMA ---
    @Transactional
    public void unfollowUser(String followerId, String targetId) {

        Optional<Follow> followOpt =
                repository.findByFollowerIdAndFollowingId(followerId, targetId);

        if (followOpt.isEmpty()) return;

        repository.delete(followOpt.get());

        if (followOpt.get().getStatus() == FollowStatus.ACCEPTED) {
            redis.delete(FOLLOWER_COUNT + targetId);
            redis.delete(FOLLOWING_COUNT + followerId);
        }

        publishEvent(followerId, targetId, "FOLLOW_REMOVED");
    }

    // --- TAKİPÇİ ÇIKARMA ---
    @Transactional
    public void removeFollower(String myId, String followerId) {

        repository.deleteByFollowerIdAndFollowingIdAndStatus(
                followerId,
                myId,
                FollowStatus.ACCEPTED
        );

        redis.delete(FOLLOWER_COUNT + myId);
        redis.delete(FOLLOWING_COUNT + followerId);

        publishEvent(myId, followerId, "FOLLOWER_REMOVED");
    }

    // --- İSTEK KABUL ETME ---
    @Transactional
    public void acceptRequest(String myId, String followerId) {

        Follow follow = repository
                .findByFollowerIdAndFollowingId(followerId, myId)
                .orElseThrow(() -> new IllegalArgumentException("İstek bulunamadı"));

        if (!follow.getFollowingId().equals(myId)) {
            throw new IllegalArgumentException("Yetkisiz işlem");
        }

        if (follow.getStatus() == FollowStatus.PENDING) {

            follow.setStatus(FollowStatus.ACCEPTED);
            repository.save(follow);

            redis.delete(FOLLOWER_COUNT + myId);
            redis.delete(FOLLOWING_COUNT + followerId);

            publishEvent(myId, followerId, "FOLLOW_ACCEPTED");
        }
    }

    // --- İSTEK REDDETME ---
    @Transactional
    public void rejectRequest(String myId, String followerId) {

        repository.deleteByFollowerIdAndFollowingIdAndStatus(
                followerId,
                myId,
                FollowStatus.PENDING
        );

        publishEvent(myId, followerId, "FOLLOW_REJECTED");
    }

    // --- DURUM SORGULAMA ---
    public String getFollowStatus(String myId, String targetId) {

        Optional<Follow> follow =
                repository.findByFollowerIdAndFollowingId(myId, targetId);

        if (follow.isEmpty()) return "NONE";

        return follow.get().getStatus().name();
    }

    // --- İSTATİSTİKLER ---
    public Map<String, Long> getStats(String userId) {

        String fKey = FOLLOWER_COUNT + userId;
        String gKey = FOLLOWING_COUNT + userId;

        String fVal = redis.opsForValue().get(fKey);
        String gVal = redis.opsForValue().get(gKey);

        long followers =
                (fVal != null)
                        ? Long.parseLong(fVal)
                        : fetchAndCache(
                        fKey,
                        () -> repository.countByFollowingIdAndStatus(
                                userId,
                                FollowStatus.ACCEPTED
                        )
                );

        long following =
                (gVal != null)
                        ? Long.parseLong(gVal)
                        : fetchAndCache(
                        gKey,
                        () -> repository.countByFollowerIdAndStatus(
                                userId,
                                FollowStatus.ACCEPTED
                        )
                );

        return Map.of(
                "followers", followers,
                "following", following
        );
    }

    private long fetchAndCache(String key, Supplier<Long> dbQuery) {

        long count = dbQuery.get();

        redis.opsForValue().set(key, String.valueOf(count));

        return count;
    }

    // --- FOLLOWERS ---
    public Page<FollowUserDto> getFollowers(String userId, Pageable pageable) {

        return repository
                .findByFollowingIdAndStatus(userId, FollowStatus.ACCEPTED, pageable)
                .map(follow -> mapToFollowUserDto(follow.getFollowerId()));
    }

    // --- FOLLOWING ---
    public Page<FollowUserDto> getFollowing(String userId, Pageable pageable) {

        return repository
                .findByFollowerIdAndStatus(userId, FollowStatus.ACCEPTED, pageable)
                .map(follow -> mapToFollowUserDto(follow.getFollowingId()));
    }

    // --- PENDING REQUESTS ---
    public Page<FollowUserDto> getPendingRequests(String myId, Pageable pageable) {

        return repository
                .findByFollowingIdAndStatus(myId, FollowStatus.PENDING, pageable)
                .map(follow -> mapToFollowUserDto(follow.getFollowerId()));
    }

    // --- GİZLİLİK AYARI ---
    @Transactional
    public void togglePrivacy(String myId, boolean isPrivate) {

        FollowSetting setting =
                settingRepository.findById(myId)
                        .orElse(FollowSetting.builder().userId(myId).build());

        setting.setPrivate(isPrivate);

        settingRepository.save(setting);
    }

    // --- DTO MAPPER ---
    private FollowUserDto mapToFollowUserDto(String authId) {

        String firstName = "Bilinmeyen";
        String lastName = "Kullanıcı";
        String imageUrl = null;
        String department = null;

        try {

            UserProfileDto profile =
                    profileServiceClient.getProfileByAuthId(authId);

            if (profile != null) {
                firstName = profile.getFirstName();
                lastName = profile.getLastName();
                imageUrl = profile.getProfileImageUrl();
                department = profile.getDepartment();
            }

        } catch (Exception e) {
            // profile service kapalıysa fallback
        }

        return FollowUserDto.builder()
                .id(authId)
                .firstName(firstName)
                .lastName(lastName)
                .profileImageUrl(imageUrl)
                .department(department)
                .build();
    }

    // --- EVENT PUBLISHER ---
    private void publishEvent(String actorId, String targetId, String type) {

        FollowEvent event = FollowEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .followerId(actorId)
                .followingId(targetId)
                .type(type)
                .timestamp(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                event
        );
    }

    public List<FollowUserDto> getRecommendations(String userId, int limit) {
        // En iyi X kişiyi getirmesi için limit koyuyoruz
        Pageable pageable = PageRequest.of(0, limit);

        List<UserRecommendationProjection> projections = repository.getRecommendations(userId, pageable);

        // SQL'den gelen ham ID'leri Profile Service ile isim/resime çevir
        return projections.stream().map(proj -> {
            // Zaten var olan yardımcı metodumuzu kullanıyoruz
            FollowUserDto dto = mapToFollowUserDto(proj.getRecommendedUserId());

            // Ortak arkadaş sayısını da DTO'ya ekle
            dto.setMutualFriendsCount(proj.getMutualCount());

            return dto;
        }).collect(Collectors.toList());
    }
}