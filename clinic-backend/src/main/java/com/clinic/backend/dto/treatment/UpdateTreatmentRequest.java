package com.clinic.backend.dto.treatment;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTreatmentRequest {

    @Size(max = 255, message = "Treatment name cannot exceed 255 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @Size(max = 100, message = "Category cannot exceed 100 characters")
    private String category;

    @DecimalMin(value = "0.0", message = "Base cost cannot be negative")
    private BigDecimal baseCost;

    @DecimalMin(value = "0.0", message = "Discount percentage cannot be negative")
    @DecimalMax(value = "100.0", message = "Discount percentage cannot exceed 100")
    private BigDecimal discountPercentage;

    @Min(value = 0, message = "Duration cannot be negative")
    private Integer durationMinutes;

    @Size(max = 500, message = "Instructions cannot exceed 500 characters")
    private String instructions;

    @Size(max = 500, message = "Prerequisites cannot exceed 500 characters")
    private String prerequisites;

    private UUID branchId;

    private Boolean isActive;
}
