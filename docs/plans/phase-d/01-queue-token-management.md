# Phase D Feature 1: Queue/Token Management System

**Status**: Ready for Implementation
**Priority**: HIGHEST
**Dependencies**: None (independent feature)
**Estimated Timeline**: 1-2 weeks
**Task Assignment**: Task Agent - Queue Management

---

## Executive Summary

Implement a real-time queue management system based on M/M/1 queuing theory. This feature provides:

- ✅ Monotonic token generation per doctor per day (proven mathematically)
- ✅ Real-time wait time estimation using M/M/1 formulas
- ✅ Queue position tracking with performance analytics
- ✅ Digital display board support for clinics
- ✅ Daily metrics calculation with queue stability monitoring

The `token_number` field already exists in the appointments table, simplifying implementation.

---

## Mathematical Foundation: M/M/1 Queuing Theory

### Assumptions (Markovian Queuing Model)

- **M/M/1**: Markovian arrivals / Markovian service / 1 server
- **Arrivals**: Poisson process with rate λ (patients per hour)
- **Service times**: Exponentially distributed with rate μ (patients per hour)
- **Queue discipline**: FIFO (First In, First Out)
- **Capacity**: Infinite queue (can grow unbounded if unstable)

### Fundamental Theorems with Proofs

#### Theorem 1: Average Wait Time in System: W = 1/(μ - λ)

**Statement**: For an M/M/1 queue with arrival rate λ and service rate μ where λ < μ (stable system), the average time a customer spends in the system (waiting + service) is:

```
W = 1/(μ - λ)
```

**Proof**:

Let ρ = λ/μ be the utilization factor (0 < ρ < 1 for stability).

The probability that exactly n customers are in the system:
```
P(N = n) = (1 - ρ)ρⁿ for n = 0, 1, 2, ...  (geometric distribution)
```

Expected number of customers in system:
```
L = E[N] = Σ(n=0 to ∞) n·P(N = n)
        = Σ(n=0 to ∞) n·(1 - ρ)ρⁿ
        = (1 - ρ)·Σ(n=1 to ∞) n·ρⁿ
```

Using identity Σ(n=1 to ∞) n·xⁿ = x/(1-x)²:
```
L = (1 - ρ)·ρ/(1-ρ)² = ρ/(1-ρ) = λ/(μ - λ)
```

By Little's Law (L = λW):
```
W = L/λ = [λ/(μ - λ)]/λ = 1/(μ - λ) ✓
```

#### Theorem 2: Little's Law: L = λW

**Statement**: In a stable queuing system, long-run average number of customers in system equals arrival rate times average time in system.

**Proof** (intuitive):
Over interval [0,T]:
- Total arrivals: A(T)
- Total departures: D(T)
- Customers in system at time t: N(t)
- Time customer i spends: Wᵢ

Area under N(t) curve equals sum of times all customers spent:
```
∫₀ᵀ N(t) dt ≈ Σᵢ₌₁^D(T) Wᵢ
```

As T → ∞:
```
[1/T ∫₀ᵀ N(t) dt] = [1/T Σᵢ₌₁^A(T) Wᵢ]
        L            =        λ·W    ✓
```

#### Theorem 3: Average Queue Length: Lq = ρ²/(1 - ρ)

**Statement**: Average number of customers waiting in queue (excluding those being served):

```
Lq = ρ²/(1 - ρ) = λ²/(μ(μ - λ))
```

**Proof**:
```
L = ρ/(1-ρ)                           [from Theorem 1]
Ls = ρ                                [probability server busy]
Lq = L - Ls = ρ/(1-ρ) - ρ
   = [ρ - ρ(1-ρ)]/(1-ρ)
   = ρ²/(1-ρ) ✓
```

#### Theorem 4: Average Wait in Queue: Wq = ρ/(μ - λ)

**Statement**: Average time customer spends waiting before service begins:

```
Wq = ρ/(μ - λ) = λ/(μ(μ - λ))
```

