package com.clinic.backend.repository;

import com.clinic.common.dto.view.AppointmentDetailViewDTO;
import com.clinic.common.dto.view.AppointmentListViewDTO;
import com.clinic.common.dto.view.TodayAppointmentViewDTO;
import com.clinic.common.enums.AppointmentStatus;
import com.clinic.common.enums.BloodGroup;
import com.clinic.common.enums.ConsultationType;
import com.clinic.common.enums.Gender;
import com.clinic.common.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for reading appointments from database views.
 * Implements CQRS read-side using native SQL queries against pre-optimized views.
 */
@Repository
@RequiredArgsConstructor
public class AppointmentViewRepository {

    private final JdbcTemplate jdbcTemplate;

    // ================================
    // Appointment List View Queries
    // ================================

    /**
     * Get paginated list of appointments from v_appointment_list view.
     */
    public Page<AppointmentListViewDTO> findAllAppointments(UUID tenantId, Pageable pageable) {
        String countSql = "SELECT COUNT(*) FROM v_appointment_list WHERE tenant_id = ?";
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, tenantId);

        String sql = """
            SELECT * FROM v_appointment_list
            WHERE tenant_id = ?
            ORDER BY appointment_time DESC
            LIMIT ? OFFSET ?
            """;

        List<AppointmentListViewDTO> content = jdbcTemplate.query(
                sql,
                new AppointmentListViewRowMapper(),
                tenantId,
                pageable.getPageSize(),
                pageable.getOffset()
        );

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * Get appointments filtered by status.
     */
    public Page<AppointmentListViewDTO> findByStatus(UUID tenantId, AppointmentStatus status, Pageable pageable) {
        String countSql = "SELECT COUNT(*) FROM v_appointment_list WHERE tenant_id = ? AND status = ?::text";
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, tenantId, status.name());

        String sql = """
            SELECT * FROM v_appointment_list
            WHERE tenant_id = ? AND status = ?::text
            ORDER BY appointment_time DESC
            LIMIT ? OFFSET ?
            """;

        List<AppointmentListViewDTO> content = jdbcTemplate.query(
                sql,
                new AppointmentListViewRowMapper(),
                tenantId,
                status.name(),
                pageable.getPageSize(),
                pageable.getOffset()
        );

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * Get appointments for a specific doctor.
     */
    public Page<AppointmentListViewDTO> findByDoctorId(UUID tenantId, UUID doctorId, Pageable pageable) {
        String countSql = "SELECT COUNT(*) FROM v_appointment_list WHERE tenant_id = ? AND doctor_id = ?";
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, tenantId, doctorId);

        String sql = """
            SELECT * FROM v_appointment_list
            WHERE tenant_id = ? AND doctor_id = ?
            ORDER BY appointment_time DESC
            LIMIT ? OFFSET ?
            """;

        List<AppointmentListViewDTO> content = jdbcTemplate.query(
                sql,
                new AppointmentListViewRowMapper(),
                tenantId,
                doctorId,
                pageable.getPageSize(),
                pageable.getOffset()
        );

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * Get appointments for a specific patient.
     */
    public Page<AppointmentListViewDTO> findByPatientId(UUID tenantId, UUID patientId, Pageable pageable) {
        String countSql = "SELECT COUNT(*) FROM v_appointment_list WHERE tenant_id = ? AND patient_id = ?";
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, tenantId, patientId);

        String sql = """
            SELECT * FROM v_appointment_list
            WHERE tenant_id = ? AND patient_id = ?
            ORDER BY appointment_time DESC
            LIMIT ? OFFSET ?
            """;

        List<AppointmentListViewDTO> content = jdbcTemplate.query(
                sql,
                new AppointmentListViewRowMapper(),
                tenantId,
                patientId,
                pageable.getPageSize(),
                pageable.getOffset()
        );

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * Get appointments for a specific date range.
     */
    public Page<AppointmentListViewDTO> findByDateRange(UUID tenantId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        String countSql = """
            SELECT COUNT(*) FROM v_appointment_list
            WHERE tenant_id = ? AND appointment_date BETWEEN ? AND ?
            """;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, tenantId, startDate, endDate);

        String sql = """
            SELECT * FROM v_appointment_list
            WHERE tenant_id = ? AND appointment_date BETWEEN ? AND ?
            ORDER BY appointment_time ASC
            LIMIT ? OFFSET ?
            """;

