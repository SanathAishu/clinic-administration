# Phase D Feature 3: Quick Reference Guide

## State Machine Overview

```
PENDING ──dispense()──> DISPENSED ──complete()──> COMPLETED
   │                                                  ▲
   └──────────cancel()───────────────────────────────┘
```

- **PENDING**: Initial state (just created)
- **DISPENSED**: Medication given to patient (inventory reduced)
- **COMPLETED**: Patient finished course (can be refilled if allowed)
- **CANCELLED**: Voided (terminal, no further action)

## Key Methods

### PrescriptionService.dispensePrescription()
**CRITICAL METHOD** - Implements Theorem 12: Atomic Inventory Deduction

```java
@Transactional
public Prescription dispensePrescription(UUID prescriptionId, UUID tenantId, User dispensedBy)
```

**What it does**:
1. Validates prescription is PENDING
2. Checks refill limit not exceeded
3. Checks all items have sufficient inventory (fail-fast)
4. Checks drug interactions (fail-fast)
5. Records inventory transaction for each item (SALE type)
6. Updates dispensedQuantity on items
7. Marks prescription DISPENSED
8. Atomically commits or rolls back all changes

**Exception handling**:
- `InsufficientStockException`: Not enough inventory
- `DrugInteractionException`: SEVERE interaction detected
- `IllegalStateException`: Invalid state or refill limit exceeded

**Guarantee**: Either all changes succeed or all rollback. No partial state.

### Other Key Methods

| Method | From State | To State | Notes |
|--------|-----------|----------|-------|
| `markAsDispensed(User)` | PENDING | DISPENSED | Sets dispensedAt, dispensedBy |
| `markAsCompleted()` | DISPENSED | COMPLETED | Sets completedAt |
| `cancel(User)` | PENDING/DISPENSED | CANCELLED | Sets cancelledAt, cancelledBy |
| `canBeRefilled()` | COMPLETED | (check) | Returns true if eligible for refill |
| `completePrescription()` | Service method | N/A | Calls markAsCompleted() |
| `cancelPrescription()` | Service method | N/A | Calls cancel() |
| `refillPrescription()` | Service method | PENDING (new) | Creates new prescription as copy |

## REST API Endpoints

### Dispense Prescription
```
POST /api/prescriptions/{id}/dispense
Content-Type: application/json

{
  "prescriptionId": "550e8400-e29b-41d4-a716-446655440000",
  "dispensedBy": "550e8400-e29b-41d4-a716-446655440001"
}

Response: PrescriptionDispensingResponse
Status: 200 OK (success), 409 CONFLICT (insufficient stock/interaction), 400 BAD REQUEST (invalid state)
```

### Complete Prescription
```
POST /api/prescriptions/{id}/complete?tenantId=...
Response: Prescription
Status: 200 OK or 400 BAD REQUEST
```

### Cancel Prescription
```
POST /api/prescriptions/{id}/cancel?tenantId=...&cancelledBy=...
Response: Prescription
Status: 200 OK or 400 BAD REQUEST
```

### Refill Prescription
```
POST /api/prescriptions/{id}/refill?tenantId=...&refillRequestedBy=...
Response: Prescription (new, in PENDING state)
Status: 201 CREATED or 400 BAD REQUEST
```

### Get Prescription
```
GET /api/prescriptions/{id}?tenantId=...
Response: Prescription with all relationships
Status: 200 OK or 404 NOT FOUND
```

### Get by Status
```
GET /api/prescriptions/status/{status}?tenantId=...
Response: List<Prescription>
Status: 200 OK
```

## Database Schema

### prescriptions Table
```sql
CREATE TABLE prescriptions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    patient_id UUID NOT NULL,
    doctor_id UUID NOT NULL,
    medical_record_id UUID,

    -- State Machine
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    dispensed_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,

    -- Refill Management
    allowed_refills INTEGER NOT NULL DEFAULT 0,
    times_filled INTEGER NOT NULL DEFAULT 0,

    -- Tracking
    dispensed_by UUID REFERENCES users(id),
    cancelled_by UUID REFERENCES users(id),

    -- Metadata
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,

    -- Constraints
    CHECK (times_filled <= allowed_refills + 1),
    CHECK (dispensed_at >= created_at),
    CHECK (completed_at >= dispensed_at OR dispensed_at IS NULL)
);
```

