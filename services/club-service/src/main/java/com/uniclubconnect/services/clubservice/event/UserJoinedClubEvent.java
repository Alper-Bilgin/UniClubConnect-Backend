package com.uniclubconnect.services.clubservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// Gamification-service bu olayı dinleyip puan verecek
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserJoinedClubEvent implements Serializable {
    private String userAuthId;
    private Long clubId;
    private String clubName;
}