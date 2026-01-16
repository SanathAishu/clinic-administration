package com.clinic.backend.service;

import com.clinic.common.entity.operational.Inventory;
import com.clinic.common.entity.operational.InventoryAnalytics;
import com.clinic.common.enums.ABCClassification;
import com.clinic.backend.repository.InventoryRepository;
import com.clinic.backend.repository.InventoryAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Inventory Optimization Service - Implements Operations Research principles.
 *
 * Provides scientific inventory management using:
 * - Economic Order Quantity (EOQ) Model: Minimize total inventory cost
 * - Reorder Point (ROP): Prevent stockouts with safety stock
 * - ABC Analysis: Classify items by value for differentiated control
 *
 * Mathematical Foundation:
 *
 * Theorem 8 (EOQ):
 * The optimal order quantity minimizing total cost is: Q* = √(2DS/H)
 * Proof: dTC/dQ = -DS/Q² + H/2 = 0 ⟹ Q* = √(2DS/H)
 * d²TC/dQ² = 2DS/Q³ > 0 ⟹ Q* is minimum ✓
 *
 * Theorem 9 (ROP with Safety Stock):
 * For service level α, reorder point is: ROP = d·L + z·σ·√L
 * Where demand during lead time ~ Normal(d·L, σ²·L)
 *
 * Theorem 10 (ABC Analysis - Pareto Principle):
 * A items (70% value, ~20% items) require tight control
 * B items (20% value, ~30% items) require moderate control
 * C items (10% value, ~50% items) allow loose control
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryOptimizationService {

    private final InventoryRepository inventoryRepository;
    private final InventoryAnalyticsRepository analyticsRepository;
    private final InventoryTransactionService transactionService;

    /**
     * Calculate Economic Order Quantity using formula Q* = √(2DS/H)
     *
     * Mathematical Proof (Theorem 8):
     * Total Cost: TC(Q) = (D/Q)·S + (Q/2)·H + D·P
     * Minimize with respect to Q:
     *   dTC/dQ = -D·S/Q² + H/2
     *   Setting to 0: D·S/Q² = H/2
     *   Solving: Q² = 2DS/H
     *   Therefore: Q* = √(2DS/H)
     *
     * Second derivative test:
     *   d²TC/dQ² = 2DS/Q³ > 0 (always positive)
     *   ∴ Q* is a minimum (not maximum) ✓
     *
     * Interpretation:
     * At Q*, ordering cost = holding cost = TC/2 (cost balance)
     * Q* represents the "sweet spot" between:
     * - Ordering frequently (high ordering cost)
     * - Holding large quantities (high holding cost)
     *
     * @param inventory The inventory item with OR parameters
     * @return EOQ value (optimal order quantity in units)
     * @throws IllegalArgumentException if required parameters are null
     * @throws IllegalStateException if EOQ calculation violates invariants
     */
    @Cacheable(value = "eoq_cache", key = "#inventory.id", unless = "#result == null")
    public Double calculateEOQ(Inventory inventory) {
        if (inventory.getAnnualDemand() == null ||
            inventory.getOrderingCost() == null ||
            inventory.getHoldingCost() == null) {
            throw new IllegalArgumentException(
                "EOQ calculation requires annualDemand, orderingCost, and holdingCost"
            );
        }

        double d = inventory.getAnnualDemand();
        double s = inventory.getOrderingCost().doubleValue();
        double h = inventory.getHoldingCost().doubleValue();

        if (d <= 0 || h <= 0) {
            throw new IllegalStateException(
                "EOQ requires positive demand and holding cost. D=" + d + ", H=" + h
            );
        }

        // Q* = √(2DS/H)
        double eoq = Math.sqrt((2 * d * s) / h);

        // Invariant: EOQ > 0
        if (eoq <= 0) {
            throw new IllegalStateException("EOQ must be positive, got: " + eoq);
        }

        log.debug("Calculated EOQ for item {}: {} units", inventory.getItemName(), eoq);
        return eoq;
    }

    /**
     * Calculate EOQ with cost breakdown for analysis.
     *
     * @param inventory The inventory item
     * @return Map with keys: "eoq", "orders_per_year", "avg_inventory",
     *                        "ordering_cost", "holding_cost", "total_cost"
     */
    public Map<String, BigDecimal> calculateEOQWithCosts(Inventory inventory) {
        Double eoq = calculateEOQ(inventory);
        Double d = inventory.getAnnualDemand();
        BigDecimal s = inventory.getOrderingCost();
        BigDecimal h = inventory.getHoldingCost();

        Map<String, BigDecimal> costs = new LinkedHashMap<>();

        // EOQ
        costs.put("eoq", BigDecimal.valueOf(eoq));

        // Orders per year: D / Q*
        double ordersPerYear = d / eoq;
        costs.put("orders_per_year", BigDecimal.valueOf(ordersPerYear));

        // Average inventory: Q* / 2
        double avgInventory = eoq / 2.0;
        costs.put("average_inventory", BigDecimal.valueOf(avgInventory));

        // Annual ordering cost: (D/Q*) × S
        BigDecimal annualOrderingCost = s.multiply(BigDecimal.valueOf(ordersPerYear));
        costs.put("annual_ordering_cost", annualOrderingCost);

        // Annual holding cost: (Q*/2) × H
        BigDecimal annualHoldingCost = h.multiply(BigDecimal.valueOf(avgInventory));
        costs.put("annual_holding_cost", annualHoldingCost);

        // Total cost (should be equal at optimality)
        BigDecimal totalCost = annualOrderingCost.add(annualHoldingCost);
        costs.put("total_inventory_cost", totalCost);

        return costs;
    }

    /**
     * Calculate Reorder Point with safety stock.
     *
     * Mathematical Proof (Theorem 9):
     * During lead time, demand follows normal distribution:
     *   Demand_L ~ Normal(d·L, σ²·L)
     *   where d = avg daily demand, L = lead time days, σ = demand std dev
     *
     * For service level α (e.g., 95% no-stockout probability):
     *   P(Demand ≤ ROP) = α
     *   ROP = d·L + z·σ·√L
     *
     * Where:
     *   d·L = expected demand during lead time (mean)
     *   z = z-score from standard normal (inverse CDF at α)
     *   σ·√L = standard deviation of lead time demand
     *   z·σ·√L = safety stock buffer
     *
     * Common z-scores:
     *   90% service level: z = 1.282
     *   95% service level: z = 1.645
     *   99% service level: z = 2.326
     *   99.9% service level: z = 3.090
     *
     * @param inventory The inventory item with ROP parameters
     * @return Reorder Point (units)
     * @throws IllegalArgumentException if required parameters are null
     * @throws IllegalStateException if ROP violates invariants
     */
    @Cacheable(value = "rop_cache", key = "#inventory.id", unless = "#result == null")
    public Integer calculateReorderPoint(Inventory inventory) {
        if (inventory.getAnnualDemand() == null ||
            inventory.getLeadTimeDays() == null ||
            inventory.getDemandStdDev() == null ||
            inventory.getServiceLevel() == null) {
            throw new IllegalArgumentException(
                "ROP calculation requires annualDemand, leadTimeDays, demandStdDev, and serviceLevel"
            );
        }

        double dailyDemand = inventory.getAnnualDemand() / 365.0;
        double z = getZScore(inventory.getServiceLevel());
        int l = inventory.getLeadTimeDays();
        double sigma = inventory.getDemandStdDev();

        // Expected demand during lead time: d·L
        int leadTimeDemand = (int) Math.ceil(dailyDemand * l);

        // Safety stock: z × σ × √L
        int safetyStock = (int) Math.ceil(z * sigma * Math.sqrt(l));

        // ROP = d·L + SS
        int rop = leadTimeDemand + safetyStock;

        // Invariant: ROP ≥ 0
        if (rop < 0) {
            throw new IllegalStateException("ROP must be non-negative, got: " + rop);
        }

        log.debug("Calculated ROP for item {}: {} units (SS: {})",
            inventory.getItemName(), rop, safetyStock);

        return rop;
    }

    /**
     * Get ROP breakdown with detailed components.
     *
     * @param inventory The inventory item
     * @return Map with keys: "lead_time_demand", "safety_stock", "reorder_point", "z_score"
     */
    public Map<String, Object> calculateReorderPointDetails(Inventory inventory) {
        double dailyDemand = inventory.getAnnualDemand() / 365.0;
        double z = getZScore(inventory.getServiceLevel());
        int l = inventory.getLeadTimeDays();
        double sigma = inventory.getDemandStdDev();

        int leadTimeDemand = (int) Math.ceil(dailyDemand * l);
        int safetyStock = (int) Math.ceil(z * sigma * Math.sqrt(l));
        int rop = leadTimeDemand + safetyStock;

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("lead_time_demand", leadTimeDemand);
        details.put("safety_stock", safetyStock);
        details.put("reorder_point", rop);
        details.put("z_score", z);
        details.put("daily_demand", dailyDemand);
        details.put("lead_time_days", l);
        details.put("demand_std_dev", sigma);

        return details;
    }

    /**
     * Perform ABC Classification analysis on all inventory items.
     *
     * Mathematical Basis (Theorem 10 - Pareto Principle):
     * Items are classified by cumulative annual value percentage:
     *
     * Algorithm:
     * 1. Calculate annual value per item: V[i] = D[i] × P[i]
     * 2. Sort items by value (descending)
     * 3. Calculate cumulative percentage:
     *    for each item (sorted):
     *      cumulativeValue += V[item]
     *      cumulativePercent = cumulativeValue / totalValue
     *      if cumulativePercent ≤ 0.70:
     *        classification = A (high value, tight control)
     *      elif cumulativePercent ≤ 0.90:
     *        classification = B (medium value, moderate control)
     *      else:
     *        classification = C (low value, loose control)
     *
     * Control Strategy Implications:
     * A items: Daily review, 95-99% service level, exact records, frequent orders
     * B items: Weekly review, 90% service level, standard procedures, periodic orders
     * C items: Monthly review, 75-80% service level, simplified procedures, bulk orders
     *
     * @param tenantId The tenant whose inventory to analyze
     * @throws IllegalArgumentException if tenant has no inventory items
     */
    @Transactional
    public void performABCAnalysis(UUID tenantId) {
        log.info("Starting ABC analysis for tenant: {}", tenantId);

        List<Inventory> items = inventoryRepository.findByTenantIdAndDeletedAtIsNull(tenantId, Pageable.unpaged()).getContent();

        if (items.isEmpty()) {
            log.warn("No inventory items found for tenant: {}", tenantId);
            return;
        }

        // Calculate annual value per item: V[i] = annualDemand[i] × unitPrice[i]
        List<InventoryValueItem> valueItems = items.stream()
            .filter(item -> item.getAnnualDemand() != null && item.getUnitPrice() != null)
            .map(item -> new InventoryValueItem(
                item.getId(),
                item,
                item.calculateAnnualValue()
            ))
            .sorted(Comparator.comparing(iv -> iv.value, (a, b) -> b.compareTo(a)))
            .collect(Collectors.toList());

        BigDecimal totalValue = valueItems.stream()
            .map(iv -> iv.value)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("Total inventory value is zero for tenant: {}", tenantId);
            return;
        }

        // Classify items by cumulative value percentage
        BigDecimal cumulativeValue = BigDecimal.ZERO;
        int processedCount = 0;

        for (InventoryValueItem iv : valueItems) {
            cumulativeValue = cumulativeValue.add(iv.value);
            double cumulativePercent = cumulativeValue.doubleValue() / totalValue.doubleValue();

            // Determine classification
            ABCClassification classification;
            if (cumulativePercent <= 0.70) {
                classification = ABCClassification.A;
            } else if (cumulativePercent <= 0.90) {
                classification = ABCClassification.B;
            } else {
                classification = ABCClassification.C;
            }

            // Update inventory item
            Inventory inventory = iv.inventory;
            inventory.setAbcClassification(classification);
            inventoryRepository.save(inventory);
            processedCount++;

            log.debug("Classified {}: {} as {} (cumulative: {:.1f}%)",
                inventory.getItemName(), iv.value, classification, cumulativePercent * 100);
        }

        log.info("ABC analysis completed for tenant {}: {} items classified",
            tenantId, processedCount);
    }

    /**
     * Check reorder points and log items needing reorder.
     * Scheduled to run daily at 8 AM.
     *
     * Checks all inventory items with ROP set and creates alerts
     * for items below reorder point.
     *
     * Invariants enforced:
     * - currentStock ≤ ROP ⟹ Reorder needed
     * - Reorder quantity = EOQ (or manual override)
     * - Only active (non-deleted) items checked
     */
    @Scheduled(cron = "0 0 8 * * *")  // Daily at 8:00 AM
    @Transactional
    public void checkReorderPoints() {
        log.info("Starting scheduled reorder point check");

        List<Inventory> items = inventoryRepository.findAll();

        int reorderCount = 0;
        for (Inventory item : items) {
            if (item.isDeleted()) {
                continue;
            }

            if (item.getReorderPoint() == null) {
                continue;
            }

            if (item.getCurrentStock() <= item.getReorderPoint()) {
                reorderCount++;

                int orderQuantity = item.getEconomicOrderQuantity() != null
                    ? item.getEconomicOrderQuantity().intValue()
                    : (int) Math.ceil(item.getAnnualDemand() / 12.0); // Fallback to monthly demand

                log.info("Reorder alert: {} | Current: {} | ROP: {} | Suggested Order: {} units",
                    item.getItemName(),
                    item.getCurrentStock(),
                    item.getReorderPoint(),
                    orderQuantity);

                // In future, create PurchaseOrder entity
                // For now, just log the alert
            }
        }

        log.info("Reorder point check complete. {} items below ROP", reorderCount);
    }

    /**
     * Get all items needing reorder (currentStock ≤ ROP).
     *
     * @param tenantId The tenant
     * @return List of items below reorder point
     */
    public List<Inventory> getItemsNeedingReorder(UUID tenantId) {
        List<Inventory> allItems = inventoryRepository.findByTenantIdAndDeletedAtIsNull(tenantId, Pageable.unpaged()).getContent();

        return allItems.stream()
            .filter(item -> item.getReorderPoint() != null &&
                          item.getCurrentStock() <= item.getReorderPoint())
            .sorted(Comparator.comparing(Inventory::getReorderPoint).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Get items by ABC classification.
     *
     * @param tenantId The tenant
     * @param classification The ABC classification (A, B, or C)
     * @return List of items with the specified classification
     */
    public List<Inventory> getItemsByClassification(UUID tenantId, ABCClassification classification) {
        List<Inventory> items = inventoryRepository.findByTenantIdAndDeletedAtIsNull(tenantId, Pageable.unpaged()).getContent();

        return items.stream()
            .filter(item -> classification.equals(item.getAbcClassification()))
            .sorted(Comparator.comparing(Inventory::calculateAnnualValue).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Get analytics for an inventory item.
     *
     * @param inventoryId The inventory item ID
     * @param tenantId The tenant
     * @return Most recent analytics entry
     */
    public Optional<InventoryAnalytics> getAnalytics(UUID inventoryId, UUID tenantId) {
        return analyticsRepository.findMostRecentByInventoryId(inventoryId, tenantId);
    }

    /**
     * Get all analytics for an inventory item.
     *
     * @param inventoryId The inventory item ID
     * @param tenantId The tenant
     * @return List of analytics entries (most recent first)
     */
    public List<InventoryAnalytics> getAnalyticsHistory(UUID inventoryId, UUID tenantId) {
        return analyticsRepository.findByInventoryIdAndTenantId(inventoryId, tenantId);
    }

    // ======================== PRIVATE HELPER METHODS ========================

    /**
     * Map service level to z-score (standard normal distribution).
     * Used in safety stock calculation: SS = z·σ·√L
     *
     * Mathematical basis:
     * For cumulative normal distribution: P(Z ≤ z) = serviceLevel
     * Where Z ~ Normal(0, 1)
     *
     * @param serviceLevel Service level (0.0 to 1.0)
     * @return Z-score for the service level
     */
    private double getZScore(double serviceLevel) {
        if (serviceLevel >= 0.999) return 3.090;
        if (serviceLevel >= 0.990) return 2.326;
        if (serviceLevel >= 0.950) return 1.645;
        if (serviceLevel >= 0.900) return 1.282;
        if (serviceLevel >= 0.750) return 0.674;
        return 1.645;  // default to 95%
    }

    /**
     * Helper class for ABC analysis value tracking.
     */
    private static class InventoryValueItem {
        UUID itemId;
        Inventory inventory;
        BigDecimal value;

        InventoryValueItem(UUID itemId, Inventory inventory, BigDecimal value) {
            this.itemId = itemId;
            this.inventory = inventory;
            this.value = value;
        }
    }
}