        List<AppointmentListViewDTO> content = jdbcTemplate.query(
                sql,
                new AppointmentListViewRowMapper(),
                tenantId,
                startDate,
                endDate,
                pageable.getPageSize(),
                pageable.getOffset()
        );

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    // ================================
    // Appointment Detail View Queries
    // ================================

    /**
     * Get detailed appointment information by ID.
     */
    public Optional<AppointmentDetailViewDTO> findDetailById(UUID tenantId, UUID appointmentId) {
        String sql = "SELECT * FROM v_appointment_detail WHERE tenant_id = ? AND id = ?";

        List<AppointmentDetailViewDTO> results = jdbcTemplate.query(
                sql,
                new AppointmentDetailViewRowMapper(),
                tenantId,
                appointmentId
        );

        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    // ================================
    // Today's Appointments Queries
    // ================================

    /**
     * Get today's appointments for dashboard display.
     */
    public List<TodayAppointmentViewDTO> findTodayAppointments(UUID tenantId) {
        String sql = """
            SELECT * FROM v_today_appointments
            WHERE tenant_id = ?
            ORDER BY display_order ASC, appointment_time ASC
            """;

        return jdbcTemplate.query(sql, new TodayAppointmentViewRowMapper(), tenantId);
    }

    /**
     * Get today's appointments for a specific doctor.
     */
    public List<TodayAppointmentViewDTO> findTodayAppointmentsByDoctor(UUID tenantId, UUID doctorId) {
        String sql = """
            SELECT * FROM v_today_appointments
            WHERE tenant_id = ? AND doctor_id = ?
            ORDER BY display_order ASC, appointment_time ASC
            """;

        return jdbcTemplate.query(sql, new TodayAppointmentViewRowMapper(), tenantId, doctorId);
    }

    // ================================
    // Row Mappers
    // ================================

    private static class AppointmentListViewRowMapper implements RowMapper<AppointmentListViewDTO> {
        @Override
        public AppointmentListViewDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            return AppointmentListViewDTO.builder()
                    .id(getUUID(rs, "id"))
                    .tenantId(getUUID(rs, "tenant_id"))
                    .patientId(getUUID(rs, "patient_id"))
                    .patientName(rs.getString("patient_name"))
                    .patientPhone(rs.getString("patient_phone"))
                    .patientEmail(rs.getString("patient_email"))
                    .patientAge(getNullableInt(rs, "patient_age"))
                    .patientGender(getEnum(rs, "patient_gender", Gender.class))
                    .doctorId(getUUID(rs, "doctor_id"))
                    .doctorName(rs.getString("doctor_name"))
                    .doctorEmail(rs.getString("doctor_email"))
                    .appointmentTime(getInstant(rs, "appointment_time"))
                    .appointmentDate(getLocalDate(rs, "appointment_date"))
                    .startTime(rs.getString("start_time"))
                    .endTime(rs.getString("end_time"))
                    .durationMinutes(rs.getInt("duration_minutes"))
                    .consultationType(getEnum(rs, "consultation_type", ConsultationType.class))
                    .status(getEnum(rs, "status", AppointmentStatus.class))
                    .reason(rs.getString("reason"))
                    .timeCategory(rs.getString("time_category"))
                    .isOverdue(rs.getBoolean("is_overdue"))
                    .createdAt(getInstant(rs, "created_at"))
                    .updatedAt(getInstant(rs, "updated_at"))
                    .build();
        }
    }

