package com.clinic.backend.controller;

import com.clinic.backend.dto.inventory.EOQCalculationDTO;
import com.clinic.backend.dto.inventory.ReorderPointDTO;
import com.clinic.backend.dto.inventory.ABCAnalysisDTO;
import com.clinic.backend.dto.inventory.InventoryAnalyticsDTO;
import com.clinic.backend.service.InventoryOptimizationService;
import com.clinic.backend.service.InventoryService;
import com.clinic.common.entity.operational.Inventory;
import com.clinic.common.entity.operational.InventoryAnalytics;
import com.clinic.common.enums.ABCClassification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API Controller for Inventory Optimization endpoints.
 *
 * Provides endpoints for:
 * - Economic Order Quantity (EOQ) calculation and analysis
 * - Reorder Point (ROP) calculation with safety stock
 * - ABC Analysis for inventory classification
 * - Inventory Analytics and demand statistics
 *
 * All endpoints support multi-tenancy with tenant_id filtering.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/inventory/optimization")
@RequiredArgsConstructor
@PreAuthorize("@tenantValidator.isValidTenant(#tenantId)")
public class InventoryOptimizationController {

    private final InventoryOptimizationService optimizationService;
    private final InventoryService inventoryService;

    // ======================== EOQ ENDPOINTS ========================

