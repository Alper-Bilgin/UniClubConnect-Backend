package com.uniclubconnect.services.clubservice.security;

import com.uniclubconnect.services.clubservice.entity.Club;
import com.uniclubconnect.services.clubservice.exception.RequestNotFoundException;
import com.uniclubconnect.services.clubservice.repository.ClubRepository;
import com.uniclubconnect.services.clubservice.repository.MembershipRequestRepository;
import com.uniclubconnect.services.clubservice.security.dto.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("clubSecurity")
public class ClubSecurityService {

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private MembershipRequestRepository membershipRequestRepository;

    // --- 1. VERSİYON: String ID ile kontrol (Event-Service için gerekli) ---
    public boolean isOwner(String authId, Long clubId) {
        if (authId == null || clubId == null) {
            return false;
        }

        return clubRepository.findById(clubId)
                .map(Club::getOwnerAuthId)
                .map(ownerId -> ownerId.equals(authId))
                .orElse(false);
    }

    // --- 2. VERSİYON: UserPrincipal ile kontrol (@PreAuthorize için gerekli) ---
    // Bu metot, yukarıdaki String versiyonunu çağırarak kod tekrarını önler.
    public boolean isOwner(UserPrincipal principal, Long clubId) {
        if (principal == null) {
            return false;
        }
        // Principal'ın içinden ID'yi alıp diğer metoda devret
        return isOwner(principal.getAuthId(), clubId);
    }

    // --- DİĞER METOTLAR ---
    public boolean isOwnerOfRequest(UserPrincipal principal, Long requestId) {
        if (principal == null || requestId == null) {
            return false;
        }
        return membershipRequestRepository.findById(requestId)
                .map(request -> request.getClub())
                .map(Club::getOwnerAuthId)
                .map(ownerId -> ownerId.equals(principal.getAuthId()))
                .orElseThrow(() -> new RequestNotFoundException("Kontrol edilecek istek bulunamadı: " + requestId));
    }
}