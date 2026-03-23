package com.uniclubconnect.services.followservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FollowUserDto {
    private String id; // Kullanıcının Auth ID'si
    private String firstName;
    private String lastName;
    private String profileImageUrl;
    private String department;

    private Integer mutualFriendsCount;
}
