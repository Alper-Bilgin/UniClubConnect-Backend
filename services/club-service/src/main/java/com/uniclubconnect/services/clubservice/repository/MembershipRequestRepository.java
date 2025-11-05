package com.uniclubconnect.services.clubservice.repository;

import com.uniclubconnect.services.clubservice.entity.ERequestStatus;
import com.uniclubconnect.services.clubservice.entity.MembershipRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MembershipRequestRepository extends JpaRepository<MembershipRequest, Long> {
    // Bir kulübün beklemedeki isteklerini bul
    List<MembershipRequest> findByClubIdAndStatus(Long clubId, ERequestStatus status);

    // Bir kullanıcının bu kulübe zaten istek atıp atmadığını kontrol et
    boolean existsByClubIdAndUserAuthIdAndStatus(Long clubId, String userAuthId, ERequestStatus status);
}
