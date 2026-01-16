# Phase D Feature 2: Inventory Optimization Service

**Status**: Ready for Implementation
**Priority**: HIGH
**Dependencies**: None (can run in parallel with Queue Management)
**Estimated Timeline**: 1-2 weeks
**Task Assignment**: Task Agent - Inventory Optimization

---

## Executive Summary

Implement Operations Research-based inventory optimization. This feature provides:

- ✅ Economic Order Quantity (EOQ) calculation to minimize total cost
- ✅ Reorder Point (ROP) with safety stock to prevent stockouts
- ✅ ABC analysis for inventory classification
- ✅ Automated reorder triggers
- ✅ FEFO (First Expired First Out) for medications
- ✅ Lead time tracking and demand statistics

Current Inventory service has basic CRUD only. This enhancement adds scientific optimization.

---

## Mathematical Foundation: Operations Research Inventory Models

### EOQ Model: Minimize Total Inventory Cost

#### Problem Statement

Determine optimal order quantity Q to minimize total annual cost:

```
TC(Q) = (D/Q)·S + (Q/2)·H + D·P
```

Where:
- D = Annual demand (units/year)
- Q = Order quantity (units per order)
- S = Fixed ordering cost per order ($)
- H = Holding (carrying) cost per unit per year ($/unit/year)
- P = Unit purchase price ($)

**Cost Components**:
1. **Ordering cost**: (D/Q)·S - more orders if Q small
2. **Holding cost**: (Q/2)·H - average inventory is Q/2
3. **Purchase cost**: D·P - constant, doesn't affect Q

#### Theorem 8: Economic Order Quantity

**Statement**: The optimal order quantity that minimizes total cost is:

```
Q* = √(2DS/H)
```

**Proof** (Calculus):

To minimize TC(Q), take derivative with respect to Q and set to zero:

```
dTC/dQ = d/dQ[(D/Q)·S + (Q/2)·H]
       = -D·S/Q² + H/2

Set dTC/dQ = 0:
-D·S/Q² + H/2 = 0
D·S/Q² = H/2
Q² = 2DS/H
Q* = √(2DS/H)
```

**Proof of Minimum** (Second derivative test):

```
d²TC/dQ² = 2DS/Q³ > 0  (always positive)
∴ Q* is a minimum (not maximum) ✓
```

**Interpretation**:
- At Q*, ordering cost = holding cost = TC/2
- Ordering more frequently increases ordering cost
- Ordering less frequently increases holding cost
- Q* balances these two opposing forces optimally

#### Theorem 9: Reorder Point with Safety Stock

**Statement**: The inventory level at which to place new order:

```
ROP = d·L + SS
```

Where:
- d = Average daily demand (units/day)
- L = Lead time (days)
- SS = Safety stock = z·σ·√L

**Derivation** (Probability Theory):

Demand during lead time follows normal distribution:
```
Demand ~ Normal(d·L, σ²·L)
```

Where σ = standard deviation of daily demand.

For service level α (e.g., 95% no-stockout probability):
```
P(Demand ≤ ROP) = α
ROP = d·L + z·σ·√L
```

Where z = z-score corresponding to service level α.

**Service Level Z-scores**:
```
90%: z = 1.282
95%: z = 1.645
99%: z = 2.326
99.9%: z = 3.090
```

**Interpretation**:
- Base demand: d·L (expected demand during lead time)
- Safety stock: z·σ·√L (buffer against demand uncertainty)
- ROP protects against both lead time and demand variability

#### Theorem 10: ABC Analysis (Pareto Principle)

**Statement**: Classify inventory into 3 categories by value:

```
A items: Top 70% of annual value (~20% of items) - tight control
B items: Next 20% of value (~30% of items) - moderate control
C items: Last 10% of value (~50% of items) - loose control
```

**Algorithm**:

1. Calculate annual value per item:
   ```
   annualValue[i] = annualDemand[i] × unitPrice[i]
   ```

2. Sort items by annual value (descending)

3. Calculate cumulative value percentage:
   ```
   cumulativeValue = 0
   for each item (sorted):
     cumulativeValue += annualValue[item]
     cumulativePercent = cumulativeValue / totalValue

     if cumulativePercent ≤ 0.70:
       classification = A
     elif cumulativePercent ≤ 0.90:
       classification = B
     else:
       classification = C
   ```

