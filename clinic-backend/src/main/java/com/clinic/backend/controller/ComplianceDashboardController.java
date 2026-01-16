package com.clinic.backend.controller;

import com.clinic.backend.dto.compliance.ComplianceDashboardDTO;
import com.clinic.backend.dto.compliance.ComplianceViolationDTO;
import com.clinic.backend.repository.ComplianceMetricsRepository;
import com.clinic.backend.security.SecurityUtils;
import com.clinic.common.entity.compliance.ComplianceMetrics;
import com.clinic.common.enums.ComplianceMetricType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST Controller for Compliance Dashboard and SLA Reporting
 *
 * Provides endpoints for monitoring compliance metrics and SLA violations
 * ISO 27001 A.18 (Compliance) aligned monitoring
 *
 * Security: Requires ADMIN or COMPLIANCE_OFFICER role
 */
@RestController
@RequestMapping("/api/compliance")
@RequiredArgsConstructor
@Slf4j
public class ComplianceDashboardController {

    private final ComplianceMetricsRepository complianceMetricsRepository;
    private final SecurityUtils securityUtils;

    /**
     * GET /api/compliance/dashboard?days=30
     * Get compliance dashboard summary for last N days
     *
     * @param days Number of days to analyze (default: 30)
     * @return Compliance dashboard DTO with summary and violations
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ComplianceDashboardDTO> getDashboard(
            @RequestParam(defaultValue = "30") int days) {

        UUID tenantId = securityUtils.getCurrentTenantId();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        log.info("Fetching compliance dashboard for tenant {} for last {} days", tenantId, days);

        try {
            List<ComplianceMetrics> metrics = complianceMetricsRepository
                .findByDateRange(tenantId, startDate, endDate);

            if (metrics.isEmpty()) {
                return ResponseEntity.ok(ComplianceDashboardDTO.builder()
                    .daysAnalyzed(0)
                    .averageComplianceRate(0.0)
                    .totalViolations(0)
                    .violationsDays(0)
                    .metricsSummary(new HashMap<>())
                    .recentViolations(new ArrayList<>())
                    .build());
            }

            // Calculate summary
            double avgCompliance = metrics.stream()
                .mapToDouble(m -> m.getComplianceRate() != null ? m.getComplianceRate() : 100.0)
                .average()
                .orElse(100.0);

            long violations = metrics.stream()
                .filter(m -> Boolean.TRUE.equals(m.getOutOfControl()))
                .count();

            // Build metric summaries
            Map<String, ComplianceDashboardDTO.ComplianceMetricSummary> metricsSummary = metrics.stream()
                .collect(Collectors.groupingBy(
                    m -> m.getMetricType().toString(),
                    Collectors.collectingAndThen(
                        Collectors.toList(),
                        list -> ComplianceDashboardDTO.ComplianceMetricSummary.builder()
                            .metricType(list.get(0).getMetricType().toString())
                            .currentRate(list.get(0).getComplianceRate() != null ?
                                list.get(0).getComplianceRate() : 100.0)
                            .averageRate(list.stream()
                                .mapToDouble(m -> m.getComplianceRate() != null ? m.getComplianceRate() : 100.0)
                                .average()
                                .orElse(100.0))
                            .minRate(list.stream()
                                .mapToDouble(m -> m.getComplianceRate() != null ? m.getComplianceRate() : 100.0)
                                .min()
                                .orElse(100.0))
                            .violations(list.stream()
                                .filter(m -> Boolean.TRUE.equals(m.getOutOfControl()))
                                .count())
                            .outOfControl(list.stream()
                                .anyMatch(m -> Boolean.TRUE.equals(m.getOutOfControl())))
                            .build()
                    )
                ));

            // Build recent violations list
            List<ComplianceViolationDTO> recentViolations = metrics.stream()
                .filter(m -> Boolean.TRUE.equals(m.getOutOfControl()))
                .sorted(Comparator.comparing(ComplianceMetrics::getMetricDate).reversed())
                .limit(5)
                .map(m -> ComplianceViolationDTO.builder()
                    .violationId(m.getId().toString())
                    .metricType(m.getMetricType())
                    .violationDate(m.getMetricDate())
                    .complianceRate(m.getComplianceRate() != null ? m.getComplianceRate() : 0.0)
                    .upperControlLimit(m.getUpperControlLimit())
                    .lowerControlLimit(m.getLowerControlLimit())
                    .severity(determineSeverity(m.getComplianceRate()))
                    .build())
                .collect(Collectors.toList());

            ComplianceDashboardDTO dashboard = ComplianceDashboardDTO.builder()
                .daysAnalyzed(days)
                .averageComplianceRate(avgCompliance)
                .totalViolations(metrics.size())
                .violationsDays(violations)
                .metricsSummary(metricsSummary)
                .recentViolations(recentViolations)
                .build();

            return ResponseEntity.ok(dashboard);

        } catch (Exception e) {
            log.error("Error fetching compliance dashboard: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/compliance/violations?days=7
     * Get recent SLA violations
     *
     * @param days Number of days to look back
     * @return List of compliance violations
     */
    @GetMapping("/violations")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<List<ComplianceViolationDTO>> getViolations(
            @RequestParam(defaultValue = "7") int days) {

        UUID tenantId = securityUtils.getCurrentTenantId();
        LocalDate startDate = LocalDate.now().minusDays(days);

        log.info("Fetching compliance violations for tenant {} since {}", tenantId, startDate);

        try {
            List<ComplianceMetrics> violations = complianceMetricsRepository
                .findOutOfControlMetrics(tenantId, startDate);

            List<ComplianceViolationDTO> dtos = violations.stream()
                .map(m -> ComplianceViolationDTO.builder()
                    .violationId(m.getId().toString())
                    .metricType(m.getMetricType())
                    .violationDate(m.getMetricDate())
                    .complianceRate(m.getComplianceRate() != null ? m.getComplianceRate() : 0.0)
                    .upperControlLimit(m.getUpperControlLimit())
                    .lowerControlLimit(m.getLowerControlLimit())
                    .severity(determineSeverity(m.getComplianceRate()))
                    .description(buildViolationDescription(m))
                    .build())
                .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            log.error("Error fetching compliance violations: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/compliance/metrics/{type}?startDate=2025-01-01&endDate=2025-01-31
     * Get specific metric type history
     *
     * @param type Metric type
     * @param startDate Range start
     * @param endDate Range end
     * @return List of metrics for type
     */
    @GetMapping("/metrics/{type}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<List<ComplianceMetrics>> getMetricsByType(
            @PathVariable String type,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        UUID tenantId = securityUtils.getCurrentTenantId();
        LocalDate actualStart = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate actualEnd = endDate != null ? endDate : LocalDate.now();

        log.info("Fetching metrics for type {} for tenant {} from {} to {}",
            type, tenantId, actualStart, actualEnd);

        try {
            ComplianceMetricType metricType = ComplianceMetricType.valueOf(type);
            List<ComplianceMetrics> metrics = complianceMetricsRepository
                .findRecentByType(tenantId, metricType,
                    (int) java.time.temporal.ChronoUnit.DAYS.between(actualStart, actualEnd));

            return ResponseEntity.ok(metrics);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid metric type: {}", type);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error fetching metrics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Determine severity based on compliance rate
     */
    private String determineSeverity(Double complianceRate) {
        if (complianceRate == null) return "CRITICAL";
        if (complianceRate >= 95.0) return "LOW";
        if (complianceRate >= 90.0) return "MEDIUM";
        if (complianceRate >= 85.0) return "HIGH";
        return "CRITICAL";
    }

    /**
     * Build human-readable violation description
     */
    private String buildViolationDescription(ComplianceMetrics metric) {
        return String.format("%s metric out of control: %.2f%% compliance (limits: %.2f%% - %.2f%%)",
            metric.getMetricType(),
            metric.getComplianceRate() != null ? metric.getComplianceRate() : 0.0,
            metric.getLowerControlLimit() != null ? metric.getLowerControlLimit() : 0.0,
            metric.getUpperControlLimit() != null ? metric.getUpperControlLimit() : 100.0);
    }
}