### prescription_items Table
```sql
CREATE TABLE prescription_items (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    prescription_id UUID NOT NULL REFERENCES prescriptions(id) ON DELETE CASCADE,
    inventory_id UUID NOT NULL REFERENCES inventory(id),

    -- Dosage
    dosage NUMERIC(10, 2) NOT NULL,
    dosage_unit VARCHAR(50) NOT NULL,
    frequency_per_day INTEGER NOT NULL,
    duration_days INTEGER NOT NULL,

    -- Quantities
    prescribed_quantity INTEGER NOT NULL,  -- = dosage × frequency × duration
    dispensed_quantity INTEGER DEFAULT 0,

    -- Instructions
    instructions TEXT,

    -- Metadata
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,

    -- Constraints
    CHECK (dispensed_quantity <= prescribed_quantity),
    CHECK (dosage > 0),
    CHECK (frequency_per_day > 0),
    CHECK (duration_days > 0)
);
```

### drug_interactions Table
```sql
CREATE TABLE drug_interactions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    medication_a_id UUID NOT NULL REFERENCES inventory(id),
    medication_b_id UUID NOT NULL REFERENCES inventory(id),

    severity VARCHAR(20) NOT NULL,  -- MINOR, MODERATE, SEVERE
    description TEXT NOT NULL,
    recommendation TEXT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,

    UNIQUE (tenant_id, medication_a_id, medication_b_id)
);
```

## Quantity Calculations (Theorem 11)

```
prescribedQuantity = dosage × frequencyPerDay × durationDays

Example:
- Medication: Amoxicillin 500mg
- Frequency: 2 times per day
- Duration: 10 days
- Prescribed Quantity = 500 × 2 × 10 = 10,000 units

Invariant:
- dispensedQuantity ≤ prescribedQuantity
- Cannot dispense more than prescribed
```

## Drug Interactions

### Severity Levels

| Severity | Meaning | Dispensing | Logging |
|----------|---------|-----------|---------|
| **MINOR** | Monitor patient | ✅ Allowed | ℹ️ Info level |
| **MODERATE** | Dose adjustment possible | ✅ Allowed | ⚠️ Warning level |
| **SEVERE** | Avoid or use with caution | ❌ Blocked | 🛑 Error level |

### Example
```java
// Creating a severe interaction
DrugInteraction severe = DrugInteraction.builder()
    .medicationA(warfarin)
    .medicationB(aspirin)
    .severity(InteractionSeverity.SEVERE)
    .description("May increase risk of bleeding")
    .recommendation("Contraindicated - do not combine. Consider alternative antiplatelet agent")
    .tenantId(tenantId)
    .build();
```

## Refill Management

### Logic
```java
public boolean canBeRefilled() {
    return status == PrescriptionStatus.COMPLETED
        && timesFilled < (allowedRefills + 1);
}
```

### Example
```
prescription.allowedRefills = 2
prescription.status = COMPLETED

Fills allowed:
- Initial fill: times_filled = 1 (from dispensing)
- Refill #1: times_filled = 2 (from refilling and dispensing)
- Refill #2: times_filled = 3 (from refilling and dispensing)
- Total: 3 fills (1 + 2 allowed refills)

canBeRefilled() returns:
- After fill 1: true (1 < 3)
- After fill 2: true (2 < 3)
- After fill 3: false (3 not < 3)
```

## Invariants (Enforced)

### At Entity Level (@PrePersist/@PreUpdate)
1. DISPENSED requires dispensedAt timestamp
2. COMPLETED requires completedAt timestamp
3. CANCELLED requires cancelledAt timestamp
4. Temporal ordering: created ≤ dispensed ≤ completed
5. Refill constraints: timesFilled ≤ (allowedRefills + 1)
6. Non-empty items list
7. dispensedQuantity ≤ prescribedQuantity
8. Dosage, frequency, duration all > 0

### At Database Level (CHECK constraints)
- times_filled <= (allowed_refills + 1)
- dispensed_at >= created_at
- completed_at >= dispensed_at (if dispensed_at not null)
- cancelled_at >= created_at
- dispensed_quantity <= prescribed_quantity
- dosage > 0, frequency_per_day > 0, duration_days > 0

## Error Codes & Status

