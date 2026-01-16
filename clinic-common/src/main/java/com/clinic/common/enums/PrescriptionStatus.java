package com.clinic.common.enums;

/**
 * Prescription Status Enumeration - DAG State Machine
 *
 * Valid State Transitions (Directed Acyclic Graph):
 * PENDING ──dispense()──> DISPENSED ──complete()──> COMPLETED
 *    │                                                  ▲
 *    └──────────cancel()───────────────────────────────┘
 *
 * Terminal States: COMPLETED, CANCELLED (no outgoing transitions)
 *
 * Reference: Phase D Feature 3 - Prescription Enhancement
 * Mathematical Foundation: Discrete Mathematics & ACID Transactions
 */
public enum PrescriptionStatus {
    /**
     * Initial state: Prescription created by doctor but not yet dispensed.
     * Transitions: DISPENSED (when medication dispensed), CANCELLED (if voided)
     */
    PENDING,

    /**
     * Medication has been dispensed to patient.
     * Transitions: COMPLETED (when patient finishes), CANCELLED (if recall needed)
     */
    DISPENSED,

    /**
     * Patient has completed medication course.
     * Terminal state - no further transitions.
     * Can be used for refilling if refillsRemaining > 0
     */
    COMPLETED,

    /**
     * Prescription cancelled (either before dispensing or after).
     * Terminal state - no further transitions.
     * No inventory recovery on cancellation (already dispensed or other reasons)
     */
    CANCELLED;

    /**
     * Check if this is a terminal state (no outgoing transitions).
     * Terminal states: COMPLETED, CANCELLED
     *
     * @return true if state is terminal
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
