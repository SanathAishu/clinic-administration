package com.clinic.common.dto.view;

import com.clinic.common.enums.AppointmentStatus;
import com.clinic.common.enums.ConsultationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO mapping to v_today_appointments database view.
 * Used for today's appointments dashboard display.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodayAppointmentViewDTO {

    private UUID id;
    private UUID tenantId;

    // Patient Information
    private UUID patientId;
    private String patientName;
    private String patientPhone;

    // Doctor Information
    private UUID doctorId;
    private String doctorName;

    // Appointment Details
    private Instant appointmentTime;
    private String timeSlot;  // Formatted time (HH24:MI)
    private Integer durationMinutes;
    private ConsultationType consultationType;
    private AppointmentStatus status;
    private String reason;

    // Computed Fields
    private String timeStatus;  // DONE, CANCELLED, IN_PROGRESS, OVERDUE, UPCOMING, SCHEDULED
    private Integer displayOrder;  // For sorting: IN_PROGRESS=1, CONFIRMED=2, SCHEDULED=3, COMPLETED=4, CANCELLED=5
}
