package com.uniclubconnect.services.clubservice.security;

import com.uniclubconnect.services.clubservice.entity.Club;
import com.uniclubconnect.services.clubservice.exception.RequestNotFoundException;
import com.uniclubconnect.services.clubservice.repository.ClubRepository;
import com.uniclubconnect.services.clubservice.repository.MembershipRequestRepository;
import com.uniclubconnect.services.clubservice.security.dto.UserPrincipal; // <-- YENİ IMPORT
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("clubSecurity")
public class ClubSecurityService {

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private MembershipRequestRepository membershipRequestRepository;

    /**
     * Gelen Principal'ın (token sahibi), ilgili kulübün (clubId)
     * sahibi olup olmadığını kontrol eder.
     */
    // METOT İMZASINI GÜNCELLEYİN
    public boolean isOwner(UserPrincipal principal, Long clubId) {
        if (principal == null || clubId == null) {
            return false;
        }

        return clubRepository.findById(clubId)
                .map(Club::getOwnerAuthId)
                .map(ownerId -> ownerId.equals(principal.getAuthId())) // principal.getAuthId() kullan
                .orElse(false);
    }

    /**
     * Gelen Principal'ın (token sahibi), bir üyelik isteğinin (requestId)
     * ait olduğu kulübün sahibi olup olmadığını kontrol eder.
     */
    // METOT İMZASINI GÜNCELLEYİN
    public boolean isOwnerOfRequest(UserPrincipal principal, Long requestId) {
        if (principal == null || requestId == null) {
            return false;
        }

        return membershipRequestRepository.findById(requestId)
                .map(request -> request.getClub())
                .map(Club::getOwnerAuthId)
                .map(ownerId -> ownerId.equals(principal.getAuthId())) // principal.getAuthId() kullan
                .orElseThrow(() -> new RequestNotFoundException("Kontrol edilecek istek bulunamadı: " + requestId));
    }
}