**Justification**:
- Pareto Principle: 80% of effects from 20% of causes
- In inventory: 80% of value from ~20% of items
- Focus intensive control on high-value A items
- Use simplified procedures for low-value C items

---

## Implementation Details

### 1. Enhance Inventory Entity

**File**: `clinic-common/src/main/java/com/clinic/common/entity/operational/Inventory.java`

Add new fields:
```java
@Column(name = "annual_demand", precision = 10, scale = 2)
private Double annualDemand;  // D (units/year)

@Column(name = "ordering_cost", precision = 10, scale = 2)
private BigDecimal orderingCost;  // S ($/order)

@Column(name = "holding_cost", precision = 10, scale = 2)
private BigDecimal holdingCost;  // H ($/unit/year)

@Column(name = "lead_time_days")
private Integer leadTimeDays;  // L (days)

@Column(name = "demand_std_dev", precision = 10, scale = 2)
private Double demandStdDev;  // σ (units/day)

@Column(name = "service_level", precision = 4, scale = 3)
private Double serviceLevel;  // 0.90, 0.95, 0.99, etc.

@Column(name = "eoq", precision = 10, scale = 2)
private Double economicOrderQuantity;  // Q* (calculated)

@Column(name = "reorder_point")
private Integer reorderPoint;  // ROP (calculated)

@Column(name = "safety_stock")
private Integer safetyStock;  // SS (calculated)

@Column(name = "abc_classification")
@Enumerated(EnumType.STRING)
private ABCClassification abcClassification;  // A, B, C
```

**Invariant Validation** in @PrePersist/@PreUpdate:

```java
@PrePersist
@PreUpdate
protected void calculateAndValidateInventory() {
    // Calculate EOQ if all parameters present
    if (annualDemand != null && orderingCost != null && holdingCost != null) {
        double d = annualDemand;
        double s = orderingCost.doubleValue();
        double h = holdingCost.doubleValue();

        // Q* = √(2DS/H)
        this.economicOrderQuantity = Math.sqrt((2 * d * s) / h);

        // Invariant: EOQ > 0
        if (economicOrderQuantity <= 0) {
            throw new IllegalStateException("EOQ must be positive");
        }
    }

    // Calculate ROP and safety stock if parameters present
    if (annualDemand != null && leadTimeDays != null &&
        demandStdDev != null && serviceLevel != null) {

        double dailyDemand = annualDemand / 365.0;
        double z = getZScore(serviceLevel);

        // SS = z × σ × √L
        this.safetyStock = (int) Math.ceil(
            z * demandStdDev * Math.sqrt(leadTimeDays)
        );

        // ROP = d×L + SS
        this.reorderPoint = (int) Math.ceil(
            dailyDemand * leadTimeDays
        ) + safetyStock;

        // Invariants
        if (safetyStock < 0 || reorderPoint < 0) {
            throw new IllegalStateException(
                "Safety stock and ROP must be non-negative"
            );
        }
    }
}

private double getZScore(double serviceLevel) {
    if (serviceLevel >= 0.999) return 3.090;
    if (serviceLevel >= 0.990) return 2.326;
    if (serviceLevel >= 0.950) return 1.645;
    if (serviceLevel >= 0.900) return 1.282;
    return 1.645;  // default to 95%
}
```

### 2. Create InventoryOptimizationService

**File**: `clinic-backend/src/main/java/com/clinic/backend/service/InventoryOptimizationService.java`

Core methods:

