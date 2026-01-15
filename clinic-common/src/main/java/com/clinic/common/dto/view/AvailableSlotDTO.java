package com.clinic.common.dto.view;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * DTO representing an available time slot for appointment booking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableSlotDTO {

    private LocalDate date;
    private Instant startTime;
    private Instant endTime;
    private String startTimeFormatted;  // HH:mm format
    private String endTimeFormatted;    // HH:mm format
    private Integer durationMinutes;
    private Boolean isAvailable;
}
