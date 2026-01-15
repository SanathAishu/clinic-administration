package com.clinic.common.dto.view;

import com.clinic.common.enums.AppointmentStatus;
import com.clinic.common.enums.ConsultationType;
import com.clinic.common.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO mapping to v_appointment_list database view.
 * Used for appointment listing screens with patient and doctor info.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentListViewDTO {

    private UUID id;
    private UUID tenantId;

    // Patient Information
    private UUID patientId;
    private String patientName;
    private String patientPhone;
    private String patientEmail;
    private Integer patientAge;
    private Gender patientGender;

    // Doctor Information
    private UUID doctorId;
    private String doctorName;
    private String doctorEmail;

    // Appointment Details
    private Instant appointmentTime;
    private LocalDate appointmentDate;
    private String startTime;
    private String endTime;
    private Integer durationMinutes;
    private ConsultationType consultationType;
    private AppointmentStatus status;
    private String reason;

    // Computed Fields
    private String timeCategory;  // TODAY, TOMORROW, PAST, UPCOMING
    private Boolean isOverdue;

    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;
}
