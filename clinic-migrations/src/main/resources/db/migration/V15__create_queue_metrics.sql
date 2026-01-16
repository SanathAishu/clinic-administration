-- ============================================================================
-- V15: Create Queue Metrics Table for Phase D (Queue/Token Management)
-- ============================================================================
-- This migration creates the queue_metrics table for M/M/1 queuing theory
-- implementation in clinic appointment systems.
--
-- Mathematical Foundation: M/M/1 Queuing System
-- - M: Markovian arrivals (Poisson process)
-- - M: Markovian service times (Exponential distribution)
-- - 1: Single server (one doctor)
--
-- Key Theorems Implemented:
-- 1. Utilization: ρ = λ/μ (INVARIANT)
-- 2. Little's Law: L = λW (INVARIANT)
-- 3. Average Wait Time: W = 1/(μ - λ)
-- 4. Queue Length: Lq = ρ²/(1-ρ)
-- 5. Queue Wait Time: Wq = ρ/(μ-λ)
-- 6. System Length: L = ρ/(1-ρ)
--
-- Invariants Enforced at Database Level:
-- 1. ρ = λ/μ (within 0.0001 tolerance)
-- 2. λ < μ (stability condition for unbounded queue)
-- 3. 0 ≤ ρ < 1.0 (valid utilization range)
-- 4. completedAppointments ≤ totalPatients
-- 5. metricStartTime ≤ metricEndTime (temporal ordering)
--
-- Operations Research Principle: Queue optimization and resource allocation
-- ============================================================================

-- ============================================================================
-- CREATE queue_metrics TABLE
-- ============================================================================
CREATE TABLE queue_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    doctor_id UUID NOT NULL,
    metric_date DATE NOT NULL,

    -- M/M/1 Parameters (Markovian Queuing Theory)
    -- λ = Arrival rate (patients/hour)
    arrival_rate NUMERIC(10,4) NOT NULL CHECK (arrival_rate >= 0),

    -- μ = Service rate (patients/hour)
    service_rate NUMERIC(10,4) NOT NULL CHECK (service_rate > 0),

    -- ρ = Utilization = λ/μ
    utilization NUMERIC(5,4) NOT NULL CHECK (utilization >= 0 AND utilization < 1.0),

    -- M/M/1 Calculated Metrics (converted to minutes for user display)
    -- W = 1/(μ - λ) hours = Average time in system
    avg_wait_time NUMERIC(10,2),

    -- Wq = ρ/(μ - λ) hours = Average time in queue
    avg_wait_in_queue NUMERIC(10,2),

    -- L = ρ/(1 - ρ) = Average number in system
    avg_system_length NUMERIC(10,4),

    -- Lq = ρ²/(1 - ρ) = Average number in queue
    avg_queue_length NUMERIC(10,4),

    -- Actual Data from Appointments
    total_patients INTEGER CHECK (total_patients >= 0),
    completed_appointments INTEGER CHECK (completed_appointments >= 0),

    -- Measurement period timestamps (for metrics calculation window)
    metric_start_time TIME,
    metric_end_time TIME,

    -- Soft Delete & Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,

    -- ========================================================================
    -- M/M/1 INVARIANT CONSTRAINTS (Discrete Math Enforcement)
    -- ========================================================================

    -- Invariant 1: ρ = λ/μ (within tolerance for floating point)
    CONSTRAINT queue_metrics_utilization_invariant CHECK (
        ABS(utilization - (arrival_rate / service_rate)) < 0.0001
    ),

    -- Invariant 2: λ < μ (Stability condition - queue must not grow unbounded)
    CONSTRAINT queue_metrics_stability CHECK (arrival_rate < service_rate),

    -- Invariant 3: completedAppointments ≤ totalPatients
    CONSTRAINT queue_metrics_completions CHECK (
        completed_appointments IS NULL OR
        total_patients IS NULL OR
        completed_appointments <= total_patients
    ),

    -- Invariant 4: Temporal ordering (startTime ≤ endTime)
    CONSTRAINT queue_metrics_time_order CHECK (
        metric_start_time IS NULL OR
        metric_end_time IS NULL OR
        metric_start_time <= metric_end_time
    ),

    -- Invariant 5: Unique metrics per doctor per date per tenant
    CONSTRAINT queue_metrics_unique_doctor_date UNIQUE (doctor_id, tenant_id, metric_date),

    -- Foreign key constraints
    CONSTRAINT fk_queue_metrics_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE,

    CONSTRAINT fk_queue_metrics_doctor FOREIGN KEY (doctor_id)
        REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================================================
