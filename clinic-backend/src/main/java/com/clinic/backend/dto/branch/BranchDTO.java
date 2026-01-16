package com.clinic.backend.dto.branch;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchDTO {

    private UUID id;

    private String branchCode;

    private String name;

    private String address;

    private String city;

    private String state;

    private String pincode;

    private String country;

    private String phone;

    private String email;

    private String description;

    private String operatingHours;

    private String facilities;

    private Integer maxPatientsPerDay;

    private Integer maxConcurrentAppointments;

    private Boolean isActive;

    private Boolean isMainBranch;

    private UUID createdById;

    private String createdByName;

    private Instant createdAt;

    private Instant updatedAt;

    private Instant deletedAt;
}
