package com.uniclubconnect.services.followservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.Id;

@Entity
@Table(name = "follow_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowSetting {

    @Id
    private String userId;

    @Builder.Default
    private boolean isPrivate = false;
}
