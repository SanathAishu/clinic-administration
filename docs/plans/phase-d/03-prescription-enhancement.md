# Phase D Feature 3: Prescription Enhancement with Inventory Integration

**Status**: Ready for Implementation
**Priority**: MEDIUM
**Dependencies**: Feature 2 - Inventory Optimization (MUST complete first)
**Estimated Timeline**: 1 week
**Task Assignment**: Task Agent - Prescription Enhancement

---

## Executive Summary

Enhance prescription management with inventory integration and state machine workflow. This feature provides:

- ✅ Prescription state machine (PENDING → DISPENSED → COMPLETED)
- ✅ Prescription items with medication details (dosage, frequency, duration)
- ✅ Atomic inventory deduction on dispensing (ACID guarantees)
- ✅ Drug interaction checking (basic rule engine)
- ✅ Refill management with limits
- ✅ Quantity validation with invariants

Current Prescription service is a basic stub. This enhancement makes it production-ready with medical safety.

---

## Mathematical Foundation: Discrete Mathematics & ACID Transactions

### Prescription Lifecycle State Machine

**States**: PENDING, DISPENSED, COMPLETED, CANCELLED

**DAG Transitions** (Directed Acyclic Graph - no cycles):

```
PENDING ──dispense()──> DISPENSED ──complete()──> COMPLETED
   │                                                    ▲
   └──────────cancel()────────────────────────────────┘

CANCELLED ──(no outgoing transitions)
```

**Invariants**:
1. Can only transition forward (no backwards movements)
2. Terminal states (COMPLETED, CANCELLED) have no outgoing transitions
3. Status transitions are idempotent (same state = same result)
4. All state changes must have timestamp (dispensedAt, completedAt)

### Theorem 11: Quantity Invariant

**Statement**: For a prescription with medication details:

```
prescribedQuantity = dosage × frequency × duration

Invariants:
  - dispensedQuantity ≤ prescribedQuantity (cannot dispense more than prescribed)
  - remainingRefills ≥ 0 (non-negative refills)
  - totalDispensedAcrossFills ≤ prescribedQuantity × (1 + allowedRefills)
```

**Proof**:
Each prescription has fixed total allowed:
```
Total allowed = prescribedQuantity × (1 + allowedRefills)

For each fill:
  dispensedQuantity[n] > 0
  totalDispensed = Σ dispensedQuantity[n]

Invariant: totalDispensed ≤ Total allowed
```

### Theorem 12: Atomic Inventory Deduction

**Statement**: When dispensing prescription, inventory reduction is atomic - either both prescription is marked DISPENSED and inventory is reduced, or neither happens.

**Proof** (via @Transactional):

Spring @Transactional with default propagation (REQUIRED):

```
@Transactional
public Prescription dispensePrescription(Prescription rx) {
    try {
        // Step 1: Validate inventory sufficient
        if (inventory.currentStock < rx.prescribedQuantity)
            throw InsufficientStockException

        // Step 2: Reduce inventory (UPDATE inventory SET stock = ...)
        inventory.currentStock -= rx.prescribedQuantity

        // Step 3: Record transaction
        InventoryTransaction txn = new InventoryTransaction(
            type: SALE,
            quantity: rx.prescribedQuantity
        )

        // Step 4: Mark prescription dispensed
        rx.status = DISPENSED
        rx.dispensedAt = Instant.now()

        // Step 5: Persist (commit if success, rollback if exception)
        return prescriptionRepository.save(rx)
    }
    catch (Exception e) {
        // Database rolls back ALL changes (atomicity)
        throw e
    }
}
```

**Atomicity Guarantee**:
- If any step fails → rollback all changes → no partial state
- Prescription not marked DISPENSED ↔ Inventory not reduced
- Either both happen or both don't (ACID property)

---

## Implementation Details

### 1. Enhance Prescription Entity

**File**: `clinic-common/src/main/java/com/clinic/common/entity/clinical/Prescription.java`

Add state machine:

