package com.uniclubconnect.services.eventservice.repository;

import com.uniclubconnect.services.eventservice.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    // Kulübe göre etkinlikleri listelemek için
    List<Event> findByClubId(Long clubId);
}
