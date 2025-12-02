package com.uniclubconnect.services.registrationservice.repository;

import com.uniclubconnect.services.registrationservice.entity.Registration;
import com.uniclubconnect.services.registrationservice.entity.ERegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    // Mükerrer kayıt kontrolü
    boolean existsByEventIdAndUserAuthIdAndStatus(Long eventId, String userAuthId, ERegistrationStatus status);

    // Kullanıcının biletleri
    List<Registration> findByUserAuthId(String userAuthId);

    // Etkinliğin katılımcıları
    List<Registration> findByEventId(Long eventId);

    // Bilet kodu ile bulma (Doğrulama için)
    Optional<Registration> findByTicketCode(String ticketCode);
}