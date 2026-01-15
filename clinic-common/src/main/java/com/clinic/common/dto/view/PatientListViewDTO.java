package com.clinic.common.dto.view;

import com.clinic.common.enums.BloodGroup;
import com.clinic.common.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO mapping to v_patient_list database view.
 * Used for listing patients with computed fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientListViewDTO {

    private UUID id;
    private UUID tenantId;
    private String firstName;
    private String middleName;
    private String lastName;
    private String fullName;
    private LocalDate dateOfBirth;
    private Integer age;
    private Gender gender;
    private String phone;
    private String email;
    private BloodGroup bloodGroup;
    private String abhaId;
    private Instant createdAt;
    private Instant updatedAt;
    private String status;
    private Boolean isActive;
}