```java
@Entity
@Table(name = "prescriptions")
public class Prescription extends SoftDeletableEntity {

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private User doctor;

    // State Machine
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull
    @Builder.Default
    private PrescriptionStatus status = PrescriptionStatus.PENDING;

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

    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL)
    @NotEmpty(message = "Prescription must have at least one medication")
    private Set<PrescriptionItem> items = new HashSet<>();

    // State Transition Methods
    public void markAsDispensed(User dispensedBy) {
        validateStatusTransition(status, PrescriptionStatus.DISPENSED);
        this.status = PrescriptionStatus.DISPENSED;
        this.dispensedAt = Instant.now();
        this.timesFilled++;
    }

    public void markAsCompleted() {
        validateStatusTransition(status, PrescriptionStatus.COMPLETED);
        this.status = PrescriptionStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void cancel(User cancelledBy) {
        validateStatusTransition(status, PrescriptionStatus.CANCELLED);
        this.status = PrescriptionStatus.CANCELLED;
        this.cancelledAt = Instant.now();
    }

    /**
     * Validate state transition (DAG enforcement)
     */
    private void validateStatusTransition(PrescriptionStatus from, PrescriptionStatus to) {
        boolean valid = switch (from) {
            case PENDING -> to == PrescriptionStatus.DISPENSED || to == PrescriptionStatus.CANCELLED;
            case DISPENSED -> to == PrescriptionStatus.COMPLETED;
            case COMPLETED, CANCELLED -> false;  // Terminal states
        };

        if (!valid) {
            throw new IllegalStateException(
                String.format("Invalid transition: %s → %s", from, to)
            );
        }
    }

    /**
     * Invariant validation (Discrete Math principles)
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
        if (dispensedAt != null && dispensedAt.isBefore(createdAt)) {
            throw new IllegalStateException(
                "Invariant violation: dispensedAt cannot be before createdAt"
            );
        }

        if (dispensedAt != null && completedAt != null && completedAt.isBefore(dispensedAt)) {
            throw new IllegalStateException(
                "Invariant violation: completedAt cannot be before dispensedAt"
            );
        }

        // Invariant 5: Refill constraints
        if (timesFilled > (allowedRefills + 1)) {
            throw new IllegalStateException(
                String.format("Invariant violation: timesFilled (%d) > allowedRefills + 1 (%d)",
                    timesFilled, allowedRefills + 1)
            );
        }

        // Invariant 6: Cannot have items
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException(
                "Invariant violation: Prescription must have at least one medication"
            );
        }
    }

    public boolean canBeRefilled() {
        return status == PrescriptionStatus.COMPLETED && timesFilled < (allowedRefills + 1);
    }
}

enum PrescriptionStatus {
    PENDING,     // Initial state
    DISPENSED,   // Medication dispensed to patient
    COMPLETED,   // Patient finished medication
    CANCELLED    // Prescription cancelled
}
```

### 2. Create PrescriptionItem Entity

**File**: `clinic-common/src/main/java/com/clinic/common/entity/clinical/PrescriptionItem.java`

```java
@Entity
@Table(name = "prescription_items")
public class PrescriptionItem extends TenantAwareEntity {

    @ManyToOne
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @ManyToOne
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory medication;

    // Dosage Information
    @Column(name = "dosage", nullable = false)
    @NotNull
    private Double dosage;  // e.g., 500 (mg)

    @Column(name = "dosage_unit", nullable = false)
    @NotBlank
    private String dosageUnit;  // mg, ml, tablet, capsule

    @Column(name = "frequency_per_day", nullable = false)
    @NotNull
    @Min(value = 1)
    private Integer frequencyPerDay;  // times per day

    @Column(name = "duration_days", nullable = false)
    @NotNull
    @Min(value = 1)
    private Integer durationDays;  // days to take

    @Column(name = "prescribed_quantity", nullable = false)
    @NotNull
    @Min(value = 1)
    private Integer prescribedQuantity;  // Total: dosage × frequency × duration

    @Column(name = "dispensed_quantity")
    @Min(value = 0)
    private Integer dispensedQuantity = 0;

    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;  // e.g., "with meals", "before bed"

    /**
     * Calculate total quantity to be prescribed
     * prescribedQuantity = dosage × frequencyPerDay × durationDays
     *
     * Theorem 11: Quantity Invariant
     */
    @PrePersist
    protected void calculatePrescribedQuantity() {
        this.prescribedQuantity = (int) Math.ceil(
            dosage * frequencyPerDay * durationDays
        );
    }

    @PrePersist
    @PreUpdate
    protected void validateInvariants() {
        // Invariant: dispensed ≤ prescribed (Theorem 11)
        if (dispensedQuantity != null && dispensedQuantity > prescribedQuantity) {
            throw new IllegalStateException(
                String.format(
                    "Invariant violation: dispensedQuantity (%d) > prescribedQuantity (%d)",
                    dispensedQuantity, prescribedQuantity
                )
            );
        }
    }
}
```

