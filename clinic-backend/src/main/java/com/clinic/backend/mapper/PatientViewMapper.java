package com.clinic.backend.mapper;

import com.clinic.common.dto.view.*;
import com.clinic.common.enums.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mapper for converting native query Object[] results to View DTOs.
 * Handles type conversion from PostgreSQL types to Java types.
 */
@Slf4j
@Component
public class PatientViewMapper {

    /**
     * Map Object[] from v_patient_list view to PatientListViewDTO.
     */
    public PatientListViewDTO toPatientListViewDTO(Object[] row) {
        if (row == null) return null;

        int idx = 0;
        return PatientListViewDTO.builder()
            .id(toUUID(row[idx++]))
            .tenantId(toUUID(row[idx++]))
            .firstName(toString(row[idx++]))
            .middleName(toString(row[idx++]))
            .lastName(toString(row[idx++]))
            .fullName(toString(row[idx++]))
            .dateOfBirth(toLocalDate(row[idx++]))
            .age(toInteger(row[idx++]))
            .gender(toGender(row[idx++]))
            .phone(toString(row[idx++]))
            .email(toString(row[idx++]))
            .bloodGroup(toBloodGroup(row[idx++]))
            .abhaId(toString(row[idx++]))
            .createdAt(toInstant(row[idx++]))
            .updatedAt(toInstant(row[idx++]))
            .status(toString(row[idx++]))
            .isActive(toBoolean(row[idx++]))
            .build();
    }

    /**
     * Map Object[] from v_patient_detail view to PatientDetailViewDTO.
     */
    public PatientDetailViewDTO toPatientDetailViewDTO(Object[] row) {
        if (row == null) return null;

        int idx = 0;
        return PatientDetailViewDTO.builder()
            .id(toUUID(row[idx++]))
            .tenantId(toUUID(row[idx++]))
            .firstName(toString(row[idx++]))
            .middleName(toString(row[idx++]))
            .lastName(toString(row[idx++]))
            .fullName(toString(row[idx++]))
            .dateOfBirth(toLocalDate(row[idx++]))
            .age(toInteger(row[idx++]))
            .gender(toGender(row[idx++]))
            .phone(toString(row[idx++]))
            .email(toString(row[idx++]))
            .addressLine1(toString(row[idx++]))
            .addressLine2(toString(row[idx++]))
            .city(toString(row[idx++]))
            .state(toString(row[idx++]))
            .pincode(toString(row[idx++]))
            .bloodGroup(toBloodGroup(row[idx++]))
            .abhaId(toString(row[idx++]))
            .abhaNumber(toString(row[idx++]))
            .maritalStatus(toMaritalStatus(row[idx++]))
            .occupation(toString(row[idx++]))
            .emergencyContactName(toString(row[idx++]))
            .emergencyContactPhone(toString(row[idx++]))
            .emergencyContactRelation(toString(row[idx++]))
            .allergies(toStringArray(row[idx++]))
            .chronicConditions(toStringArray(row[idx++]))
            .createdAt(toInstant(row[idx++]))
            .updatedAt(toInstant(row[idx++]))
            .deletedAt(toInstant(row[idx++]))
            .latestVitalId(toUUID(row[idx++]))
            .latestVitalRecordedAt(toInstant(row[idx++]))
            .temperatureCelsius(toBigDecimal(row[idx++]))
            .pulseBpm(toInteger(row[idx++]))
            .systolicBp(toInteger(row[idx++]))
            .diastolicBp(toInteger(row[idx++]))
            .respiratoryRate(toInteger(row[idx++]))
            .oxygenSaturation(toInteger(row[idx++]))
            .weightKg(toBigDecimal(row[idx++]))
            .heightCm(toBigDecimal(row[idx++]))
            .bmi(toBigDecimal(row[idx++]))
            .bloodPressure(toString(row[idx++]))
            .totalAppointments(toLong(row[idx++]))
            .totalMedicalRecords(toLong(row[idx++]))
            .totalPrescriptions(toLong(row[idx++]))
            .totalLabTests(toLong(row[idx++]))
            .outstandingBalance(toBigDecimal(row[idx++]))
            .build();
    }

