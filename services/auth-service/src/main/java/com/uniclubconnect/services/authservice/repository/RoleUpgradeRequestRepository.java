package com.uniclubconnect.services.authservice.repository;

import com.uniclubconnect.services.authservice.entity.ERoleRequestStatus;
import com.uniclubconnect.services.authservice.entity.RoleUpgradeRequest;
import com.uniclubconnect.services.authservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleUpgradeRequestRepository extends JpaRepository<RoleUpgradeRequest, Long> {

    // Kullanıcının beklemede olan veya onaylanmış bir isteği var mı?
    boolean existsByRequestingUserAndStatusIn(User user, List<ERoleRequestStatus> statuses);

    // Admin'in listelemesi için
    List<RoleUpgradeRequest> findByStatus(ERoleRequestStatus status);

    // Kullanıcının kendi isteğini görmesi için
    Optional<RoleUpgradeRequest> findByRequestingUser(User user);
}