### 3. Create DrugInteraction Entity

**File**: `clinic-common/src/main/java/com/clinic/common/entity/clinical/DrugInteraction.java`

```java
@Entity
@Table(name = "drug_interactions")
public class DrugInteraction extends TenantAwareEntity {

    @ManyToOne
    @JoinColumn(name = "medication_a_id")
    private Inventory medicationA;

    @ManyToOne
    @JoinColumn(name = "medication_b_id")
    private Inventory medicationB;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity")
    private InteractionSeverity severity;  // MINOR, MODERATE, SEVERE

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "recommendation", columnDefinition = "TEXT")
    private String recommendation;

    enum InteractionSeverity {
        MINOR,      // Monitor patient
        MODERATE,   // Dosage adjustment may be needed
        SEVERE      // Avoid combination or use with caution
    }
}
```

### 4. Create PrescriptionService Implementation

**File**: `clinic-backend/src/main/java/com/clinic/backend/service/PrescriptionService.java`

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final InventoryService inventoryService;
    private final InventoryTransactionService inventoryTransactionService;
    private final DrugInteractionRepository drugInteractionRepository;

    /**
     * Dispense prescription with atomic inventory deduction.
     *
     * Theorem 12: Atomic Inventory Deduction
     * Either both prescription is marked DISPENSED AND inventory is reduced,
     * or neither happens. @Transactional guarantees atomicity.
     *
     * @param prescriptionId Prescription to dispense
     * @param tenantId Tenant context
     * @param dispensedBy User performing the dispensing
     * @return Updated prescription
     * @throws InsufficientStockException If inventory insufficient
     */
    @Transactional
    public Prescription dispensePrescription(
            UUID prescriptionId,
            UUID tenantId,
            User dispensedBy) {

        // Step 1: Fetch prescription
        Prescription prescription = prescriptionRepository
            .findByIdAndTenantIdAndDeletedAtIsNull(prescriptionId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Prescription not found"));

        // Step 2: Validate state
        if (prescription.getStatus() != PrescriptionStatus.PENDING) {
            throw new IllegalStateException(
                "Cannot dispense prescription in status: " + prescription.getStatus()
            );
        }

        // Step 3: Validate no refills exceeded
        if (prescription.getTimesFilled() > prescription.getAllowedRefills()) {
            throw new IllegalStateException(
                "Prescription refill limit exceeded"
            );
        }

        List<PrescriptionItem> items = prescription.getItems();

        // Step 4: Check ALL items have sufficient inventory (fail-fast)
        for (PrescriptionItem item : items) {
            Inventory inventory = inventoryService.getInventoryById(
                item.getInventoryId(), tenantId
            );

            if (inventory.getCurrentStock() < item.getPrescribedQuantity()) {
                throw new InsufficientStockException(
                    String.format(
                        "Insufficient stock for %s: need %d, have %d",
                        inventory.getItemName(),
                        item.getPrescribedQuantity(),
                        inventory.getCurrentStock()
                    )
                );
            }
        }

        // Step 5: Check drug interactions
        checkDrugInteractions(items, prescription.getPatient());

        // Step 6: Deduct inventory for each item (atomic transaction)
        for (PrescriptionItem item : items) {
            // Record inventory transaction (SALE)
            inventoryTransactionService.recordTransaction(
                item.getInventoryId(),
                tenantId,
                TransactionType.SALE,
                item.getPrescribedQuantity(),
                "Prescription dispensed: " + prescriptionId,
                dispensedBy
            );

            item.setDispensedQuantity(item.getPrescribedQuantity());
        }

        // Step 7: Mark prescription as dispensed
        prescription.markAsDispensed(dispensedBy);

        // Step 8: Persist (commit if all success, rollback if any failure)
        return prescriptionRepository.save(prescription);

        // If any step 4-7 fails → @Transactional rolls back ALL changes
        // ⟹ Prescription NOT marked DISPENSED and inventory NOT reduced
        // ✓ Atomic guarantee maintained
    }

    /**
     * Check for drug interactions in prescription
     *
     * @param items Medications in prescription
     * @param patient Patient receiving medications
     * @throws DrugInteractionException If severe interaction detected
     */
    private void checkDrugInteractions(List<PrescriptionItem> items, Patient patient) {
        // Check combinations of items
        for (int i = 0; i < items.size(); i++) {
            for (int j = i + 1; j < items.size(); j++) {
                UUID medA = items.get(i).getInventoryId();
                UUID medB = items.get(j).getInventoryId();

                Optional<DrugInteraction> interaction = drugInteractionRepository
                    .findInteraction(medA, medB);

                if (interaction.isPresent()) {
                    DrugInteraction di = interaction.get();

                    if (di.getSeverity() == InteractionSeverity.SEVERE) {
                        throw new DrugInteractionException(
                            "Severe drug interaction: " + di.getDescription()
                        );
                    }

                    // Log warning for MODERATE interactions
                    if (di.getSeverity() == InteractionSeverity.MODERATE) {
                        log.warn("Moderate interaction for patient {}: {}",
                            patient.getId(), di.getDescription());
                    }
                }
            }
        }
    }

    /**
     * Complete prescription (when patient finished medication)
     */
    @Transactional
    public Prescription completePrescription(UUID prescriptionId, UUID tenantId) {
        Prescription prescription = prescriptionRepository
            .findByIdAndTenantIdAndDeletedAtIsNull(prescriptionId, tenantId)
            .orElseThrow();

        prescription.markAsCompleted();
        return prescriptionRepository.save(prescription);
    }

    /**
     * Cancel prescription
     */
    @Transactional
    public Prescription cancelPrescription(
            UUID prescriptionId,
            UUID tenantId,
            User cancelledBy) {

        Prescription prescription = prescriptionRepository
            .findByIdAndTenantIdAndDeletedAtIsNull(prescriptionId, tenantId)
            .orElseThrow();

        prescription.cancel(cancelledBy);
        return prescriptionRepository.save(prescription);
    }

    /**
     * Refill prescription (if allowed)
     */
    @Transactional
    public Prescription refillPrescription(UUID prescriptionId, UUID tenantId) {
        Prescription prescription = prescriptionRepository
            .findByIdAndTenantIdAndDeletedAtIsNull(prescriptionId, tenantId)
            .orElseThrow();

        if (!prescription.canBeRefilled()) {
            throw new IllegalStateException(
                "Prescription cannot be refilled"
            );
        }

        // Create new prescription as copy
        Prescription newRx = Prescription.builder()
            .patient(prescription.getPatient())
            .doctor(prescription.getDoctor())
            .items(new HashSet<>(prescription.getItems()))
            .allowedRefills(prescription.getAllowedRefills())
            .status(PrescriptionStatus.PENDING)
            .build();

        return prescriptionRepository.save(newRx);
    }
}
```

### 5. Database Migration: V17__enhance_prescriptions.sql

```sql
-- Enhance prescriptions table with state machine
ALTER TABLE prescriptions
ADD COLUMN status VARCHAR(20) DEFAULT 'PENDING' NOT NULL,
ADD COLUMN dispensed_at TIMESTAMPTZ,
ADD COLUMN completed_at TIMESTAMPTZ,
ADD COLUMN cancelled_at TIMESTAMPTZ,
ADD COLUMN allowed_refills INTEGER DEFAULT 0,
ADD COLUMN times_filled INTEGER DEFAULT 0;

