package com.clinic.backend.dto.compliance;

import com.clinic.common.enums.ComplianceMetricType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for individual SLA violation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceViolationDTO {
    private String violationId;
    private ComplianceMetricType metricType;
    private LocalDate violationDate;
    private double complianceRate;
    private double upperControlLimit;
    private double lowerControlLimit;
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    private String description;
}
