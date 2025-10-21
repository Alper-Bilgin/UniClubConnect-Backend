package com.uniclubconnect.services.profileservice.repository;

import com.uniclubconnect.services.profileservice.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    // authId'ye göre profil bul (API'lar için kritik)
    Optional<UserProfile> findByAuthId(String authId);

    boolean existsByAuthId(String authId);
}