    /**
     * Map Object[] from v_patient_appointments view to PatientAppointmentViewDTO.
     */
    public PatientAppointmentViewDTO toPatientAppointmentViewDTO(Object[] row) {
        if (row == null) return null;

        int idx = 0;
        return PatientAppointmentViewDTO.builder()
            .id(toUUID(row[idx++]))
            .tenantId(toUUID(row[idx++]))
            .patientId(toUUID(row[idx++]))
            .patientName(toString(row[idx++]))
            .patientPhone(toString(row[idx++]))
            .doctorId(toUUID(row[idx++]))
            .doctorName(toString(row[idx++]))
            .doctorEmail(toString(row[idx++]))
            .appointmentTime(toLocalDateTime(row[idx++]))
            .appointmentDate(toLocalDate(row[idx++]))
            .appointmentTimeOnly(toString(row[idx++]))
            .durationMinutes(toInteger(row[idx++]))
            .endTime(toLocalDateTime(row[idx++]))
            .consultationType(toConsultationType(row[idx++]))
            .status(toAppointmentStatus(row[idx++]))
            .reason(toString(row[idx++]))
            .notes(toString(row[idx++]))
            .confirmedAt(toInstant(row[idx++]))
            .startedAt(toInstant(row[idx++]))
            .completedAt(toInstant(row[idx++]))
            .cancelledAt(toInstant(row[idx++]))
            .cancellationReason(toString(row[idx++]))
            .createdAt(toInstant(row[idx++]))
            .updatedAt(toInstant(row[idx++]))
            .medicalRecordId(toUUID(row[idx++]))
            .chiefComplaint(toString(row[idx++]))
            .isOverdue(toBoolean(row[idx++]))
            .isToday(toBoolean(row[idx++]))
            .build();
    }

    /**
     * Map Object[] from v_patient_medical_history view to PatientMedicalHistoryViewDTO.
     */
    public PatientMedicalHistoryViewDTO toPatientMedicalHistoryViewDTO(Object[] row) {
        if (row == null) return null;

        int idx = 0;
        return PatientMedicalHistoryViewDTO.builder()
            .id(toUUID(row[idx++]))
            .tenantId(toUUID(row[idx++]))
            .patientId(toUUID(row[idx++]))
            .patientName(toString(row[idx++]))
            .doctorId(toUUID(row[idx++]))
            .doctorName(toString(row[idx++]))
            .recordDate(toLocalDate(row[idx++]))
            .chiefComplaint(toString(row[idx++]))
            .historyPresentIllness(toString(row[idx++]))
            .examinationFindings(toString(row[idx++]))
            .clinicalNotes(toString(row[idx++]))
            .treatmentPlan(toString(row[idx++]))
            .followUpInstructions(toString(row[idx++]))
            .followUpDate(toLocalDate(row[idx++]))
            .appointmentId(toUUID(row[idx++]))
            .appointmentTime(toLocalDateTime(row[idx++]))
            .consultationType(toConsultationType(row[idx++]))
            .diagnosisCount(toLong(row[idx++]))
            .prescriptionCount(toLong(row[idx++]))
            .labTestCount(toLong(row[idx++]))
            .createdAt(toInstant(row[idx++]))
            .updatedAt(toInstant(row[idx++]))
            .build();
    }