-- INDEXES FOR QUERY OPTIMIZATION
-- ============================================================================

-- Index: Tenant-scoped queries (multi-tenancy RLS support)
-- Use case: Get all queue metrics for a tenant
-- Example: SELECT * FROM queue_metrics WHERE tenant_id = ? AND deleted_at IS NULL
CREATE INDEX idx_queue_metrics_tenant ON queue_metrics(tenant_id)
WHERE deleted_at IS NULL;

-- Index: Doctor-specific metrics (commonly queried)
-- Use case: Get all metrics for a doctor on a date
-- Example: SELECT * FROM queue_metrics WHERE doctor_id = ? AND metric_date = ?
CREATE INDEX idx_queue_metrics_doctor_date ON queue_metrics(doctor_id, metric_date)
WHERE deleted_at IS NULL;

-- Index: High-utilization detection (bottleneck analysis)
-- Use case: Find queues with utilization > 0.85 (alert threshold)
-- Example: SELECT * FROM queue_metrics WHERE utilization > 0.85 ORDER BY utilization DESC
CREATE INDEX idx_queue_metrics_utilization ON queue_metrics(utilization DESC)
WHERE utilization > 0.85 AND deleted_at IS NULL;

-- Index: Date range queries (analytics and reporting)
-- Use case: Get metrics for a date range
-- Example: SELECT * FROM queue_metrics WHERE metric_date BETWEEN ? AND ?
CREATE INDEX idx_queue_metrics_date_range ON queue_metrics(metric_date DESC)
WHERE deleted_at IS NULL;

-- Composite index: Doctor + Date + Tenant (most common query pattern)
-- Use case: Get specific doctor's metrics for a date
-- Example: SELECT * FROM queue_metrics WHERE doctor_id = ? AND tenantid = ? AND metric_date = ?
CREATE INDEX idx_queue_metrics_doctor_tenant_date ON queue_metrics(doctor_id, tenant_id, metric_date)
WHERE deleted_at IS NULL;

-- Index: Unstable queue detection (λ ≥ μ condition)
-- Use case: Find queues approaching or exceeding capacity
-- Example: SELECT * FROM queue_metrics WHERE arrival_rate >= service_rate
CREATE INDEX idx_queue_metrics_stability ON queue_metrics(arrival_rate, service_rate)
WHERE deleted_at IS NULL;

-- ============================================================================
-- ROW LEVEL SECURITY (RLS) for Multi-Tenancy Enforcement
-- ============================================================================
-- Enable RLS on queue_metrics table
ALTER TABLE queue_metrics ENABLE ROW LEVEL SECURITY;

-- RLS Policy: Tenant isolation
-- Tenants can only access their own queue metrics
CREATE POLICY queue_metrics_tenant_isolation ON queue_metrics
    USING (tenant_id = current_setting('app.current_tenant_id')::UUID)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id')::UUID);

-- ============================================================================
-- DOCUMENTATION COMMENTS (PostgreSQL pg_description)
-- ============================================================================
COMMENT ON TABLE queue_metrics IS
    'Queue metrics using M/M/1 queuing theory. Stores daily queue statistics for operations analysis.';

COMMENT ON COLUMN queue_metrics.arrival_rate IS
    'λ - Arrival rate in patients/hour. Calculated from scheduled appointments.';

COMMENT ON COLUMN queue_metrics.service_rate IS
    'μ - Service rate in patients/hour. Calculated from historical appointment durations.';

COMMENT ON COLUMN queue_metrics.utilization IS
    'ρ = λ/μ - Utilization factor. Must be < 1.0 for queue stability.';

COMMENT ON COLUMN queue_metrics.avg_wait_time IS
    'W = 1/(μ - λ) - Average time in system, converted to minutes.';

COMMENT ON COLUMN queue_metrics.avg_queue_length IS
    'Lq = ρ²/(1 - ρ) - Average number of customers waiting (Little''s Law).';
