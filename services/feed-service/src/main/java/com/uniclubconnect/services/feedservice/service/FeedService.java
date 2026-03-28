package com.uniclubconnect.services.feedservice.service;

import com.uniclubconnect.services.feedservice.client.FollowClient;
import com.uniclubconnect.services.feedservice.client.PostClient;
import com.uniclubconnect.services.feedservice.dto.FollowUserDto;
import com.uniclubconnect.services.feedservice.dto.PageResponse;
import com.uniclubconnect.services.feedservice.dto.PostEvent;
import com.uniclubconnect.services.feedservice.dto.PostResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final StringRedisTemplate redis;
    private final FollowClient followClient;
    private final PostClient postClient;

    private static final String FEED_KEY = "feed:";

    // YENİ POST ATILDIĞINDA
    public void handlePostCreated(PostEvent event) {
        String authorId = event.getAuthorId();
        String postId = event.getPostId();

        // 1. DTO olarak takipçileri çekiyoruz
        PageResponse<FollowUserDto> followers = followClient.getFollowers(authorId, 0, 1000);

        // 2. Eğer content null gelirse patlamasın diye kontrol ediyoruz
        if (followers.getContent() == null) {
            return;
        }

        // 3. DTO'ların içindeki sadece 'id' alanlarını alıp String listesine çeviriyoruz
        List<String> targetFeeds = followers.getContent().stream()
                .map(FollowUserDto::getId)
                .collect(java.util.stream.Collectors.toList());

        // 4. Postu atanı da kendi akışına ekle
        targetFeeds.add(authorId);

        for (String followerId : targetFeeds) {
            redis.opsForList().leftPush(FEED_KEY + followerId, postId);
            redis.opsForList().trim(FEED_KEY + followerId, 0, 500);
        }
    }

    // POST SİLİNDİĞİNDE (LAZY DELETE STRATEJİSİ)
    public void handlePostDeleted(PostEvent event) {
        // İÇİ BOŞ! Neden? Çünkü 1000 kişinin Redis listesini tek tek dönüp
        // silmek (O(N) işlemi) sistemi kilitler.
        // Bunun yerine aşağıda getFeed metodunda "Lazy Delete" yapacağız.
    }

    // FEED'İ GETİR VE ZENGİNLEŞTİR
    public List<PostResponse> getFeed(String userId, int page, int size) {
        int start = page * size;
        int end = start + size - 1;

        // 1. Redis'ten ID'leri çek
        List<String> postIds = redis.opsForList().range(FEED_KEY + userId, start, end);

        if (postIds == null || postIds.isEmpty()) return List.of();

        // 2. Post Service'ten Batch olarak içerikleri çek
        List<PostResponse> posts = postClient.getPostsByIds(postIds);

        // 3. LAZY DELETE MANTIĞI:
        // Redis'te ID var ama Post silinmişse, Post Service null döner.
        // Null olanları filtreleyerek silinmiş postları kullanıcıya göstermeyiz!
        return postIds.stream()
                .map(id -> posts.stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null))
                .filter(Objects::nonNull) // SİLİNMİŞLER BURADA ELENİR!
                .collect(Collectors.toList());
    }
}
