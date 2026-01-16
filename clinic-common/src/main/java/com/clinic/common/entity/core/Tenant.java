package com.clinic.common.entity.core;

import com.clinic.common.entity.BaseEntity;
import com.clinic.common.enums.TenantStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "tenants", indexes = {
    @Index(name = "idx_tenants_subdomain", columnList = "subdomain"),
    @Index(name = "idx_tenants_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant extends BaseEntity {

    @Column(name = "name", nullable = false)
    @NotBlank(message = "Tenant name is required")
    @Size(max = 255)
    private String name;

    @Column(name = "subdomain", nullable = false, unique = true, length = 100)
    @NotBlank(message = "Subdomain is required")
    @Pattern(regexp = "^[a-z0-9-]{3,30}$", message = "Subdomain must be 3-30 characters, lowercase alphanumeric with hyphens")
    private String subdomain;

    @Column(name = "email", nullable = false)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255)
    private String email;

    @Column(name = "phone", length = 15)
    @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$", message = "Phone number must be valid")
    private String phone;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "city", length = 100)
    @Size(max = 100)
    private String city;

    @Column(name = "state", length = 100)
    @Size(max = 100)
    private String state;

    @Column(name = "pincode", length = 10)
    @Size(max = 10)
    private String pincode;

    @Column(name = "gstin", length = 15, unique = true)
    @Size(max = 15)
    private String gstin;

    @Column(name = "clinic_registration_number", length = 100)
    @Size(max = 100)
    private String clinicRegistrationNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull(message = "Status is required")
    private TenantStatus status = TenantStatus.TRIAL;

    @Column(name = "subscription_start_date", nullable = false)
    @NotNull(message = "Subscription start date is required")
    private LocalDate subscriptionStartDate = LocalDate.now();

    @Column(name = "subscription_end_date", nullable = false)
    @NotNull(message = "Subscription end date is required")
    private LocalDate subscriptionEndDate;

    @Column(name = "max_users", nullable = false)
    @Min(value = 1, message = "Max users must be at least 1")
    private Integer maxUsers = 10;

    @Column(name = "max_patients", nullable = false)
    @Min(value = 1, message = "Max patients must be at least 1")
    private Integer maxPatients = 500;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * Subscription Period Invariant (Discrete Math: Sequences & Recurrence)
     * Ensures subscription has positive duration
     */
    @PrePersist
    @PreUpdate
    protected void validateSubscriptionPeriod() {
        if (subscriptionEndDate != null) {
            if (subscriptionEndDate.isBefore(subscriptionStartDate) ||
                subscriptionEndDate.equals(subscriptionStartDate)) {
                throw new IllegalStateException(
                    String.format(
                        "Invariant violation: Subscription end date (%s) must be after start date (%s)",
                        subscriptionEndDate, subscriptionStartDate
                    )
                );
            }
        }
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
    }
}
