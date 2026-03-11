package com.uniclubconnect.services.followservice.controller;

import com.uniclubconnect.services.followservice.security.dto.UserPrincipal;
import com.uniclubconnect.services.followservice.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService service;

    @PostMapping("/{targetId}")
    public ResponseEntity<?> follow(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable String targetId) {
        service.followUser(user.getAuthId(), targetId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping("/{targetId}")
    public ResponseEntity<?> unfollow(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable String targetId) {
        service.unfollowUser(user.getAuthId(), targetId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // Yeni: Detaylı Status Dönen Endpoint ("NONE", "PENDING", "ACCEPTED")
    @GetMapping("/status/{targetId}")
    public ResponseEntity<?> getFollowStatus(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable String targetId) {
        String status = service.getFollowStatus(user.getAuthId(), targetId);
        return ResponseEntity.ok(Map.of("status", status));
    }

    @GetMapping("/{userId}/counts")
    public ResponseEntity<?> getStats(@PathVariable String userId) {
        return ResponseEntity.ok(service.getStats(userId));
    }

    @GetMapping("/{userId}/followers")
    public ResponseEntity<Page<String>> getFollowers(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getFollowers(userId, PageRequest.of(page, size)));
    }

    @GetMapping("/{userId}/following")
    public ResponseEntity<Page<String>> getFollowing(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getFollowing(userId, PageRequest.of(page, size)));
    }

    @GetMapping("/requests")
    public ResponseEntity<Page<String>> getPendingRequests(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getPendingRequests(user.getAuthId(), PageRequest.of(page, size)));
    }

    @PutMapping("/requests/{followerId}/accept")
    public ResponseEntity<?> acceptRequest(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable String followerId) {
        service.acceptRequest(user.getAuthId(), followerId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping("/requests/{followerId}/reject")
    public ResponseEntity<?> rejectRequest(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable String followerId) {
        service.rejectRequest(user.getAuthId(), followerId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PutMapping("/settings/privacy")
    public ResponseEntity<?> updatePrivacy(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam boolean isPrivate) {
        service.togglePrivacy(user.getAuthId(), isPrivate);
        return ResponseEntity.ok(Map.of("success", true, "isPrivate", isPrivate));
    }
}