**Proof** (by Little's Law applied to queue):
```
Lq = λWq
Wq = Lq/λ = [ρ²/(1-ρ)]/λ = ρ/(μ - λ) ✓
```

#### Theorem 5: Stability Condition: ρ < 1

**Statement**: For M/M/1 queue to reach steady state (not grow unboundedly):

```
ρ = λ/μ < 1  ⟹  λ < μ
```

**Proof by Contradiction**:
Assume ρ ≥ 1, i.e., λ ≥ μ.

Arrivals at rate λ, service at rate μ.
Net accumulation: (λ - μ) ≥ 0

Over time t:
```
E[N(t)] = N(0) + (λ - μ)·t → ∞ as t → ∞
```

This contradicts existence of steady state.
Therefore: ρ < 1 ✓

#### Theorem 6: Token Monotonicity

**Statement**: For doctor d on date D, token numbers strictly increase with appointment times:

```
∀ appointments A₁, A₂ at times t₁ < t₂:
  token(A₁) < token(A₂)
```

**Algorithm**:
```
token(d, D, t) = COUNT(appointments WHERE
                   doctor = d AND
                   date = D AND
                   appointmentTime ≤ t AND
                   status NOT IN ('CANCELLED', 'NO_SHOW')) + 1
```

**Proof**:
For t₁ < t₂:
```
{appointments with time ≤ t₁} ⊂ {appointments with time ≤ t₂}
⟹ COUNT(...time ≤ t₁) < COUNT(...time ≤ t₂)
⟹ token(d, D, t₁) < token(d, D, t₂) ✓
```

#### Theorem 7: Token Uniqueness

**Statement**: No two active appointments for same doctor on same day have same token.

**Proof by Contradiction**:
Assume appointments A₁ and A₂ both have token = k for doctor d on date D.

Case 1: t₁ < t₂
- A₁ appears in COUNT(...time ≤ t₁)
- Both A₁ and A₂ appear in COUNT(...time ≤ t₂)
- COUNT(...time ≤ t₁) < COUNT(...time ≤ t₂)
- ⟹ token(d, D, t₁) < token(d, D, t₂)
- Contradicts k = k ✓

Case 2: t₁ = t₂
- Impossible by slot allocation (no overlapping appointments)

Case 3: t₁ > t₂
- Symmetric to Case 1

Therefore token uniqueness proven ✓

---

## Implementation Details

### 1. Create QueueMetrics Entity

**File**: `clinic-common/src/main/java/com/clinic/common/entity/operational/QueueMetrics.java`

Key fields:
- `arrivalRate` (λ): patients/hour
- `serviceRate` (μ): patients/hour
- `utilization` (ρ): λ/μ
- `avgWaitTime` (W): 1/(μ - λ) in minutes
- `avgWaitInQueue` (Wq): ρ/(μ - λ) in minutes
- `avgSystemLength` (L): λW
- `avgQueueLength` (Lq): ρ²/(1-ρ)

**Invariant Validation** in @PrePersist/@PreUpdate:
- ρ = λ/μ (within 0.0001 tolerance)
- λ < μ (stability)
- 0 ≤ ρ < 1.0 (valid utilization)
- All Little's Law relationships hold
- Temporal ordering: endTime ≥ startTime
- Completed ≤ Total patients
- noShows + cancellations ≤ total

### 2. Create QueueManagementService

**File**: `clinic-backend/src/main/java/com/clinic/backend/service/QueueManagementService.java`

Core methods:
- `generateTokenNumber(doctorId, tenantId, date, time)`: Generate next token (Theorem 6/7)
- `estimateWaitTime(appointmentId, tenantId)`: M/M/1 calculation (Theorem 1)
- `getQueuePosition(appointmentId, doctorId, tenantId, date)`: Current position in queue
- `calculateServiceRate(doctorId, tenantId)`: Historical average μ (last 7 days)
- `calculateArrivalRate(doctorId, tenantId, date)`: Current day λ
- `getCurrentQueueStatus(doctorId, tenantId)`: DTO for digital display (30s cache)
- `calculateQueueMetrics(doctorId, tenantId, date)`: Store metrics for analytics
- `calculateDailyMetrics()`: @Scheduled at 23:59 daily

**Caching Strategy**:
- Queue status: 30 seconds (highly volatile)
- Wait time estimates: Per appointment (evict on status change)
- Service rate: 1 hour (stable)
- Arrival rate: 5 minutes (medium volatility)

**Transaction Boundaries**:
- `@Transactional` on token generation (atomic count + increment)
- `@Transactional` on metrics calculation (atomic save)

### 3. Database Migration: V15__create_queue_metrics.sql

Create table with:
- All M/M/1 formula fields
- CHECK constraints for invariants:
  - `ABS(utilization - (arrival_rate / service_rate)) < 0.0001`
  - `arrival_rate < service_rate` (stability)
  - `completed_appointments <= total_patients`
  - `min_wait_time <= max_wait_time`
- Composite indexes:
  - `(tenant_id, doctor_id, metric_date)`
  - `(utilization DESC)` WHERE utilization > 0.85
- Row Level Security (RLS) for multi-tenancy

### 4. Create REST API Endpoints

**File**: `clinic-backend/src/main/java/com/clinic/backend/controller/QueueManagementController.java`

Endpoints:
- `GET /api/queue/status/{doctorId}` - Current queue display
- `GET /api/queue/wait-time/{appointmentId}` - Patient wait estimate
- `GET /api/queue/position/{appointmentId}` - Queue position
- `GET /api/queue/metrics/{doctorId}?date=2026-01-16` - Analytics
- `POST /api/queue/metrics/calculate?date=2026-01-16` - Manual trigger

**DTOs**:
- `QueueStatusDTO`: current_token, next_token, patients_waiting, avg_wait_time, utilization
- `WaitTimeDTO`: appointment_id, estimated_wait_minutes
- `QueuePositionDTO`: position, queue_length, ahead_count

**Security**: `@PreAuthorize("hasAnyAuthority('ADMIN', 'DOCTOR', 'RECEPTIONIST')")`

### 5. Modify Existing Services

**AppointmentService.java**:
- Call `queueManagementService.generateTokenNumber()` when creating appointment
- Call `queueManagementService.evictQueueCache()` when status changes

**AppointmentRepository.java** - Add query methods:
- `countTokensBeforeTime(doctorId, tenantId, dayStart, appointmentTime, excludeStatuses)`
- `countAppointmentsBeforeInQueue(doctorId, tenantId, dayStart, appointmentTime, statuses)`
- `findDoctorAppointmentsForDay(doctorId, tenantId, dayStart, dayEnd)`
- `findDoctorsWithAppointmentsOnDate(date)`
- `findFirstAppointmentForDoctorOnDate(doctorId, date)`

---

## Verification Checklist

### Mathematical Correctness
- [ ] Token numbers are strictly monotonic per doctor per day
- [ ] No two appointments have same token for same doctor on same date
- [ ] M/M/1 formulas all satisfied:
  - ρ = λ/μ (within tolerance)
  - L = λW (Little's Law)
  - Lq = ρ²/(1-ρ) (queue length)
  - Wq = ρ/(μ-λ) (queue wait)
- [ ] Stability condition λ < μ enforced (logs warning if violated)
- [ ] All invariants validated in @PrePersist/@PreUpdate
- [ ] Database CHECK constraints prevent invalid states

### Functional Testing
- [ ] Wait time estimates within 20% of actual (validate against 2+ weeks history)
- [ ] Queue position correctly calculated (count query verification)
- [ ] Service rate calculation from historical data matches expectations
- [ ] Arrival rate calculation matches daily appointments ÷ 8 hours
- [ ] Digital display shows correct current/next tokens
- [ ] Metrics calculation runs on schedule (11:59 PM)
- [ ] Cache evicts correctly on appointment status changes

### Performance Testing
- [ ] Token generation < 100ms (single count + increment)
- [ ] Wait time calculation < 50ms (cached when possible)
- [ ] Queue status endpoint returns < 200ms
- [ ] Metrics calculation completes < 5 seconds (even with 100+ appointments)
- [ ] Daily metrics job completes within 5 minutes (all doctors)

### Security & Multi-Tenancy
- [ ] All queries filtered by tenant_id
- [ ] RLS policies enforced in database
- [ ] Cache keys include tenant_id prefix
- [ ] Token numbers isolated per tenant (same doctor ID across tenants won't conflict)
- [ ] Audit logging on queue-related operations

### Edge Cases
- [ ] Handle day boundary (tokens reset at midnight)
- [ ] Handle cancelled/no-show appointments (excluded from token count)
- [ ] Handle zero appointments on a day (no metrics calculated)
- [ ] Handle λ = μ (prevent division by zero, log warning)
- [ ] Handle λ > μ (unstable queue, use heuristic wait time)
- [ ] Handle back-dated appointment creation (token generation still works)

---

## Success Metrics

After deployment, measure:

| Metric | Target | Validation |
|--------|--------|-----------|
| Wait time accuracy | ±20% of actual | Compare estimates vs. actual for 2 weeks |
| Token uniqueness | 100% | No duplicates in system |
| Queue stability | ρ < 0.85 | Monitor utilization trends |
| System responsiveness | <200ms per API call | Load test with 100+ concurrent users |
| Patient satisfaction | 85%+ satisfied with wait accuracy | Survey after 2 weeks |
| SLA compliance | <15 min avg wait | 95% of days meet SLA |

---

## Implementation Order

1. **Week 1 (Days 1-2)**: Create QueueMetrics entity with invariant validation
2. **Week 1 (Days 3-4)**: Implement QueueManagementService (all algorithms)
3. **Week 1 (Days 5)**: Create Flyway migration V15 with database schema
4. **Week 2 (Days 1-2)**: Create REST API endpoints and DTOs
5. **Week 2 (Days 3-4)**: Integrate with AppointmentService and repositories
6. **Week 2 (Days 5)**: Testing and verification

---

## Dependencies & Integration Points

**Depends On**:
- Appointment entity (already exists with token_number field)
- AppointmentRepository (existing)
- User/Doctor entities (existing)
- Redis cache (existing)

**Integrates With**:
- AppointmentService (calls token generation)
- Appointment controllers (wait time display)
- Admin dashboards (metrics/analytics)
- Digital display screens (queue status)

---

## Rollback Plan

If issues arise:

1. Disable daily metrics job (`@Scheduled` method)
2. Stop calling token generation in AppointmentService
3. Drop queue_metrics table
4. Clear Redis cache (`queue:*` keys)
5. Drop QueueMetrics entity class
6. Drop QueueManagementService

No data loss since queue metrics are computed analytics (not source data).

---

## References

- CLAUDE.md: Operations Research Principles & Queuing Theory (lines 112-212)
- CLAUDE.md: Discrete Mathematics Principles (lines 214-309)
- Existing: AppointmentService.java (slot calculation pattern)
- Existing: RedisCacheConfig.java (multi-level caching example)

---

**Next Feature**: Inventory Optimization (02-inventory-optimization.md)
