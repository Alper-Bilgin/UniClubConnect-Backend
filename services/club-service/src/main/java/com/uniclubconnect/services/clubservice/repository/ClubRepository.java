package com.uniclubconnect.services.clubservice.repository;

import com.uniclubconnect.services.clubservice.entity.Club;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClubRepository extends JpaRepository<Club, Long> {
    Optional<Club> findByName(String name);

    // Sahip ID'sine göre kulübü bul
    Optional<Club> findByOwnerAuthId(String ownerAuthId);

    // İsmi içinde 'name' geçenleri getir (Büyük/küçük harf duyarsız - ILIKE mantığı)
    List<Club> findByNameContainingIgnoreCase(String name);
}
