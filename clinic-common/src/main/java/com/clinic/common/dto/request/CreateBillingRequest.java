package com.clinic.common.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for creating a new billing record.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBillingRequest {

    @NotNull(message = "Patient ID is required")
    private UUID patientId;

    private UUID appointmentId;

    @NotBlank(message = "Invoice number is required")
    @Size(max = 50, message = "Invoice number must not exceed 50 characters")
    private String invoiceNumber;

    private LocalDate invoiceDate;

    @NotNull(message = "Subtotal is required")
    @DecimalMin(value = "0.0", message = "Subtotal cannot be negative")
    private BigDecimal subtotal;

    @DecimalMin(value = "0.0", message = "Discount cannot be negative")
    private BigDecimal discountAmount;

    @DecimalMin(value = "0.0", message = "Tax cannot be negative")
    private BigDecimal taxAmount;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.0", message = "Total cannot be negative")
    private BigDecimal totalAmount;

    @NotNull(message = "Line items are required")
    private Map<String, Object> lineItems;
}
