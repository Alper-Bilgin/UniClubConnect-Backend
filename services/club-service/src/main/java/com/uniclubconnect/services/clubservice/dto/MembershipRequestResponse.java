package com.uniclubconnect.services.clubservice.dto;
import com.uniclubconnect.services.clubservice.entity.ERequestStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class MembershipRequestResponse {
    private Long id;
    private Long clubId;
    private String userAuthId;
    private String userEmail;
    private ERequestStatus status;
    private LocalDateTime requestDate;
}