```java
/**
 * Calculate Economic Order Quantity using formula Q* = √(2DS/H)
 *
 * @param inventory The inventory item
 * @return EOQ value
 */
public Double calculateEOQ(Inventory inventory) {
    // Q* = √(2DS/H)
    double d = inventory.getAnnualDemand();
    double s = inventory.getOrderingCost().doubleValue();
    double h = inventory.getHoldingCost().doubleValue();

    return Math.sqrt((2 * d * s) / h);
}

/**
 * Calculate Reorder Point with safety stock
 * ROP = d×L + z×σ×√L
 *
 * @param inventory The inventory item
 * @return ROP value
 */
public Integer calculateReorderPoint(Inventory inventory) {
    double dailyDemand = inventory.getAnnualDemand() / 365.0;
    double z = getZScore(inventory.getServiceLevel());

    // Safety stock: SS = z × σ × √L
    int safetyStock = (int) Math.ceil(
        z * inventory.getDemandStdDev() *
        Math.sqrt(inventory.getLeadTimeDays())
    );

    // ROP = d×L + SS
    return (int) Math.ceil(
        dailyDemand * inventory.getLeadTimeDays()
    ) + safetyStock;
}

/**
 * Perform ABC classification on all inventory items
 * Classify by cumulative value percentage
 *
 * @param tenantId The tenant
 */
@Transactional
public void performABCAnalysis(UUID tenantId) {
    List<Inventory> items = inventoryRepository.findByTenantIdAndDeletedAtIsNull(tenantId);

    // Calculate annual value per item
    List<InventoryValue> values = items.stream()
        .map(item -> new InventoryValue(
            item.getId(),
            item.getAnnualDemand() * item.getUnitPrice()
        ))
        .sorted((a, b) -> b.value.compareTo(a.value))  // Descending
        .collect(Collectors.toList());

    double totalValue = values.stream()
        .mapToDouble(v -> v.value)
        .sum();

    // Classify items
    double cumulativeValue = 0;
    for (InventoryValue iv : values) {
        cumulativeValue += iv.value;
        double cumulativePercent = cumulativeValue / totalValue;

        Inventory item = inventoryRepository.findById(iv.itemId).get();

        if (cumulativePercent <= 0.70) {
            item.setAbcClassification(ABCClassification.A);
        } else if (cumulativePercent <= 0.90) {
            item.setAbcClassification(ABCClassification.B);
        } else {
            item.setAbcClassification(ABCClassification.C);
        }

        inventoryRepository.save(item);
    }
}

/**
 * Check reorder points and trigger orders if needed
 * Scheduled daily at 8 AM
 */
@Scheduled(cron = "0 0 8 * * *")
@Transactional
public void checkReorderPoints() {
    List<Inventory> items = inventoryRepository.findAll();

    for (Inventory item : items) {
        if (item.getCurrentStock() <= item.getReorderPoint()) {
            int orderQuantity = item.getEconomicOrderQuantity().intValue();

            log.info("Reorder triggered: {} | Current: {} | ROP: {} | Order: {}",
                item.getItemName(), item.getCurrentStock(),
                item.getReorderPoint(), orderQuantity);

            createPurchaseOrder(item, orderQuantity);
        }
    }
}
```

### 3. Create InventoryAnalytics Entity

**File**: `clinic-common/src/main/java/com/clinic/common/entity/operational/InventoryAnalytics.java`

Track demand statistics for ROP calculation:

```java
@Entity
@Table(name = "inventory_analytics")
public class InventoryAnalytics extends TenantAwareEntity {

    @ManyToOne
    private Inventory inventory;

    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    @Column(name = "total_demand")
    private Integer totalDemand;  // units

    @Column(name = "avg_daily_demand")
    private Double avgDailyDemand;  // units/day

    @Column(name = "demand_std_dev")
    private Double demandStdDev;  // σ

    @Column(name = "min_daily_demand")
    private Integer minDailyDemand;

    @Column(name = "max_daily_demand")
    private Integer maxDailyDemand;
}
```

### 4. Database Migration: V16__enhance_inventory_optimization.sql

```sql
-- Add OR fields to inventory table
ALTER TABLE inventory
ADD COLUMN annual_demand NUMERIC(10,2),
ADD COLUMN ordering_cost NUMERIC(10,2),
ADD COLUMN holding_cost NUMERIC(10,2),
ADD COLUMN lead_time_days INTEGER,
ADD COLUMN demand_std_dev NUMERIC(10,2),
ADD COLUMN service_level NUMERIC(4,3),
ADD COLUMN eoq NUMERIC(10,2),
ADD COLUMN reorder_point INTEGER,
ADD COLUMN safety_stock INTEGER,
ADD COLUMN abc_classification VARCHAR(1);

-- Create inventory analytics table for demand tracking
CREATE TABLE inventory_analytics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    inventory_id UUID NOT NULL REFERENCES inventory(id),
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    total_demand INTEGER NOT NULL CHECK (total_demand >= 0),
    avg_daily_demand NUMERIC(10,4) NOT NULL CHECK (avg_daily_demand >= 0),
    demand_std_dev NUMERIC(10,4) NOT NULL CHECK (demand_std_dev >= 0),
    min_daily_demand INTEGER,
    max_daily_demand INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT period_ordering CHECK (period_end >= period_start),
    CONSTRAINT min_max_demand CHECK (min_daily_demand <= max_daily_demand)
);

-- Indexes
CREATE INDEX idx_inventory_analytics_tenant ON inventory_analytics(tenant_id);
CREATE INDEX idx_inventory_analytics_inventory ON inventory_analytics(inventory_id, period_start DESC);
CREATE INDEX idx_inventory_eoq ON inventory(eoq) WHERE eoq IS NOT NULL;
CREATE INDEX idx_inventory_reorder_point ON inventory(reorder_point)
    WHERE current_stock <= reorder_point;
CREATE INDEX idx_inventory_abc ON inventory(abc_classification)
    WHERE abc_classification IS NOT NULL;
```

