-- ============================================================================
-- V16: Enhance Inventory with Operations Research Fields (Phase D Feature 2)
-- ============================================================================
-- This migration adds Economic Order Quantity (EOQ) and Reorder Point (ROP)
-- calculation fields to the inventory table, enabling scientific inventory
-- optimization based on Operations Research principles.
--
-- Mathematical Foundation:
--
-- Theorem 8 (EOQ - Economic Order Quantity):
-- Optimal order quantity minimizing total cost: Q* = √(2DS/H)
-- Where:
--   D = Annual demand (units/year)
--   S = Fixed ordering cost per order ($)
--   H = Holding cost per unit per year ($/unit/year)
--
-- Proof: Total Cost TC(Q) = (D/Q)·S + (Q/2)·H + D·P
--        dTC/dQ = -DS/Q² + H/2 = 0
--        Q² = 2DS/H
--        Q* = √(2DS/H) > 0
--        d²TC/dQ² = 2DS/Q³ > 0 ⟹ minimum ✓
--
-- Theorem 9 (ROP - Reorder Point with Safety Stock):
-- Prevent stockouts: ROP = d·L + z·σ·√L
-- Where:
--   d = Average daily demand (units/day)
--   L = Lead time (days)
--   σ = Standard deviation of daily demand
--   z = Z-score for service level
--   z·σ·√L = Safety stock
--
-- Theorem 10 (ABC Analysis - Pareto Principle):
-- Classify items by cumulative annual value:
--   A items: 70% of value (~20% of items) - Tight control
--   B items: 20% of value (~30% of items) - Moderate control
--   C items: 10% of value (~50% of items) - Loose control
--
-- Invariants Enforced:
-- 1. EOQ > 0 (if parameters present)
-- 2. ROP ≥ 0, Safety Stock ≥ 0 (if parameters present)
-- 3. Temporal ordering: period_end ≥ period_start
-- 4. Demand stats: min ≤ max, avg ≥ 0, std_dev ≥ 0
-- 5. Service level: 0.0 ≤ service_level ≤ 1.0
--
-- Operations Research Principle: Inventory Management and Cost Optimization
-- ============================================================================

-- ============================================================================
-- ALTER INVENTORY TABLE - Add OR Fields
-- ============================================================================
-- Add 9 new columns for EOQ/ROP calculation parameters

ALTER TABLE inventory
ADD COLUMN annual_demand NUMERIC(10,2) CHECK (annual_demand IS NULL OR annual_demand >= 0),
ADD COLUMN ordering_cost NUMERIC(10,2) CHECK (ordering_cost IS NULL OR ordering_cost >= 0),
ADD COLUMN holding_cost NUMERIC(10,2) CHECK (holding_cost IS NULL OR holding_cost >= 0),
ADD COLUMN lead_time_days INTEGER CHECK (lead_time_days IS NULL OR lead_time_days >= 0),
ADD COLUMN demand_std_dev NUMERIC(10,2) CHECK (demand_std_dev IS NULL OR demand_std_dev >= 0),
ADD COLUMN service_level NUMERIC(4,3) CHECK (service_level IS NULL OR (service_level >= 0.0 AND service_level <= 1.0)),
ADD COLUMN eoq NUMERIC(10,2) CHECK (eoq IS NULL OR eoq > 0),
ADD COLUMN reorder_point INTEGER CHECK (reorder_point IS NULL OR reorder_point >= 0),
ADD COLUMN safety_stock INTEGER CHECK (safety_stock IS NULL OR safety_stock >= 0),
ADD COLUMN abc_classification VARCHAR(1) CHECK (abc_classification IS NULL OR abc_classification IN ('A', 'B', 'C'));

-- Add comments explaining OR fields
COMMENT ON COLUMN inventory.annual_demand IS 'Annual demand in units/year (D in EOQ formula)';
COMMENT ON COLUMN inventory.ordering_cost IS 'Fixed ordering cost per order in currency (S in EOQ formula)';
COMMENT ON COLUMN inventory.holding_cost IS 'Holding cost per unit per year in currency (H in EOQ formula)';
COMMENT ON COLUMN inventory.lead_time_days IS 'Supplier lead time in days (L in ROP formula)';
COMMENT ON COLUMN inventory.demand_std_dev IS 'Standard deviation of daily demand (σ in ROP formula)';
COMMENT ON COLUMN inventory.service_level IS 'Target service level 0.0-1.0 (no-stockout probability)';
COMMENT ON COLUMN inventory.eoq IS 'Calculated Economic Order Quantity: Q* = √(2DS/H)';
COMMENT ON COLUMN inventory.reorder_point IS 'Calculated Reorder Point: ROP = d·L + z·σ·√L';
COMMENT ON COLUMN inventory.safety_stock IS 'Calculated Safety Stock: SS = z·σ·√L';
COMMENT ON COLUMN inventory.abc_classification IS 'ABC Classification (A=70% value, B=20%, C=10%)';

-- ============================================================================
-- CREATE INVENTORY_ANALYTICS TABLE
-- ============================================================================
-- Track historical demand statistics for ROP parameter estimation
-- Used to calculate avgDailyDemand and demandStdDev for ROP formula

