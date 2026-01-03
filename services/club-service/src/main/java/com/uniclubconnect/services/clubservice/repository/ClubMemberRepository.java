package com.uniclubconnect.services.clubservice.repository;

import com.uniclubconnect.services.clubservice.entity.ClubMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClubMemberRepository extends JpaRepository<ClubMember, Long> {
    // Bir kullanıcı bu kulübe zaten üye mi?
    boolean existsByClubIdAndUserAuthId(Long clubId, String userAuthId);

    // Bir kulübün tüm üyelerini getir
    List<ClubMember> findByClubId(Long clubId);

    // Verilen kulüp ID'sine sahip kaç kayıt var? (Hızlı sayım yapar)
    long countByClubId(Long clubId);
}