### 5. Create REST API Endpoints

**File**: `clinic-backend/src/main/java/com/clinic/backend/controller/InventoryOptimizationController.java`

Endpoints:
- `GET /api/inventory/{itemId}/eoq` - Get EOQ for item
- `GET /api/inventory/{itemId}/rop` - Get ROP for item
- `POST /api/inventory/abc-analysis` - Trigger ABC classification
- `GET /api/inventory/abc/A` - Get all A items
- `GET /api/inventory/reorder-needed` - Items below ROP
- `GET /api/inventory/analytics/{itemId}` - Demand statistics

---

## Verification Checklist

### Mathematical Correctness
- [ ] EOQ calculation matches formula Q* = √(2DS/H)
- [ ] Total cost at Q* is less than at nearby values (verify curve)
- [ ] ROP calculation matches ROP = d·L + z·σ·√L
- [ ] Z-scores correct for service levels (95% = 1.645, 99% = 2.326)
- [ ] ABC classification follows 70-20-10 rule (±5% tolerance)
- [ ] All invariants validated in @PrePersist/@PreUpdate

### Functional Testing
- [ ] EOQ reduces total inventory cost vs. current ordering
- [ ] ROP prevents stockouts (97%+ no-stockout rate for 95% service level)
- [ ] Reorder triggers fire within 1 day of hitting ROP
- [ ] ABC analysis correctly classifies items
- [ ] A items get daily review, C items weekly review

### Performance Testing
- [ ] EOQ calculation < 10ms
- [ ] ROP calculation < 10ms
- [ ] ABC analysis completes < 2 seconds (even with 1000+ items)
- [ ] Daily reorder check completes < 5 seconds
- [ ] Analytics queries return < 200ms

### Edge Cases
- [ ] Handle zero annual demand (EOQ = 0, avoid division by zero)
- [ ] Handle single item demand (σ = 0 for new items)
- [ ] Handle missing OR parameters (use defaults safely)
- [ ] Handle lead time = 0 (same-day delivery)
- [ ] Handle demand spikes (utilization tracking)

---

## Success Metrics

After deployment, measure:

| Metric | Target | Validation |
|--------|--------|-----------|
| Stockout frequency | <2% | Count items out of stock |
| Inventory carrying cost | -15-20% reduction | Compare before/after costs |
| Reorder accuracy | Orders within 2 days of ROP | Audit purchase orders |
| ABC classification | 70-20-10 split | Verify percentages |
| A item availability | >99% | Track fulfillment |
| C item carrying cost | Minimize | Review holding costs |

---

## Integration with Prescription Feature

This feature is a **prerequisite** for Prescription Enhancement (Feature 3).

When prescriptions are dispensed, inventory will be reduced atomically:
```
@Transactional
public void dispensePrescription(Prescription rx) {
    // Check: current_stock >= prescribed_quantity
    // Dispense: current_stock -= prescribed_quantity
    // Create: InventoryTransaction(SALE, quantity)
    // Mark: rx.status = DISPENSED
}
```

---

## Implementation Order

1. **Day 1**: Enhance Inventory entity with OR fields and calculations
2. **Day 2**: Create InventoryOptimizationService with all algorithms
3. **Day 3**: Create InventoryAnalytics entity and migration
4. **Day 4**: Create REST API endpoints
5. **Day 5**: Add scheduled job for daily reorder check

---

## References

- CLAUDE.md: Operations Research - Inventory Management (lines 213-310)
- CLAUDE.md: Discrete Mathematics - Invariants (lines 462-520)
- Existing: InventoryService.java (basic CRUD pattern)
- Existing: InventoryTransactionService.java (transaction tracking)

---

**Next Feature**: Prescription Enhancement (03-prescription-enhancement.md)
**Previous Feature**: Queue/Token Management (01-queue-token-management.md)
