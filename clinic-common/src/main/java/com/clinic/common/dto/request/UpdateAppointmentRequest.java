package com.clinic.common.dto.request;

import com.clinic.common.enums.ConsultationType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class UpdateAppointmentRequest {

    private UUID patientId;
    private UUID doctorId;

    @Future(message = "Appointment time must be in the future")
    private Instant appointmentTime;

    @Min(value = 15, message = "Duration must be at least 15 minutes")
    @Max(value = 240, message = "Duration cannot exceed 240 minutes")
    private Integer durationMinutes;

    private ConsultationType consultationType;
    private String reason;
    private String notes;
}
