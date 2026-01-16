package com.clinic.backend.dto.order;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrderRequest {

    @Size(max = 255, message = "Product name cannot exceed 255 characters")
    private String productName;

    @Size(max = 100, message = "Product type cannot exceed 100 characters")
    private String productType;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @DecimalMin(value = "0.0", message = "Unit price cannot be negative")
    private BigDecimal unitPrice;

    @Size(max = 255, message = "Manufacturer name cannot exceed 255 characters")
    private String manufacturerName;

    @Email(message = "Manufacturer email must be valid")
    @Size(max = 255, message = "Manufacturer email cannot exceed 255 characters")
    private String manufacturerEmail;

    @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$", message = "Manufacturer phone number must be valid")
    private String manufacturerPhone;

    private LocalDate expectedDeliveryDate;

    @Size(max = 500, message = "Special instructions cannot exceed 500 characters")
    private String specialInstructions;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;
}
