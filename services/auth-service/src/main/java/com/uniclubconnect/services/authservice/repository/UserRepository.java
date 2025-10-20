package com.uniclubconnect.services.authservice.repository;

import com.uniclubconnect.services.authservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    // E-postaya göre kullanıcı bul (Login için)
    Optional<User> findByEmail(String email);

    // E-posta zaten kayıtlı mı? (Register için)
    Boolean existsByEmail(String email);
}
