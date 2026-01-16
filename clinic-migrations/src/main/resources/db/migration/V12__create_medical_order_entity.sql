-- ============================================================================
-- V12: Create Medical Order Entity
-- ============================================================================
-- This migration creates:
-- 1. MedicalOrder table for tracking product orders (braces, devices, etc.)
--
-- Feature #12: Medical Orders
-- Tracks orders for braces, orthotic devices, surgical items, and medical products
-- sent to manufacturers for custom fabrication or sourcing
--
-- Discrete Math Principles:
-- - State Machine: 8 states forming a Directed Acyclic Graph (DAG)
--   PENDING → SENT → IN_PRODUCTION → SHIPPED → RECEIVED → READY_FOR_PICKUP → DELIVERED
--   CANCELLED (from PENDING, SENT, IN_PRODUCTION, SHIPPED, RECEIVED)
-- - Temporal Ordering: order_date <= expected_delivery_date <= actual_delivery_date
-- - Invariants: Status-timestamp consistency
--
-- Operations Research:
-- - Inventory management: Track order status for supply chain
-- - Demand forecasting: Analyze order patterns
-- - Lead time tracking: Monitor expected vs actual delivery
-- ============================================================================

CREATE TABLE medical_orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE RESTRICT,
    branch_id UUID REFERENCES branches(id) ON DELETE SET NULL,

    -- Order Details (Discrete Math: State machine)
    product_name VARCHAR(255) NOT NULL,
    product_type VARCHAR(100),
    description TEXT,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL,
    total_amount NUMERIC(12, 2),

    -- Manufacturer Information
    manufacturer_name VARCHAR(255) NOT NULL,
    manufacturer_email VARCHAR(255),
    manufacturer_phone VARCHAR(15),

    -- Order Timeline (Discrete Math: Temporal ordering invariant)
    order_date DATE NOT NULL,
    expected_delivery_date DATE,
    actual_delivery_date DATE,

    -- Tracking
    tracking_number VARCHAR(100),

    -- Status (State Machine: 8 states, forming a DAG)
    -- PENDING: Order created, not yet sent
    -- SENT: Order sent to manufacturer
    -- IN_PRODUCTION: Item is being fabricated/assembled
    -- SHIPPED: Item shipped by manufacturer, in transit
    -- RECEIVED: Item received at clinic
    -- READY_FOR_PICKUP: Item ready for patient to collect
    -- DELIVERED: Item delivered/handed to patient
    -- CANCELLED: Order cancelled (can happen from PENDING through RECEIVED)
    status order_status_enum NOT NULL DEFAULT 'PENDING',

    -- Timestamps for State Machine (Discrete Math: Temporal consistency)
    sent_at TIMESTAMPTZ,
    received_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,

    -- Special Instructions
    special_instructions TEXT,
    notes TEXT,

    -- Patient Notification
    patient_notified_at TIMESTAMPTZ,

    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users(id),
    deleted_at TIMESTAMPTZ,

    -- Constraints (Discrete Math: Invariants)
    CONSTRAINT medical_order_quantity_positive CHECK (quantity > 0),
    CONSTRAINT medical_order_price_non_negative CHECK (unit_price >= 0),
    CONSTRAINT medical_order_total_positive CHECK (total_amount IS NULL OR total_amount >= 0),
    CONSTRAINT medical_order_dates_ordered CHECK (
        order_date IS NULL OR expected_delivery_date IS NULL OR
        order_date <= expected_delivery_date
    ),
    CONSTRAINT medical_order_delivery_dates_ordered CHECK (
        expected_delivery_date IS NULL OR actual_delivery_date IS NULL OR
        expected_delivery_date <= actual_delivery_date
    ),
    CONSTRAINT medical_order_status_timestamp CHECK (
        (status != 'DELIVERED' OR delivered_at IS NOT NULL) AND
        (status != 'RECEIVED' OR received_at IS NOT NULL) AND
        (status != 'SENT' OR sent_at IS NOT NULL)
    )
);

-- ============================================================================
-- MEDICAL ORDER INDEXES
-- ============================================================================

-- Tenant query index (RLS + Multi-tenancy)
CREATE INDEX idx_medical_orders_tenant ON medical_orders(tenant_id)
WHERE deleted_at IS NULL;

-- Patient query index (feature: get orders by patient)
CREATE INDEX idx_medical_orders_patient ON medical_orders(patient_id, tenant_id)
WHERE deleted_at IS NULL;

