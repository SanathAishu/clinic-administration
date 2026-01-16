package com.clinic.common.entity.core;

import com.clinic.common.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Branch Entity
 * Represents physical locations/branches of a clinic organization (Tenant)
 * One tenant can have multiple branches
 */
@Entity
@Table(name = "branches", indexes = {
    @Index(name = "idx_branches_tenant", columnList = "tenant_id"),
    @Index(name = "idx_branches_code", columnList = "branch_code"),
    @Index(name = "idx_branches_active", columnList = "is_active")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_branches_tenant_code", columnNames = {"tenant_id", "branch_code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, insertable = false, updatable = false)
    private Tenant tenant;

    @Column(name = "branch_code", nullable = false, length = 50)
    @NotBlank(message = "Branch code is required")
    @Pattern(regexp = "^[A-Z0-9_-]{2,50}$",
             message = "Branch code must be 2-50 uppercase letters, numbers, underscores or hyphens")
    private String branchCode;

    @Column(name = "name", nullable = false, length = 255)
    @NotBlank(message = "Branch name is required")
    @Size(max = 255, message = "Branch name cannot exceed 255 characters")
    private String name;

    // Location Details
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "city", length = 100)
    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;

    @Column(name = "state", length = 100)
    @Size(max = 100, message = "State cannot exceed 100 characters")
    private String state;

    @Column(name = "pincode", length = 10)
    @Size(max = 10, message = "Pincode cannot exceed 10 characters")
    private String pincode;

    @Column(name = "country", length = 100)
    @Size(max = 100, message = "Country cannot exceed 100 characters")
    @Builder.Default
    private String country = "India";

    // Contact Information
    @Column(name = "phone", length = 15)
    @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$", message = "Phone number must be valid")
    private String phone;

    @Column(name = "email", length = 255)
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    private String email;

    // Operational Details
    @Column(name = "is_active", nullable = false)
    @NotNull
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_main_branch", nullable = false)
    @NotNull
    @Builder.Default
    private Boolean isMainBranch = false;

    // Operating Hours (optional, can be detailed later)
    @Column(name = "operating_hours", columnDefinition = "TEXT")
    private String operatingHours;

    // Additional Information
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "facilities", columnDefinition = "TEXT")
    private String facilities;

    // Capacity (Operations Research: Resource Constraints)
    @Column(name = "max_patients_per_day")
    @Min(value = 1, message = "Max patients per day must be at least 1")
    private Integer maxPatientsPerDay;

    @Column(name = "max_concurrent_appointments")
    @Min(value = 1, message = "Max concurrent appointments must be at least 1")
    private Integer maxConcurrentAppointments;

    // Metadata
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @NotNull
    private User createdBy;

    /**
     * Invariant validations (Discrete Math: Invariants)
     */
    @PrePersist
    @PreUpdate
    protected void validateInvariants() {
        // Invariant: Branch code must be uppercase
        if (branchCode != null) {
            branchCode = branchCode.toUpperCase();
        }

        // Invariant: At least one contact method required
        if ((phone == null || phone.trim().isEmpty()) &&
            (email == null || email.trim().isEmpty())) {
            throw new IllegalStateException(
                "Invariant violation: Branch must have at least phone or email"
            );
        }
    }
}
