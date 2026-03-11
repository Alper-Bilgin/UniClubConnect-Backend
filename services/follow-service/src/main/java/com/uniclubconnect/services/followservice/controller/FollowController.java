package com.uniclubconnect.services.followservice.controller;

import com.uniclubconnect.services.followservice.dto.FollowUserDto;
import com.uniclubconnect.services.followservice.security.dto.UserPrincipal;
import com.uniclubconnect.services.followservice.service.FollowService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService service;

    // --- FOLLOW USER ---
    @PostMapping("/{targetId}")
    public ResponseEntity<?> follow(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable String targetId) {

        service.followUser(user.getAuthId(), targetId);

        return ResponseEntity.ok(Map.of("success", true));
    }

    // --- UNFOLLOW USER ---
    @DeleteMapping("/{targetId}")
    public ResponseEntity<?> unfollow(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable String targetId) {

        service.unfollowUser(user.getAuthId(), targetId);

        return ResponseEntity.ok(Map.of("success", true));
    }

    // --- REMOVE FOLLOWER ---
    @DeleteMapping("/followers/{followerId}")
    public ResponseEntity<?> removeFollower(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable String followerId) {

        service.removeFollower(user.getAuthId(), followerId);

        return ResponseEntity.ok(Map.of("success", true));
    }

    // --- FOLLOW STATUS ---
    @GetMapping("/status/{targetId}")
    public ResponseEntity<?> getFollowStatus(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable String targetId) {

        String status = service.getFollowStatus(
                user.getAuthId(),
                targetId
        );

        return ResponseEntity.ok(Map.of("status", status));
    }

    // --- USER FOLLOW STATS ---
    @GetMapping("/{userId}/counts")
    public ResponseEntity<?> getStats(@PathVariable String userId) {

        return ResponseEntity.ok(service.getStats(userId));
    }

    // --- FOLLOWERS LIST ---
    @GetMapping("/{userId}/followers")
    public ResponseEntity<Page<FollowUserDto>> getFollowers(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                service.getFollowers(userId, PageRequest.of(page, size))
        );
    }

    // --- FOLLOWING LIST ---
    @GetMapping("/{userId}/following")
    public ResponseEntity<Page<FollowUserDto>> getFollowing(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                service.getFollowing(userId, PageRequest.of(page, size))
        );
    }

    // --- PENDING FOLLOW REQUESTS ---
    @GetMapping("/requests")
    public ResponseEntity<Page<FollowUserDto>> getPendingRequests(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                service.getPendingRequests(
                        user.getAuthId(),
                        PageRequest.of(page, size)
                )
        );
    }

    // --- ACCEPT FOLLOW REQUEST ---
    @PutMapping("/requests/{followerId}/accept")
    public ResponseEntity<?> acceptRequest(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable String followerId) {

        service.acceptRequest(
                user.getAuthId(),
                followerId
        );

        return ResponseEntity.ok(Map.of("success", true));
    }

    // --- REJECT FOLLOW REQUEST ---
    @DeleteMapping("/requests/{followerId}/reject")
    public ResponseEntity<?> rejectRequest(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable String followerId) {

        service.rejectRequest(
                user.getAuthId(),
                followerId
        );

        return ResponseEntity.ok(Map.of("success", true));
    }

    // --- UPDATE PRIVACY ---
    @PutMapping("/settings/privacy")
    public ResponseEntity<?> updatePrivacy(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam boolean isPrivate) {

        service.togglePrivacy(
                user.getAuthId(),
                isPrivate
        );

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "isPrivate", isPrivate
                )
        );
    }

    // --- GET MY PRIVACY STATUS ---
    @GetMapping("/settings/privacy")
    public ResponseEntity<Map<String, Boolean>> getMyPrivacyStatus(
            @AuthenticationPrincipal UserPrincipal user) {

        boolean isPrivate = service.isUserPrivate(user.getAuthId());

        return ResponseEntity.ok(
                Map.of("isPrivate", isPrivate)
        );
    }

    // --- GET USER PRIVACY STATUS ---
    @GetMapping("/{userId}/privacy-status")
    public ResponseEntity<Map<String, Boolean>> getUserPrivacyStatus(
            @PathVariable String userId) {

        boolean isPrivate = service.isUserPrivate(userId);

        return ResponseEntity.ok(
                Map.of("isPrivate", isPrivate)
        );
    }

}