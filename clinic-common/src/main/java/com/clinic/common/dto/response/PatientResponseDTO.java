package com.clinic.common.dto.response;

import com.clinic.common.enums.BloodGroup;
import com.clinic.common.enums.Gender;
import com.clinic.common.enums.MaritalStatus;
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
public class PatientResponseDTO {

    private UUID id;

    // Demographics
    private String firstName;
    private String middleName;
    private String lastName;
    private String fullName;
    private LocalDate dateOfBirth;
    private Integer age;
    private Gender gender;
    private BloodGroup bloodGroup;

    // Contact Information
    private String email;
    private String phone;
    private String alternatePhone;

    // Address
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String pincode;

    // ABHA Integration
    private String abhaId;
    private String abhaNumber;

    // Additional Information
    private MaritalStatus maritalStatus;
    private String occupation;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelation;

    // Medical Information
    private String[] allergies;
    private String[] chronicConditions;

    // Metadata
    private UUID createdById;
    private Instant createdAt;
    private Instant updatedAt;
}
