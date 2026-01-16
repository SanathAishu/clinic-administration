package com.clinic.common.enums;

/**
 * Medical Order Status
 * Tracks the lifecycle of orders for braces, medical products, etc.
 */
public enum OrderStatus {
    PENDING,         // Order placed, not yet sent to manufacturer
    SENT,            // Order sent to manufacturer
    IN_PRODUCTION,   // Manufacturer is working on the order
    SHIPPED,         // Order shipped by manufacturer
    RECEIVED,        // Order received at clinic
    READY_FOR_PICKUP,// Ready for patient to collect
    DELIVERED,       // Delivered to patient
    CANCELLED        // Order cancelled
}
