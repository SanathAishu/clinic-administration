package com.clinic.backend.entity;

import com.clinic.common.entity.SoftDeletableEntity;
import com.clinic.common.enums.BloodGroup;
import com.clinic.common.enums.Gender;
import com.clinic.common.enums.MaritalStatus;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "patients", indexes = {
    @Index(name = "idx_patients_tenant", columnList = "tenant_id"),
    @Index(name = "idx_patients_phone", columnList = "phone"),
    @Index(name = "idx_patients_abha", columnList = "abha_id"),
    @Index(name = "idx_patients_name", columnList = "tenant_id, last_name, first_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient extends SoftDeletableEntity {

    // Demographics
    @Column(name = "first_name", nullable = false, length = 100)
    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @Column(name = "middle_name", length = 100)
    @Size(max = 100)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 100)
    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    @NotNull(message = "Gender is required")
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group")
    private BloodGroup bloodGroup;

    // Contact Information
    @Column(name = "email")
    @Email(message = "Email must be valid")
    @Size(max = 255)
    private String email;

    @Column(name = "phone", nullable = false, length = 15)
    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$", message = "Phone number must be valid")
    private String phone;

    @Column(name = "alternate_phone", length = 15)
    @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$", message = "Phone number must be valid")
    private String alternatePhone;

    // Address
    @Column(name = "address_line1", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Address line 1 is required")
    private String addressLine1;

    @Column(name = "address_line2", columnDefinition = "TEXT")
    private String addressLine2;

    @Column(name = "city", nullable = false, length = 100)
    @NotBlank(message = "City is required")
    @Size(max = 100)
    private String city;

    @Column(name = "state", nullable = false, length = 100)
    @NotBlank(message = "State is required")
    @Size(max = 100)
    private String state;

    @Column(name = "pincode", nullable = false, length = 10)
    @NotBlank(message = "Pincode is required")
    @Size(max = 10)
    private String pincode;

    // ABHA Integration
    @Column(name = "abha_id", unique = true, length = 14)
    @Pattern(regexp = "^\\d{14}$", message = "ABHA ID must be 14 digits")
    private String abhaId;

    @Column(name = "abha_number", unique = true, length = 17)
    @Size(max = 17)
    private String abhaNumber;

    // Additional Information
    @Enumerated(EnumType.STRING)
    @Column(name = "marital_status")
    private MaritalStatus maritalStatus;

    @Column(name = "occupation", length = 100)
    @Size(max = 100)
    private String occupation;

    @Column(name = "emergency_contact_name", length = 200)
    @Size(max = 200)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 15)
    @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$", message = "Phone number must be valid")
    private String emergencyContactPhone;

    @Column(name = "emergency_contact_relation", length = 50)
    @Size(max = 50)
    private String emergencyContactRelation;

    // Medical Information
    @Type(StringArrayType.class)
    @Column(name = "allergies", columnDefinition = "text[]")
    private String[] allergies;

    @Type(StringArrayType.class)
    @Column(name = "chronic_conditions", columnDefinition = "text[]")
    private String[] chronicConditions;

    // Metadata
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @NotNull
    private User createdBy;

    public String getFullName() {
        if (middleName != null && !middleName.isEmpty()) {
            return firstName + " " + middleName + " " + lastName;
        }
        return firstName + " " + lastName;
    }

    public int getAge() {
        return LocalDate.now().getYear() - dateOfBirth.getYear();
    }
}
