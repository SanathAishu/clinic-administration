package com.clinic.common.dto.response;

import com.clinic.common.enums.TenantStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantResponseDTO {

    private UUID id;
    private String subdomain;
    private String name;
    private String email;
    private String phone;
    private String address;
    private TenantStatus status;
    private LocalDate subscriptionStartDate;
    private LocalDate subscriptionEndDate;
    private Integer maxUsers;
    private Integer maxPatients;
    private Instant createdAt;
    private Instant updatedAt;
}