CREATE TABLE inventory_analytics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    inventory_id UUID NOT NULL REFERENCES inventory(id),

    -- Analysis period (inclusive range)
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    CONSTRAINT period_ordering CHECK (period_end >= period_start),

    -- Demand statistics
    total_demand INTEGER NOT NULL CHECK (total_demand >= 0),
    avg_daily_demand NUMERIC(10,4) NOT NULL CHECK (avg_daily_demand >= 0),
    demand_std_dev NUMERIC(10,4) NOT NULL CHECK (demand_std_dev >= 0),
    min_daily_demand INTEGER,
    max_daily_demand INTEGER,
    CONSTRAINT min_max_demand CHECK (min_daily_demand IS NULL OR max_daily_demand IS NULL OR min_daily_demand <= max_daily_demand),

    -- Audit trail
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

-- Add comments for inventory_analytics table
COMMENT ON TABLE inventory_analytics IS 'Historical demand statistics for inventory items, used to calculate ROP parameters';
COMMENT ON COLUMN inventory_analytics.total_demand IS 'Total units consumed during the period';
COMMENT ON COLUMN inventory_analytics.avg_daily_demand IS 'Average daily demand (d in ROP formula)';
COMMENT ON COLUMN inventory_analytics.demand_std_dev IS 'Standard deviation of daily demand (σ in ROP formula)';

-- ============================================================================
-- CREATE INDEXES for Performance
-- ============================================================================

-- Index for EOQ lookups
CREATE INDEX idx_inventory_eoq
ON inventory(eoq)
WHERE eoq IS NOT NULL;

-- Index for ROP lookups and reorder checks
-- Optimizes: "find items where current_stock <= reorder_point"
CREATE INDEX idx_inventory_reorder_point
ON inventory(reorder_point)
WHERE reorder_point IS NOT NULL;

-- Index for ABC classification queries
CREATE INDEX idx_inventory_abc
ON inventory(abc_classification)
WHERE abc_classification IS NOT NULL;

-- Index for analytics queries
CREATE INDEX idx_inventory_analytics_tenant
ON inventory_analytics(tenant_id, deleted_at);

-- Index for analytics by inventory and period (time-series queries)
CREATE INDEX idx_inventory_analytics_inventory
ON inventory_analytics(inventory_id, period_start DESC)
WHERE deleted_at IS NULL;

-- Index for period range queries
CREATE INDEX idx_inventory_analytics_period
ON inventory_analytics(period_start, period_end)
WHERE deleted_at IS NULL;

-- ============================================================================
-- CREATE OR REPLACE TRIGGERS for Invariant Validation
-- ============================================================================

-- Trigger function to validate temporal invariants
CREATE OR REPLACE FUNCTION validate_inventory_analytics_temporal()
RETURNS TRIGGER AS $$
BEGIN
    -- Invariant: periodStart ≤ periodEnd
    IF NEW.period_start > NEW.period_end THEN
        RAISE EXCEPTION 'Invariant violation: period_start (%) must be ≤ period_end (%)',
            NEW.period_start, NEW.period_end;
    END IF;

    -- Invariant: min_daily_demand ≤ max_daily_demand
    IF NEW.min_daily_demand IS NOT NULL AND NEW.max_daily_demand IS NOT NULL THEN
        IF NEW.min_daily_demand > NEW.max_daily_demand THEN
            RAISE EXCEPTION 'Invariant violation: min_daily_demand (%) > max_daily_demand (%)',
                NEW.min_daily_demand, NEW.max_daily_demand;
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger on inventory_analytics
CREATE TRIGGER trg_validate_inventory_analytics_temporal
BEFORE INSERT OR UPDATE ON inventory_analytics
FOR EACH ROW
EXECUTE FUNCTION validate_inventory_analytics_temporal();

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================
-- Run these to verify the migration completed successfully

-- Verify new columns added
-- SELECT column_name FROM information_schema.columns
-- WHERE table_name = 'inventory'
-- AND column_name IN ('annual_demand', 'ordering_cost', 'holding_cost',
--                     'lead_time_days', 'demand_std_dev', 'service_level',
--                     'eoq', 'reorder_point', 'safety_stock', 'abc_classification');

-- Verify inventory_analytics table created
-- SELECT * FROM information_schema.tables WHERE table_name = 'inventory_analytics';

-- Verify indexes created
-- SELECT indexname FROM pg_indexes
-- WHERE tablename IN ('inventory', 'inventory_analytics')
-- AND indexname LIKE 'idx_inventory%';

-- ============================================================================
-- ROLLBACK PROCEDURE (if needed)
-- ============================================================================
-- To rollback, execute:
-- DROP TABLE IF EXISTS inventory_analytics CASCADE;
-- ALTER TABLE inventory DROP COLUMN IF EXISTS annual_demand;
-- ALTER TABLE inventory DROP COLUMN IF EXISTS ordering_cost;
-- ALTER TABLE inventory DROP COLUMN IF EXISTS holding_cost;
-- ALTER TABLE inventory DROP COLUMN IF EXISTS lead_time_days;
-- ALTER TABLE inventory DROP COLUMN IF EXISTS demand_std_dev;
-- ALTER TABLE inventory DROP COLUMN IF EXISTS service_level;
-- ALTER TABLE inventory DROP COLUMN IF EXISTS eoq;
-- ALTER TABLE inventory DROP COLUMN IF EXISTS reorder_point;
-- ALTER TABLE inventory DROP COLUMN IF EXISTS safety_stock;
-- ALTER TABLE inventory DROP COLUMN IF EXISTS abc_classification;
