package com.uniclubconnect.services.followservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name="follows",
        indexes={
                @Index(name="idx_follower",columnList="follower_id"),
                @Index(name="idx_following",columnList="following_id")
        },
        uniqueConstraints={
                @UniqueConstraint(columnNames={"follower_id","following_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Follow {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @Column(name="follower_id",nullable=false)
    private String followerId;

    @Column(name="following_id",nullable=false)
    private String followingId;

    @CreationTimestamp
    private LocalDateTime createdAt;

}