-- Create prescription_items table
CREATE TABLE prescription_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    prescription_id UUID NOT NULL REFERENCES prescriptions(id) ON DELETE CASCADE,
    inventory_id UUID NOT NULL REFERENCES inventory(id),

    -- Dosage info
    dosage NUMERIC(10,2) NOT NULL CHECK (dosage > 0),
    dosage_unit VARCHAR(50) NOT NULL,
    frequency_per_day INTEGER NOT NULL CHECK (frequency_per_day > 0),
    duration_days INTEGER NOT NULL CHECK (duration_days > 0),

    -- Quantities
    prescribed_quantity INTEGER NOT NULL CHECK (prescribed_quantity > 0),
    dispensed_quantity INTEGER DEFAULT 0 CHECK (dispensed_quantity >= 0),

    -- Additional info
    instructions TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,

    -- Invariants
    CONSTRAINT item_quantity_check CHECK (dispensed_quantity <= prescribed_quantity)
);

-- Create drug_interactions table
CREATE TABLE drug_interactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    medication_a_id UUID NOT NULL REFERENCES inventory(id),
    medication_b_id UUID NOT NULL REFERENCES inventory(id),
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('MINOR', 'MODERATE', 'SEVERE')),
    description TEXT,
    recommendation TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_interaction UNIQUE (medication_a_id, medication_b_id)
);

