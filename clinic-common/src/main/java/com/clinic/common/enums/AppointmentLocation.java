package com.clinic.common.enums;

/**
 * Appointment location type
 * Supports clinic visits, house visits, and virtual consultations
 */
public enum AppointmentLocation {
    CLINIC,      // Patient visits clinic
    HOME,        // Doctor visits patient at home
    VIRTUAL      // Online/telemedicine consultation
}