    /**
     * Map Object[] from v_patient_billing_history view to PatientBillingHistoryViewDTO.
     */
    public PatientBillingHistoryViewDTO toPatientBillingHistoryViewDTO(Object[] row) {
        if (row == null) return null;

        int idx = 0;
        return PatientBillingHistoryViewDTO.builder()
            .id(toUUID(row[idx++]))
            .tenantId(toUUID(row[idx++]))
            .patientId(toUUID(row[idx++]))
            .patientName(toString(row[idx++]))
            .patientPhone(toString(row[idx++]))
            .invoiceNumber(toString(row[idx++]))
            .invoiceDate(toLocalDate(row[idx++]))
            .subtotal(toBigDecimal(row[idx++]))
            .discountAmount(toBigDecimal(row[idx++]))
            .taxAmount(toBigDecimal(row[idx++]))
            .totalAmount(toBigDecimal(row[idx++]))
            .paidAmount(toBigDecimal(row[idx++]))
            .balanceAmount(toBigDecimal(row[idx++]))
            .paymentStatus(toPaymentStatus(row[idx++]))
            .paymentMethod(toPaymentMethod(row[idx++]))
            .paymentDate(toLocalDate(row[idx++]))
            .paymentReference(toString(row[idx++]))
            .lineItems(toString(row[idx++]))
            .appointmentId(toUUID(row[idx++]))
            .appointmentTime(toLocalDateTime(row[idx++]))
            .consultationType(toConsultationType(row[idx++]))
            .doctorId(toUUID(row[idx++]))
            .doctorName(toString(row[idx++]))
            .isOverdue(toBoolean(row[idx++]))
            .hasBalance(toBoolean(row[idx++]))
            .createdAt(toInstant(row[idx++]))
            .updatedAt(toInstant(row[idx++]))
            .build();
    }

    // ============================
    // Type Conversion Helpers
    // ============================

    private UUID toUUID(Object value) {
        if (value == null) return null;
        if (value instanceof UUID) return (UUID) value;
        return UUID.fromString(value.toString());
    }

    private String toString(Object value) {
        if (value == null) return null;
        return value.toString();
    }

    private Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(value.toString());
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        return new BigDecimal(value.toString());
    }

    private Boolean toBoolean(Object value) {
        if (value == null) return null;
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(value.toString());
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate) return (LocalDate) value;
        if (value instanceof Date) return ((Date) value).toLocalDate();
        if (value instanceof java.util.Date) return new java.sql.Date(((java.util.Date) value).getTime()).toLocalDate();
        return LocalDate.parse(value.toString());
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime) return (LocalDateTime) value;
        if (value instanceof Timestamp) return ((Timestamp) value).toLocalDateTime();
        return LocalDateTime.parse(value.toString());
    }

    private Instant toInstant(Object value) {
        if (value == null) return null;
        if (value instanceof Instant) return (Instant) value;
        if (value instanceof Timestamp) return ((Timestamp) value).toInstant();
        if (value instanceof java.util.Date) return ((java.util.Date) value).toInstant();
        return Instant.parse(value.toString());
    }

    private String[] toStringArray(Object value) {
        if (value == null) return null;
        if (value instanceof String[]) return (String[]) value;
        if (value instanceof Array) {
            try {
                Object array = ((Array) value).getArray();
                if (array instanceof String[]) return (String[]) array;
                if (array instanceof Object[]) {
                    Object[] objects = (Object[]) array;
                    String[] result = new String[objects.length];
                    for (int i = 0; i < objects.length; i++) {
                        result[i] = objects[i] != null ? objects[i].toString() : null;
                    }
                    return result;
                }
            } catch (Exception e) {
                log.warn("Failed to convert SQL Array to String[]: {}", e.getMessage());
            }
        }
        return null;
    }

    // ============================
    // Enum Conversion Helpers
    // ============================

    private Gender toGender(Object value) {
        if (value == null) return null;
        return Gender.valueOf(value.toString());
    }

    private BloodGroup toBloodGroup(Object value) {
        if (value == null) return null;
        return BloodGroup.valueOf(value.toString());
    }

    private MaritalStatus toMaritalStatus(Object value) {
        if (value == null) return null;
        return MaritalStatus.valueOf(value.toString());
    }

    private AppointmentStatus toAppointmentStatus(Object value) {
        if (value == null) return null;
        return AppointmentStatus.valueOf(value.toString());
    }

    private ConsultationType toConsultationType(Object value) {
        if (value == null) return null;
        return ConsultationType.valueOf(value.toString());
    }

    private PaymentStatus toPaymentStatus(Object value) {
        if (value == null) return null;
        return PaymentStatus.valueOf(value.toString());
    }

    private PaymentMethod toPaymentMethod(Object value) {
        if (value == null) return null;
        return PaymentMethod.valueOf(value.toString());
    }
}
