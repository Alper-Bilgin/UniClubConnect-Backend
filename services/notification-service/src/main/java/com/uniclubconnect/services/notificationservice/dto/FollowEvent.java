package com.uniclubconnect.services.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowEvent implements Serializable {
    private String eventId;
    private String followerId;
    private String followingId;
    private String type; // "FOLLOW_REQUESTED", "FOLLOW_ACCEPTED" vb.
    private LocalDateTime timestamp;
}
