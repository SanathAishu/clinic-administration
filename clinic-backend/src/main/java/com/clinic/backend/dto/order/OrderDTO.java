package com.clinic.backend.dto.order;

import com.clinic.common.enums.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDTO {

    private UUID id;

    private UUID patientId;

    private String patientName;

    private String productName;

    private String productType;

    private String description;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal totalAmount;

    private String manufacturerName;

    private String manufacturerEmail;

    private String manufacturerPhone;

    private OrderStatus status;

    private LocalDate orderDate;

    private LocalDate expectedDeliveryDate;

    private LocalDate actualDeliveryDate;

    private String trackingNumber;

    private Instant sentAt;

    private Instant receivedAt;

    private Instant deliveredAt;

    private Instant patientNotifiedAt;

    private String specialInstructions;

    private String notes;

    private UUID branchId;

    private Instant createdAt;

    private Instant updatedAt;

    private Instant deletedAt;
}
