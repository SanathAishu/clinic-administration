package com.clinic.common.dto.view;

import com.clinic.common.enums.BloodGroup;
import com.clinic.common.enums.Gender;
import com.clinic.common.enums.MaritalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO mapping to v_patient_detail database view.
 * Comprehensive patient profile with latest vitals and summary counts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientDetailViewDTO {

    // Basic Info
    private UUID id;
    private UUID tenantId;
    private String firstName;
    private String middleName;
    private String lastName;
    private String fullName;
    private LocalDate dateOfBirth;
    private Integer age;
    private Gender gender;

    // Contact Info
    private String phone;
    private String email;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String pincode;

    // Medical Info
    private BloodGroup bloodGroup;
    private String abhaId;
    private String abhaNumber;
    private MaritalStatus maritalStatus;
    private String occupation;

    // Emergency Contact
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelation;

    // Medical Arrays
    private String[] allergies;
    private String[] chronicConditions;

    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    // Latest Vital Signs
    private UUID latestVitalId;
    private Instant latestVitalRecordedAt;
    private BigDecimal temperatureCelsius;
    private Integer pulseBpm;
    private Integer systolicBp;
    private Integer diastolicBp;
    private Integer respiratoryRate;
    private Integer oxygenSaturation;
    private BigDecimal weightKg;
    private BigDecimal heightCm;
    private BigDecimal bmi;
    private String bloodPressure;

    // Summary Counts
    private Long totalAppointments;
    private Long totalMedicalRecords;
    private Long totalPrescriptions;
    private Long totalLabTests;
    private BigDecimal outstandingBalance;
}
