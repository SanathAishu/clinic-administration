package com.clinic.backend.dto;

import com.clinic.common.enums.PrescriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO for prescription dispensing response
 *
 * Contains complete information about a dispensing operation:
 * - Prescription details (ID, patient, doctor, status)
 * - Dispensing result (timestamp, dispensed by, items dispensed)
 * - Inventory transactions (what was deducted from stock)
 * - Warnings (drug interactions, low stock alerts)
 *
 * Example response:
 * {
 *   "prescriptionId": "550e8400-e29b-41d4-a716-446655440000",
 *   "status": "DISPENSED",
 *   "dispensedAt": "2025-01-16T10:30:00Z",
 *   "dispensedBy": "John Doe",
 *   "patientName": "Jane Smith",
 *   "doctorName": "Dr. Johnson",
 *   "totalItemsDispensed": 3,
 *   "itemsDispensed": [
 *     {
 *       "medicationName": "Amoxicillin",
 *       "quantity": 30,
 *       "dosageUnit": "tablet"
 *     }
 *   ],
 *   "inventoryTransactionsCreated": 3,
 *   "interactions": [],
 *   "success": true
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionDispensingResponse {

    /**
     * Prescription ID
     */
    private UUID prescriptionId;

    /**
     * Final prescription status after dispensing
     */
    private PrescriptionStatus status;

    /**
     * Timestamp when dispensing occurred
     */
    private Instant dispensedAt;

    /**
     * Name of user who dispensed (from User entity)
     */
    private String dispensedBy;

    /**
     * Patient name (from Patient entity)
     */
    private String patientName;

    /**
     * Patient ID
     */
    private UUID patientId;

    /**
     * Doctor/prescriber name (from User entity)
     */
    private String doctorName;

    /**
     * Doctor ID
     */
    private UUID doctorId;

    /**
     * Total number of items in prescription that were dispensed
     */
    private Integer totalItemsDispensed;

    /**
     * Detailed list of dispensed items
     */
    private List<DispensedItemDetail> itemsDispensed;

    /**
     * Number of inventory transactions created
     * Should equal totalItemsDispensed in success case
     */
    private Integer inventoryTransactionsCreated;

    /**
     * Any drug interactions detected during dispensing
     */
    private List<DrugInteractionDetail> interactions;

    /**
     * Whether dispensing was successful
     */
    private Boolean success;

    /**
     * Error message if dispensing failed
     * Will be null on success
     */
    private String errorMessage;

    /**
     * Details of a dispensed item
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DispensedItemDetail {

        /**
         * Medication name (from Inventory.itemName)
         */
        private String medicationName;

        /**
         * Medication code (from Inventory.itemCode)
         */
        private String medicationCode;

        /**
         * Prescribed quantity (dosage * frequency * duration)
         */
        private Integer prescribedQuantity;

        /**
         * Actual dispensed quantity
         */
        private Integer dispensedQuantity;

        /**
         * Unit of measurement (mg, ml, tablet, capsule)
         */
        private String dosageUnit;

        /**
         * Dosage amount
         */
        private Double dosage;

        /**
         * How many times per day
         */
        private Integer frequencyPerDay;

        /**
         * Duration in days
         */
        private Integer durationDays;

        /**
         * Special instructions
         */
        private String instructions;

        /**
         * Stock before this dispensing
         */
        private Integer stockBefore;

        /**
         * Stock after this dispensing
         */
        private Integer stockAfter;
    }

    /**
     * Details of a detected drug interaction
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DrugInteractionDetail {

        /**
         * Severity level: MINOR, MODERATE, SEVERE
         */
        private String severity;

        /**
         * First medication name
         */
        private String medicationA;

        /**
         * Second medication name
         */
        private String medicationB;

        /**
         * Description of the interaction
         */
        private String description;

        /**
         * Clinical recommendation
         */
        private String recommendation;
    }
}
