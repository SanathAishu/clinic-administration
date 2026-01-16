package com.clinic.backend.dto.compliance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for Compliance Dashboard - SLA monitoring summary
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceDashboardDTO {
    private int daysAnalyzed;
    private double averageComplianceRate;
    private long totalViolations;
    private long violationsDays;
    private Map<String, ComplianceMetricSummary> metricsSummary;
    private List<ComplianceViolationDTO> recentViolations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComplianceMetricSummary {
        private String metricType;
        private double currentRate;
        private double averageRate;
        private double minRate;
        private long violations;
        private boolean outOfControl;
    }
}
