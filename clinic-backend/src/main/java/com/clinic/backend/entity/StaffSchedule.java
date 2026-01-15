package com.clinic.backend.entity;

import com.clinic.common.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "staff_schedules", indexes = {
    @Index(name = "idx_staff_schedules_tenant", columnList = "tenant_id"),
    @Index(name = "idx_staff_schedules_user", columnList = "user_id, day_of_week"),
    @Index(name = "idx_staff_schedules_validity", columnList = "valid_from, valid_until")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffSchedule extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private User user;

    // Schedule Details
    @Column(name = "day_of_week", nullable = false)
    @NotNull
    @Min(value = 0, message = "Day of week must be between 0 and 6")
    @Max(value = 6, message = "Day of week must be between 0 and 6")
    private Integer dayOfWeek;

    @Column(name = "start_time", nullable = false)
    @NotNull
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    @NotNull
    private LocalTime endTime;

    @Column(name = "is_available", nullable = false)
    @NotNull
    private Boolean isAvailable = true;

    // Date Range (for temporary schedules or leaves)
    @Column(name = "valid_from", nullable = false)
    @NotNull
    private LocalDate validFrom = LocalDate.now();

    @Column(name = "valid_until")
    private LocalDate validUntil;

    // Break Time
    @Column(name = "break_start_time")
    private LocalTime breakStartTime;

    @Column(name = "break_end_time")
    private LocalTime breakEndTime;

    // Additional Information
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Metadata
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @NotNull
    private User createdBy;

    // Helper methods
    public String getDayName() {
        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        return days[dayOfWeek];
    }

    public boolean isActiveOn(LocalDate date) {
        if (isDeleted()) {
            return false;
        }
        if (Boolean.FALSE.equals(isAvailable)) {
            return false;
        }
        if (date.isBefore(validFrom)) {
            return false;
        }
        if (validUntil != null && date.isAfter(validUntil)) {
            return false;
        }
        return date.getDayOfWeek().getValue() % 7 == dayOfWeek;
    }

    public boolean hasBreak() {
        return breakStartTime != null && breakEndTime != null;
    }
}