-- Branch query index (branch-specific orders)
CREATE INDEX idx_medical_orders_branch ON medical_orders(branch_id)
WHERE deleted_at IS NULL;

-- Status query index (feature: filter by status)
CREATE INDEX idx_medical_orders_status ON medical_orders(status, tenant_id)
WHERE deleted_at IS NULL;

-- Order date range index (analytics: date range queries)
CREATE INDEX idx_medical_orders_order_date ON medical_orders(order_date)
WHERE deleted_at IS NULL;

-- Expected delivery date index (feature: find overdue orders)
CREATE INDEX idx_medical_orders_expected_delivery ON medical_orders(expected_delivery_date)
WHERE deleted_at IS NULL AND status NOT IN ('DELIVERED', 'CANCELLED');

-- Manufacturer lookup index (feature: track orders per manufacturer)
CREATE INDEX idx_medical_orders_manufacturer ON medical_orders(manufacturer_name, status)
WHERE deleted_at IS NULL AND status IN ('PENDING', 'SENT', 'IN_PRODUCTION', 'SHIPPED');

-- Patient notification tracking (feature: SMS reminders for ready-to-pickup)
CREATE INDEX idx_medical_orders_ready_not_notified ON medical_orders(tenant_id, status, patient_notified_at)
WHERE status = 'READY_FOR_PICKUP' AND patient_notified_at IS NULL AND deleted_at IS NULL;

-- ============================================================================
-- TABLE COMMENTS
-- ============================================================================

COMMENT ON TABLE medical_orders IS 'Medical product orders table. Tracks orders for braces, orthotic devices, surgical items, and custom-fabricated medical products sent to manufacturers.';

COMMENT ON COLUMN medical_orders.id IS 'Unique identifier (UUID)';
COMMENT ON COLUMN medical_orders.tenant_id IS 'Multi-tenancy: isolates orders per organization';
COMMENT ON COLUMN medical_orders.patient_id IS 'Patient who ordered the product';
COMMENT ON COLUMN medical_orders.branch_id IS 'Branch that placed or manages the order';
COMMENT ON COLUMN medical_orders.status IS 'Current status in 8-state machine (DAG: PENDING → ... → DELIVERED). (Discrete Math: State machine)';
COMMENT ON COLUMN medical_orders.order_date IS 'Date order was placed (Discrete Math: Temporal ordering anchor)';
COMMENT ON COLUMN medical_orders.expected_delivery_date IS 'Expected date of delivery from manufacturer (Discrete Math: Temporal ordering)';
COMMENT ON COLUMN medical_orders.actual_delivery_date IS 'Actual date item was received at clinic (Discrete Math: Temporal ordering)';
COMMENT ON COLUMN medical_orders.sent_at IS 'Timestamp when order was sent to manufacturer (State machine: SENT state timestamp)';
COMMENT ON COLUMN medical_orders.received_at IS 'Timestamp when item was received at clinic (State machine: RECEIVED state timestamp)';
COMMENT ON COLUMN medical_orders.delivered_at IS 'Timestamp when item was delivered to patient (State machine: DELIVERED state timestamp)';
COMMENT ON COLUMN medical_orders.patient_notified_at IS 'Timestamp when patient was notified of ready-to-pickup status (Feature: SMS notifications)';
COMMENT ON COLUMN medical_orders.deleted_at IS 'Soft delete timestamp (null = active, not null = deleted)';

COMMENT ON CONSTRAINT medical_order_quantity_positive ON medical_orders IS 'Invariant: quantity must be at least 1 (Discrete Math: domain constraint)';
COMMENT ON CONSTRAINT medical_order_price_non_negative ON medical_orders IS 'Invariant: unit price must be non-negative (Discrete Math: domain constraint)';
COMMENT ON CONSTRAINT medical_order_dates_ordered ON medical_orders IS 'Temporal ordering invariant: order_date ≤ expected_delivery_date (Discrete Math: total order)';
COMMENT ON CONSTRAINT medical_order_delivery_dates_ordered ON medical_orders IS 'Temporal ordering invariant: expected_delivery_date ≤ actual_delivery_date (Discrete Math: total order)';
COMMENT ON CONSTRAINT medical_order_status_timestamp ON medical_orders IS 'State consistency invariant: status must match timestamp fields (Discrete Math: state consistency)';
