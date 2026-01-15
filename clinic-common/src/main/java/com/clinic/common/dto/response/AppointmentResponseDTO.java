package com.clinic.common.dto.response;

import com.clinic.common.enums.AppointmentStatus;
import com.clinic.common.enums.ConsultationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponseDTO {

    private UUID id;

    // Related Entities (nested DTOs)
    private UUID patientId;
    private String patientName;
    private UUID doctorId;
    private String doctorName;

    // Appointment Details
    private Instant appointmentTime;
    private Instant endTime;
    private Integer durationMinutes;
    private ConsultationType consultationType;
    private AppointmentStatus status;

    // Additional Information
    private String reason;
    private String notes;

    // State Machine Tracking
    private Instant confirmedAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant cancelledAt;
    private UUID cancelledById;
    private String cancelledByName;
    private String cancellationReason;

    // Metadata
    private UUID createdById;
    private String createdByName;
    private Instant createdAt;
    private Instant updatedAt;
}
