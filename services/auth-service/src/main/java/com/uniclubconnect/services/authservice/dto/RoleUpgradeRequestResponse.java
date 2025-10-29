package com.uniclubconnect.services.authservice.dto;

import com.uniclubconnect.services.authservice.entity.ERoleRequestStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RoleUpgradeRequestResponse {
    private Long id;
    private String requestingUserEmail; // Kullanıcının e-postası
    private ERoleRequestStatus status;
    private LocalDateTime requestDate;
    private LocalDateTime resolutionDate; // Onay/Red tarihi (varsa)
    private String reviewedByAdminEmail; // İnceleyen adminin e-postası (varsa)
}