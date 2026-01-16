package com.clinic.common.entity.clinical;

import com.clinic.common.entity.core.User;
import com.clinic.common.entity.SoftDeletableEntity;
import com.clinic.common.entity.patient.Patient;
import com.clinic.common.enums.PrescriptionStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "prescriptions", indexes = {
    @Index(name = "idx_prescriptions_tenant", columnList = "tenant_id"),
    @Index(name = "idx_prescriptions_patient", columnList = "patient_id, prescription_date"),
    @Index(name = "idx_prescriptions_medical_record", columnList = "medical_record_id"),
    @Index(name = "idx_prescriptions_status", columnList = "status"),
    @Index(name = "idx_prescriptions_patient_status", columnList = "patient_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prescription extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    @NotNull
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_record_id")
    private MedicalRecord medicalRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    @NotNull
    private User doctor;

    // Prescription Details
    @Column(name = "prescription_date", nullable = false)
    @NotNull
    private LocalDate prescriptionDate = LocalDate.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull
    @Builder.Default
    private PrescriptionStatus status = PrescriptionStatus.PENDING;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // State Machine: Timestamps for state transitions
    @Column(name = "dispensed_at")
    private Instant dispensedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    // Refill Management
    @Column(name = "allowed_refills")
    @Min(value = 0)
    @Builder.Default
    private Integer allowedRefills = 0;

    @Column(name = "times_filled")
    @Min(value = 0)
    @Builder.Default
    private Integer timesFilled = 0;

    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    @NotEmpty(message = "Prescription must have at least one medication")
    @Builder.Default
    private List<PrescriptionItem> items = new ArrayList<>();

    // Metadata
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @NotNull
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispensed_by")
    private User dispensedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private User cancelledBy;

    // Helper methods
    public void addItem(PrescriptionItem item) {
        items.add(item);
        item.setPrescription(this);
    }

    public void removeItem(PrescriptionItem item) {
        items.remove(item);
        item.setPrescription(null);
    }

    /**
     * Mark prescription as dispensed (PENDING → DISPENSED transition).
     *
     * Theorem 12: Atomic Inventory Deduction
     * This method is called within a @Transactional context to ensure atomicity.
     * Either both prescription is marked DISPENSED AND inventory is reduced, or neither happens.
     *
     * @param dispensedByUser User performing the dispensing
     * @throws IllegalStateException if transition is invalid
     */
    public void markAsDispensed(User dispensedByUser) {
        validateStatusTransition(status, PrescriptionStatus.DISPENSED);
        this.status = PrescriptionStatus.DISPENSED;
        this.dispensedAt = Instant.now();
        this.dispensedBy = dispensedByUser;
        this.timesFilled++;
    }

    /**
     * Mark prescription as completed (DISPENSED → COMPLETED transition).
     *
     * @throws IllegalStateException if transition is invalid
     */
    public void markAsCompleted() {
        validateStatusTransition(status, PrescriptionStatus.COMPLETED);
        this.status = PrescriptionStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    /**
     * Cancel prescription (PENDING → CANCELLED or DISPENSED → CANCELLED transition).
     *
     * @param cancelledByUser User performing the cancellation
     * @throws IllegalStateException if transition is invalid
     */
    public void cancel(User cancelledByUser) {
        validateStatusTransition(status, PrescriptionStatus.CANCELLED);
        this.status = PrescriptionStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.cancelledBy = cancelledByUser;
    }

    /**
     * Validate state transition using DAG (Directed Acyclic Graph) principles.
     *
     * Valid transitions (DAG):
     * PENDING ──dispense()──> DISPENSED ──complete()──> COMPLETED
     *    │                                                  ▲
     *    └──────────cancel()───────────────────────────────┘
     *
     * CANCELLED is terminal (no outgoing transitions)
     * COMPLETED is terminal (no outgoing transitions)
     *
     * @param from Current status
     * @param to Target status
     * @throws IllegalStateException if transition is not in the valid DAG
     */
    private void validateStatusTransition(PrescriptionStatus from, PrescriptionStatus to) {
        boolean valid = switch (from) {
            case PENDING -> to == PrescriptionStatus.DISPENSED || to == PrescriptionStatus.CANCELLED;
            case DISPENSED -> to == PrescriptionStatus.COMPLETED || to == PrescriptionStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;  // Terminal states - no outgoing transitions
        };

        if (!valid) {
            throw new IllegalStateException(
                String.format("Invalid transition: %s → %s", from, to)
            );
        }
    }

    /**
     * Check if prescription can be refilled.
     *
     * Theorem 11: Quantity Invariant
     * Can refill if:
     * 1. Status is COMPLETED (patient finished medication)
     * 2. timesFilled < (allowedRefills + 1) (refill limit not exceeded)
     *
     * @return true if prescription is eligible for refill
     */
    public boolean canBeRefilled() {
        return status == PrescriptionStatus.COMPLETED && timesFilled < (allowedRefills + 1);
    }

    /**
     * Invariant validation: Discrete Math Principles
     *
     * Invariants enforced:
     * 1. DISPENSED status requires dispensedAt timestamp
     * 2. COMPLETED status requires completedAt timestamp
     * 3. CANCELLED status requires cancelledAt timestamp
     * 4. Temporal ordering: created ≤ dispensed ≤ completed
     * 5. Refill constraints: timesFilled ≤ (allowedRefills + 1)
     * 6. Non-empty items list
     *
     * @throws IllegalStateException if any invariant is violated
     */
    @PrePersist
    @PreUpdate
    protected void validateInvariants() {
        // Invariant 1: DISPENSED requires dispensedAt
        if (status == PrescriptionStatus.DISPENSED && dispensedAt == null) {
            throw new IllegalStateException(
                "Invariant violation: DISPENSED status requires dispensedAt timestamp"
            );
        }

        // Invariant 2: COMPLETED requires completedAt
        if (status == PrescriptionStatus.COMPLETED && completedAt == null) {
            throw new IllegalStateException(
                "Invariant violation: COMPLETED status requires completedAt timestamp"
            );
        }

        // Invariant 3: CANCELLED requires cancelledAt
        if (status == PrescriptionStatus.CANCELLED && cancelledAt == null) {
            throw new IllegalStateException(
                "Invariant violation: CANCELLED status requires cancelledAt timestamp"
            );
        }

        // Invariant 4: Temporal ordering (created ≤ dispensed ≤ completed)
        if (dispensedAt != null && getCreatedAt() != null && dispensedAt.isBefore(getCreatedAt())) {
            throw new IllegalStateException(
                "Invariant violation: dispensedAt cannot be before createdAt"
            );
        }

        if (dispensedAt != null && completedAt != null && completedAt.isBefore(dispensedAt)) {
            throw new IllegalStateException(
                "Invariant violation: completedAt cannot be before dispensedAt"
            );
        }

        // Invariant 5: Refill constraints (timesFilled ≤ allowedRefills + 1)
        if (timesFilled > (allowedRefills + 1)) {
            throw new IllegalStateException(
                String.format("Invariant violation: timesFilled (%d) > allowedRefills + 1 (%d)",
                    timesFilled, allowedRefills + 1)
            );
        }

        // Invariant 6: Non-empty items list
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException(
                "Invariant violation: Prescription must have at least one medication"
            );
        }
    }
}
