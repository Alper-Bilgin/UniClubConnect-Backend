package com.uniclubconnect.services.clubservice.dto;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ClubMemberResponse {
    private Long id;
    private String userAuthId;
    private LocalDateTime joinDate;
    // (Gelecekte user-profile-service'ten kullanıcının adını/resmini de çekebiliriz)
}