-- Indexes
CREATE INDEX idx_prescription_items_prescription ON prescription_items(prescription_id);
CREATE INDEX idx_prescription_items_inventory ON prescription_items(inventory_id);
CREATE INDEX idx_drug_interactions_medications ON drug_interactions(medication_a_id, medication_b_id);
CREATE INDEX idx_prescriptions_status ON prescriptions(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_prescriptions_patient_status ON prescriptions(patient_id, status) WHERE deleted_at IS NULL;

-- Constraints
ALTER TABLE prescriptions
ADD CONSTRAINT rx_temporal_order CHECK (
    (dispensed_at IS NULL OR dispensed_at >= created_at) AND
    (completed_at IS NULL OR completed_at >= dispensed_at OR dispensed_at IS NULL) AND
    (cancelled_at IS NULL OR cancelled_at >= created_at)
),
ADD CONSTRAINT rx_refill_constraint CHECK (times_filled <= (allowed_refills + 1));
```

### 6. REST API Endpoints

**File**: `clinic-backend/src/main/java/com/clinic/backend/controller/PrescriptionController.java`

Endpoints:
- `GET /api/prescriptions/{id}` - Get prescription details
- `POST /api/prescriptions` - Create new prescription
- `POST /api/prescriptions/{id}/dispense` - Dispense with inventory deduction
- `POST /api/prescriptions/{id}/complete` - Mark as completed
- `POST /api/prescriptions/{id}/cancel` - Cancel prescription
- `POST /api/prescriptions/{id}/refill` - Refill if allowed
- `GET /api/prescriptions/{id}/interactions` - Check drug interactions

---

## Verification Checklist

### State Machine Correctness
- [ ] Token numbers are strictly monotonic per doctor per day
- [ ] Valid transitions only (PENDING→DISPENSED, DISPENSED→COMPLETED, others→CANCELLED)
- [ ] Cannot transition backwards (no COMPLETED→PENDING)
- [ ] All state changes have timestamps (dispensedAt, completedAt, cancelledAt)
- [ ] Invariants enforced: temporal ordering, timestamp requirements

### Inventory Integration
- [ ] Dispensing atomically reduces inventory (test rollback on failure)
- [ ] Invariant: dispensedQuantity ≤ prescribedQuantity maintained
- [ ] Inventory transaction (SALE) created for audit trail
- [ ] Sufficient stock checked before dispensing (fail-fast)
- [ ] Multi-item prescriptions: all-or-nothing (atomic for all items)

### Drug Interactions
- [ ] Interactions checked before dispensing
- [ ] Severe interactions block dispensing
- [ ] Moderate interactions log warnings
- [ ] Minor interactions logged
- [ ] New interactions can be added to database

### Refill Management
- [ ] Refill counter increments on each dispense
- [ ] Cannot exceed allowedRefills + 1
- [ ] Refill only works from COMPLETED status
- [ ] New prescription created as copy on refill

### Quantity Calculations
- [ ] prescribedQuantity = dosage × frequency × duration
- [ ] Formula matches Theorem 11
- [ ] All units consistent (mg, ml, tablets, capsules)

### Multi-Tenancy & Security
- [ ] All queries filtered by tenant_id
- [ ] Cannot access other tenant's prescriptions
- [ ] Audit log: who dispensed, when, what quantity
- [ ] Sensitive data not exposed in errors

---

## Edge Cases to Test

1. **Insufficient Stock**: Try to dispense when inventory < prescribed_quantity → InsufficientStockException
2. **Concurrent Dispensing**: Two users try to dispense same prescription → Second fails (status check)
3. **Refill After Expiry**: Check prescription expiry before refill
4. **Drug Interaction**: Try to dispense conflicting medications → Warning or error
5. **Partial Inventory**: Stock sufficient for item A but not item B → Entire dispensing fails (atomicity)
6. **Status Transitions**: Try invalid transitions (COMPLETED→PENDING) → IllegalStateException
7. **Missing Items**: Prescription with no items → ValidationException
8. **Zero Dosage**: Create item with dosage = 0 → ValidationException

---

## Success Metrics

After deployment, measure:

| Metric | Target | Validation |
|--------|--------|-----------|
| Prescription processing time | <2 minutes | End-to-end timing |
| Inventory discrepancy | <1% | Audit trail matches stock |
| Dispensing accuracy | 100% | Count errors |
| Refill success rate | >95% | Successful refills / attempts |
| Drug interaction alerts | 100% | All interactions caught |
| State machine violations | 0 | Invariant checks pass |

---

## Integration with Previous Features

**Depends On**:
- Feature 2: Inventory Optimization (MUST complete first)
  - Requires: InventoryService, InventoryTransactionService, Inventory entity
  - Uses: inventoryService.getInventoryById(), inventoryTransactionService.recordTransaction()

**Used By**:
- Patient portal (view prescriptions, request refills)
- Pharmacy management (dispense medications)
- Audit system (track prescribing patterns)

---

## Implementation Order

1. **Day 1**: Enhance Prescription entity with state machine
2. **Day 2**: Create PrescriptionItem and DrugInteraction entities
3. **Day 3**: Implement PrescriptionService with dispensing logic
4. **Day 4**: Create database migration V17
5. **Day 5**: Create REST API endpoints and DTOs

---

## Rollback Plan

If issues arise:

1. Stop calling dispensePrescription in API
2. Disable refill endpoints
3. Revert to basic prescription management (no inventory integration)
4. Rollback migration V17 (drop new columns/tables)
5. Drop PrescriptionItem and DrugInteraction entities

Prescriptions remain in database (no data loss). Basic CRUD still works.

---

## References

- CLAUDE.md: Discrete Mathematics - State Machines (lines 294-332)
- CLAUDE.md: Discrete Mathematics - Invariants (lines 462-520)
- Existing: MedicalOrder.java (8-state machine reference)
- Existing: Appointment.java (6-state machine reference)
- Feature 2: InventoryOptimizationService.java

---

**Previous Feature**: Inventory Optimization (02-inventory-optimization.md)
**Parent**: Phase D Main Plan (docs/plans/phase-d/)

---

**IMPORTANT**: This feature requires Feature 2 (Inventory Optimization) to be completed first. Do NOT start until Inventory Optimization tables and services are deployed.
