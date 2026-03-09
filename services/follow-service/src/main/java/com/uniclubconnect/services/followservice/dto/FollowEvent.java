package com.uniclubconnect.services.followservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FollowEvent {

    private String eventId;

    private String followerId;

    private String followingId;

    private String type;

    private LocalDateTime timestamp;

}