    private static class AppointmentDetailViewRowMapper implements RowMapper<AppointmentDetailViewDTO> {
        @Override
        public AppointmentDetailViewDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            return AppointmentDetailViewDTO.builder()
                    .id(getUUID(rs, "id"))
                    .tenantId(getUUID(rs, "tenant_id"))
                    .patientId(getUUID(rs, "patient_id"))
                    .patientFirstName(rs.getString("patient_first_name"))
                    .patientLastName(rs.getString("patient_last_name"))
                    .patientName(rs.getString("patient_name"))
                    .patientPhone(rs.getString("patient_phone"))
                    .patientEmail(rs.getString("patient_email"))
                    .patientDob(getLocalDate(rs, "patient_dob"))
                    .patientAge(getNullableInt(rs, "patient_age"))
                    .patientGender(getEnum(rs, "patient_gender", Gender.class))
                    .patientBloodGroup(getEnum(rs, "patient_blood_group", BloodGroup.class))
                    .patientAbhaId(rs.getString("patient_abha_id"))
                    .doctorId(getUUID(rs, "doctor_id"))
                    .doctorFirstName(rs.getString("doctor_first_name"))
                    .doctorLastName(rs.getString("doctor_last_name"))
                    .doctorName(rs.getString("doctor_name"))
                    .doctorEmail(rs.getString("doctor_email"))
                    .appointmentTime(getInstant(rs, "appointment_time"))
                    .appointmentDate(getLocalDate(rs, "appointment_date"))
                    .appointmentTimeOnly(rs.getString("appointment_time_only"))
                    .durationMinutes(rs.getInt("duration_minutes"))
                    .endTime(getInstant(rs, "end_time"))
                    .consultationType(getEnum(rs, "consultation_type", ConsultationType.class))
                    .status(getEnum(rs, "status", AppointmentStatus.class))
                    .reason(rs.getString("reason"))
                    .notes(rs.getString("notes"))
                    .confirmedAt(getInstant(rs, "confirmed_at"))
                    .startedAt(getInstant(rs, "started_at"))
                    .completedAt(getInstant(rs, "completed_at"))
                    .cancelledAt(getInstant(rs, "cancelled_at"))
                    .cancelledBy(getUUID(rs, "cancelled_by"))
                    .cancelledByName(rs.getString("cancelled_by_name"))
                    .cancellationReason(rs.getString("cancellation_reason"))
                    .medicalRecordId(getUUID(rs, "medical_record_id"))
                    .chiefComplaint(rs.getString("chief_complaint"))
                    .clinicalNotes(rs.getString("clinical_notes"))
                    .treatmentPlan(rs.getString("treatment_plan"))
                    .billingId(getUUID(rs, "billing_id"))
                    .invoiceNumber(rs.getString("invoice_number"))
                    .totalAmount(rs.getBigDecimal("total_amount"))
                    .balanceAmount(rs.getBigDecimal("balance_amount"))
                    .paymentStatus(getEnum(rs, "payment_status", PaymentStatus.class))
                    .createdAt(getInstant(rs, "created_at"))
                    .updatedAt(getInstant(rs, "updated_at"))
                    .createdBy(getUUID(rs, "created_by"))
                    .createdByName(rs.getString("created_by_name"))
                    .build();
        }
    }

    private static class TodayAppointmentViewRowMapper implements RowMapper<TodayAppointmentViewDTO> {
        @Override
        public TodayAppointmentViewDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            return TodayAppointmentViewDTO.builder()
                    .id(getUUID(rs, "id"))
                    .tenantId(getUUID(rs, "tenant_id"))
                    .patientId(getUUID(rs, "patient_id"))
                    .patientName(rs.getString("patient_name"))
                    .patientPhone(rs.getString("patient_phone"))
                    .doctorId(getUUID(rs, "doctor_id"))
                    .doctorName(rs.getString("doctor_name"))
                    .appointmentTime(getInstant(rs, "appointment_time"))
                    .timeSlot(rs.getString("time_slot"))
                    .durationMinutes(rs.getInt("duration_minutes"))
                    .consultationType(getEnum(rs, "consultation_type", ConsultationType.class))
                    .status(getEnum(rs, "status", AppointmentStatus.class))
                    .reason(rs.getString("reason"))
                    .timeStatus(rs.getString("time_status"))
                    .displayOrder(rs.getInt("display_order"))
                    .build();
        }
    }

    // ================================
    // Helper Methods for ResultSet
    // ================================

    private static UUID getUUID(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value != null ? UUID.fromString(value) : null;
    }

    private static Instant getInstant(ResultSet rs, String columnName) throws SQLException {
        Timestamp ts = rs.getTimestamp(columnName);
        return ts != null ? ts.toInstant() : null;
    }

    private static LocalDate getLocalDate(ResultSet rs, String columnName) throws SQLException {
        java.sql.Date date = rs.getDate(columnName);
        return date != null ? date.toLocalDate() : null;
    }

    private static Integer getNullableInt(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    private static <T extends Enum<T>> T getEnum(ResultSet rs, String columnName, Class<T> enumType) throws SQLException {
        String value = rs.getString(columnName);
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