    /**
     * Calculate Economic Order Quantity for a specific item.
     *
     * GET /api/v1/tenants/{tenantId}/inventory/optimization/eoq/{itemId}
     *
     * @param tenantId The tenant ID
     * @param itemId The inventory item ID
     * @return EOQCalculationDTO with detailed cost analysis
     */
    @GetMapping("/eoq/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<EOQCalculationDTO> calculateEOQ(
            @PathVariable UUID tenantId,
            @PathVariable UUID itemId) {
        log.debug("Calculating EOQ for item: {} in tenant: {}", itemId, tenantId);

        Inventory item = inventoryService.getInventoryItemById(itemId, tenantId);

        try {
            Double eoq = optimizationService.calculateEOQ(item);
            Map<String, BigDecimal> costs = optimizationService.calculateEOQWithCosts(item);

            double ordersPerYear = item.getAnnualDemand() / eoq;
            double avgInventory = eoq / 2.0;
            double orderFrequencyDays = 365.0 / ordersPerYear;

            EOQCalculationDTO dto = EOQCalculationDTO.builder()
                .itemId(item.getId())
                .itemName(item.getItemName())
                .annualDemand(item.getAnnualDemand())
                .orderingCost(item.getOrderingCost())
                .holdingCost(item.getHoldingCost())
                .economicOrderQuantity(eoq)
                .ordersPerYear(ordersPerYear)
                .averageInventory(avgInventory)
                .annualOrderingCost(costs.get("annual_ordering_cost"))
                .annualHoldingCost(costs.get("annual_holding_cost"))
                .totalInventoryCost(costs.get("total_inventory_cost"))
                .currentStock(item.getCurrentStock())
                .recommendedOrderFrequencyDays(orderFrequencyDays)
                .build();

            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("EOQ calculation failed for item {}: {}", itemId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ======================== ROP ENDPOINTS ========================

    /**
     * Calculate Reorder Point with safety stock for a specific item.
     *
     * GET /api/v1/tenants/{tenantId}/inventory/optimization/rop/{itemId}
     *
     * @param tenantId The tenant ID
     * @param itemId The inventory item ID
     * @return ReorderPointDTO with ROP, safety stock, and reorder status
     */
    @GetMapping("/rop/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ReorderPointDTO> calculateReorderPoint(
            @PathVariable UUID tenantId,
            @PathVariable UUID itemId) {
        log.debug("Calculating ROP for item: {} in tenant: {}", itemId, tenantId);

        Inventory item = inventoryService.getInventoryItemById(itemId, tenantId);

        try {
            Integer rop = optimizationService.calculateReorderPoint(item);
            Map<String, Object> details = optimizationService.calculateReorderPointDetails(item);

            double avgDailyDemand = item.getAnnualDemand() / 365.0;
            int unitsBelowROP = item.getCurrentStock() - rop;

            ReorderPointDTO dto = ReorderPointDTO.builder()
                .itemId(item.getId())
                .itemName(item.getItemName())
                .annualDemand(item.getAnnualDemand())
                .averageDailyDemand(avgDailyDemand)
                .leadTimeDays(item.getLeadTimeDays())
                .demandStdDev(item.getDemandStdDev())
                .serviceLevel(item.getServiceLevel())
                .zScore((Double) details.get("z_score"))
                .leadTimeDemand((Integer) details.get("lead_time_demand"))
                .safetyStock((Integer) details.get("safety_stock"))
                .reorderPoint(rop)
                .currentStock(item.getCurrentStock())
                .unitsBelowROP(unitsBelowROP)
                .reorderNeeded(item.getCurrentStock() <= rop)
                .build();

            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("ROP calculation failed for item {}: {}", itemId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ======================== ABC ANALYSIS ENDPOINTS ========================

    /**
     * Perform ABC Classification analysis on all inventory items for a tenant.
     *
     * POST /api/v1/tenants/{tenantId}/inventory/optimization/abc-analysis
     *
     * Trigger ABC analysis which classifies all items by cumulative annual value:
     * - A items: Top 70% of value (~20% of items) - Tight control
     * - B items: Next 20% of value (~30% of items) - Moderate control
     * - C items: Bottom 10% of value (~50% of items) - Loose control
     *
     * @param tenantId The tenant ID
     * @return HTTP 202 Accepted (analysis runs in background)
     */
    @PostMapping("/abc-analysis")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> performABCAnalysis(
            @PathVariable UUID tenantId) {
        log.info("Triggering ABC analysis for tenant: {}", tenantId);

        try {
            optimizationService.performABCAnalysis(tenantId);
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        } catch (IllegalArgumentException e) {
            log.warn("ABC analysis failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get all items with a specific ABC classification.
     *
     * GET /api/v1/tenants/{tenantId}/inventory/optimization/abc/{classification}
     *
     * @param tenantId The tenant ID
     * @param classification The ABC classification (A, B, or C)
     * @param pageable Pagination parameters
     * @return Page of ABCAnalysisDTO
     */
    @GetMapping("/abc/{classification}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Page<ABCAnalysisDTO>> getItemsByClassification(
            @PathVariable UUID tenantId,
            @PathVariable String classification,
            Pageable pageable) {
        log.debug("Getting {} classified items for tenant: {}", classification, tenantId);

        try {
            ABCClassification abcClass = ABCClassification.valueOf(classification.toUpperCase());
            List<Inventory> items = optimizationService.getItemsByClassification(tenantId, abcClass);

            List<ABCAnalysisDTO> dtos = items.stream()
                .map(item -> mapToABCAnalysisDTO(item, abcClass))
                .collect(Collectors.toList());

            Page<ABCAnalysisDTO> page = new PageImpl<>(dtos, pageable, dtos.size());
            return ResponseEntity.ok(page);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid ABC classification: {}", classification);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get items that are currently below reorder point and need ordering.
     *
     * GET /api/v1/tenants/{tenantId}/inventory/optimization/reorder-needed
     *
     * @param tenantId The tenant ID
     * @param pageable Pagination parameters
     * @return Page of items below ROP
     */
    @GetMapping("/reorder-needed")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Page<ReorderPointDTO>> getItemsNeedingReorder(
            @PathVariable UUID tenantId,
            Pageable pageable) {
        log.debug("Getting items needing reorder for tenant: {}", tenantId);

        List<Inventory> items = optimizationService.getItemsNeedingReorder(tenantId);

        List<ReorderPointDTO> dtos = items.stream()
            .map(item -> {
                try {
                    Integer rop = optimizationService.calculateReorderPoint(item);
                    Map<String, Object> details = optimizationService.calculateReorderPointDetails(item);

                    double avgDailyDemand = item.getAnnualDemand() / 365.0;

                    return ReorderPointDTO.builder()
                        .itemId(item.getId())
                        .itemName(item.getItemName())
                        .annualDemand(item.getAnnualDemand())
                        .averageDailyDemand(avgDailyDemand)
                        .leadTimeDays(item.getLeadTimeDays())
                        .demandStdDev(item.getDemandStdDev())
                        .serviceLevel(item.getServiceLevel())
                        .zScore((Double) details.get("z_score"))
                        .leadTimeDemand((Integer) details.get("lead_time_demand"))
                        .safetyStock((Integer) details.get("safety_stock"))
                        .reorderPoint(rop)
                        .currentStock(item.getCurrentStock())
                        .unitsBelowROP(item.getCurrentStock() - rop)
                        .reorderNeeded(true)
                        .build();
                } catch (IllegalArgumentException | IllegalStateException e) {
                    log.warn("Failed to calculate ROP for item {}: {}", item.getId(), e.getMessage());
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        Page<ReorderPointDTO> page = new PageImpl<>(dtos, pageable, dtos.size());
        return ResponseEntity.ok(page);
    }

    // ======================== ANALYTICS ENDPOINTS ========================

    /**
     * Get demand analytics for a specific inventory item.
     *
     * GET /api/v1/tenants/{tenantId}/inventory/optimization/analytics/{itemId}
     *
     * Returns the most recent analytics entry with demand statistics.
     *
     * @param tenantId The tenant ID
     * @param itemId The inventory item ID
     * @return InventoryAnalyticsDTO or 404 if not found
     */
    @GetMapping("/analytics/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<InventoryAnalyticsDTO> getAnalytics(
            @PathVariable UUID tenantId,
            @PathVariable UUID itemId) {
        log.debug("Getting analytics for item: {} in tenant: {}", itemId, tenantId);

        // Verify item exists
        Inventory item = inventoryService.getInventoryItemById(itemId, tenantId);

        Optional<InventoryAnalytics> analytics = optimizationService.getAnalytics(itemId, tenantId);

        if (analytics.isPresent()) {
            InventoryAnalyticsDTO dto = mapToAnalyticsDTO(analytics.get());
            return ResponseEntity.ok(dto);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get historical analytics for a specific inventory item.
     *
     * GET /api/v1/tenants/{tenantId}/inventory/optimization/analytics/{itemId}/history
     *
     * @param tenantId The tenant ID
     * @param itemId The inventory item ID
     * @param pageable Pagination parameters
     * @return Page of InventoryAnalyticsDTO ordered by most recent first
     */
    @GetMapping("/analytics/{itemId}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Page<InventoryAnalyticsDTO>> getAnalyticsHistory(
            @PathVariable UUID tenantId,
            @PathVariable UUID itemId,
            Pageable pageable) {
        log.debug("Getting analytics history for item: {} in tenant: {}", itemId, tenantId);

        // Verify item exists
        Inventory item = inventoryService.getInventoryItemById(itemId, tenantId);

        List<InventoryAnalytics> analyticsList = optimizationService.getAnalyticsHistory(itemId, tenantId);

        List<InventoryAnalyticsDTO> dtos = analyticsList.stream()
            .map(this::mapToAnalyticsDTO)
            .collect(Collectors.toList());

        Page<InventoryAnalyticsDTO> page = new PageImpl<>(dtos, pageable, dtos.size());
        return ResponseEntity.ok(page);
    }

    // ======================== HELPER METHODS ========================

    /**
     * Map Inventory entity to ABCAnalysisDTO.
     */
    private ABCAnalysisDTO mapToABCAnalysisDTO(Inventory item, ABCClassification classification) {
        BigDecimal annualValue = item.calculateAnnualValue();

        String controlStrategy = switch (classification) {
            case A -> "Tight control, daily review, exact records, frequent orders";
            case B -> "Moderate control, weekly review, standard procedures, periodic orders";
            case C -> "Loose control, monthly review, simplified procedures, bulk orders";
        };

        String reviewFrequency = switch (classification) {
            case A -> "Daily";
            case B -> "Weekly";
            case C -> "Monthly";
        };

        double serviceLevel = classification.getRecommendedServiceLevel();

        return ABCAnalysisDTO.builder()
            .itemId(item.getId())
            .itemName(item.getItemName())
            .annualDemand(item.getAnnualDemand())
            .unitPrice(item.getUnitPrice())
            .annualValue(annualValue)
            .classification(classification)
            .recommendedControlStrategy(controlStrategy)
            .recommendedReviewFrequency(reviewFrequency)
            .recommendedServiceLevel(serviceLevel)
            .build();
    }

    /**
     * Map InventoryAnalytics entity to InventoryAnalyticsDTO.
     */
    private InventoryAnalyticsDTO mapToAnalyticsDTO(InventoryAnalytics analytics) {
        Double cv = analytics.getCoefficientOfVariation();
        String demandStability = "Unknown";

        if (cv != null) {
            if (cv < 0.5) {
                demandStability = "Stable";
            } else if (cv < 1.0) {
                demandStability = "Moderate";
            } else {
                demandStability = "High Variability";
            }
        }

        return InventoryAnalyticsDTO.builder()
            .analyticsId(analytics.getId())
            .inventoryId(analytics.getInventory().getId())
            .itemName(analytics.getInventory().getItemName())
            .periodStart(analytics.getPeriodStart())
            .periodEnd(analytics.getPeriodEnd())
            .periodDays(analytics.getPeriodDays())
            .totalDemand(analytics.getTotalDemand())
            .avgDailyDemand(analytics.getAvgDailyDemand())
            .demandStdDev(analytics.getDemandStdDev())
            .minDailyDemand(analytics.getMinDailyDemand())
            .maxDailyDemand(analytics.getMaxDailyDemand())
            .demandRange(analytics.getMaxDailyDemand() != null && analytics.getMinDailyDemand() != null
                ? analytics.getMaxDailyDemand() - analytics.getMinDailyDemand()
                : null)
            .coefficientOfVariation(cv)
            .demandStability(demandStability)
            .isStableDemand(analytics.isStableDemand())
            .isHighVariabilityDemand(analytics.isHighlyVariableDemand())
            .build();
    }
}