| HTTP Status | Meaning | Example |
|------------|---------|---------|
| 200 OK | Success | Dispensing completed |
| 201 CREATED | Created | Refill created |
| 400 BAD REQUEST | Invalid state or input | Try to dispense non-PENDING |
| 404 NOT FOUND | Resource not found | Prescription doesn't exist |
| 409 CONFLICT | Business rule violation | Insufficient stock, SEVERE interaction |
| 500 INTERNAL ERROR | Unexpected error | Database error |

## Testing Checklist

### Basic Flow
- [ ] Create prescription in PENDING state
- [ ] Dispense prescription (becomes DISPENSED, inventory reduced)
- [ ] Complete prescription (becomes COMPLETED)
- [ ] Refill prescription (new PENDING prescription created)

### State Transitions
- [ ] Cannot complete PENDING prescription (must be DISPENSED first)
- [ ] Cannot dispense already-dispensed prescription
- [ ] Cannot dispense cancelled prescription
- [ ] Cannot refill non-completed prescription

### Inventory Integrity
- [ ] Inventory correctly reduced on dispensing
- [ ] Cannot dispense if insufficient stock
- [ ] Inventory transaction created for audit
- [ ] All items reduced atomically (all or nothing)

### Drug Interactions
- [ ] SEVERE interaction blocks dispensing
- [ ] MODERATE interaction logs warning but allows dispensing
- [ ] MINOR interaction logs info
- [ ] Bidirectional lookup works

### Refill Limits
- [ ] Can refill up to allowedRefills times
- [ ] Cannot exceed limit
- [ ] New refill starts at times_filled = 0

## Files Reference

### Entity Classes
- `Prescription.java`: State machine, timestamps, refills
- `PrescriptionItem.java`: Dosage, quantities, inventory reference
- `DrugInteraction.java`: Medication interactions with severity

### Services
- `PrescriptionService.java`: All prescription operations
- `DrugInteractionService.java`: Interaction checking
- `InventoryService.java`: Stock management
- `InventoryTransactionService.java`: Audit trail

### Repositories
- `PrescriptionRepository.java`: Prescription queries
- `DrugInteractionRepository.java`: Interaction queries
- `InventoryRepository.java`: Inventory queries
- `InventoryTransactionRepository.java`: Transaction history

### REST Controllers
- `PrescriptionEnhancedController.java`: API endpoints

### DTOs
- `DispensePrescriptionRequest.java`: Dispense request
- `PrescriptionDispensingResponse.java`: Dispense response

### Database
- `V16__enhance_prescriptions_with_inventory_integration.sql`: Migration

## Troubleshooting

### "Cannot dispense prescription in status: ACTIVE"
**Cause**: Old code using ACTIVE instead of PENDING
**Fix**: Update enum value or migration

### "Invariant violation: dispensedQuantity > prescribedQuantity"
**Cause**: Attempting to dispense more than prescribed
**Fix**: Check PrescriptionItem quantity calculation

### "Insufficient stock" error on dispense
**Cause**: Inventory insufficient for one or more items
**Fix**: Check inventory levels, add stock if needed

### "Severe drug interaction detected"
**Cause**: Medications have known severe interaction
**Fix**: Prescriber must change medication or approve override

### Concurrent dispensing fails on second attempt
**Expected behavior**: Only first user succeeds, second gets "Cannot dispense prescription in status: DISPENSED"
**Why**: State machine prevents duplicate dispensing

## Performance Considerations

- **Indexes**: Created on status, patient_id, doctor_id, inventory_id
- **Query optimization**: Use pagination for large result sets
- **Transaction scope**: Keep @Transactional methods tight (only necessary operations)
- **Drug interaction checking**: O(n²) for n medications, acceptable for typical prescriptions (5-10 items)

## Security Considerations

- **Tenant isolation**: All queries filtered by tenant_id
- **User tracking**: dispensedBy, cancelledBy logged for audit
- **Audit trail**: InventoryTransaction records what, when, by whom
- **Validation**: Input validation via @NotNull, @Min, etc.
- **Authorization**: Implement @PreAuthorize on endpoints

## Production Deployment

1. Run migration: `mvn flyway:migrate` (V16)
2. Deploy code changes
3. Verify: Check prescriptions status changed from ACTIVE to PENDING
4. Test endpoints with sample data
5. Monitor: Check error rates on /dispense endpoint
6. Rollback plan: If issues, stop using /dispense, revert to manual inventory management
