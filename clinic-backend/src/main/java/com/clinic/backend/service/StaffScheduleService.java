package com.clinic.backend.service;

import com.clinic.common.entity.operational.StaffSchedule;
import com.clinic.backend.repository.StaffScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffScheduleService {

    private final StaffScheduleRepository staffScheduleRepository;

    @Transactional
    public StaffSchedule createSchedule(StaffSchedule schedule, UUID tenantId) {
        log.debug("Creating schedule for staff: {}", schedule.getUser().getId());

        schedule.setTenantId(tenantId);

        if (schedule.getValidFrom() == null) {
            schedule.setValidFrom(LocalDate.now());
        }

        if (schedule.getIsAvailable() == null) {
            schedule.setIsAvailable(true);
        }

        // Validate temporal ordering: startTime < endTime
        if (schedule.getStartTime() != null && schedule.getEndTime() != null) {
            if (!schedule.getStartTime().isBefore(schedule.getEndTime())) {
                throw new IllegalArgumentException("Start time must be before end time");
            }
        }

        // Validate date range: validFrom <= validUntil
        if (schedule.getValidFrom() != null && schedule.getValidUntil() != null) {
            if (schedule.getValidFrom().isAfter(schedule.getValidUntil())) {
                throw new IllegalArgumentException("Valid from date must be before or equal to valid until date");
            }
        }

        // Check for overlapping schedules
        long overlapping = staffScheduleRepository.countOverlappingSchedules(
                schedule.getUser().getId(),
                tenantId,
                String.valueOf(schedule.getDayOfWeek()),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getValidFrom(),
                schedule.getValidUntil() != null ? schedule.getValidUntil() : LocalDate.of(9999, 12, 31),
                UUID.randomUUID() // New schedule, exclude nothing
        );

        if (overlapping > 0) {
            throw new IllegalStateException("Schedule conflict: overlapping schedule exists for this staff member");
        }

        StaffSchedule saved = staffScheduleRepository.save(schedule);
        log.info("Created staff schedule: {}", saved.getId());
        return saved;
    }

    public StaffSchedule getScheduleById(UUID id, UUID tenantId) {
        return staffScheduleRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + id));
    }

    public Page<StaffSchedule> getSchedulesForStaff(UUID staffId, UUID tenantId, Pageable pageable) {
        return staffScheduleRepository.findByStaffIdAndTenantIdAndDeletedAtIsNull(staffId, tenantId, pageable);
    }

    public List<StaffSchedule> getActiveSchedulesForStaff(UUID staffId, UUID tenantId, LocalDate date) {
        return staffScheduleRepository.findActiveSchedulesForStaff(staffId, tenantId, date);
    }

    public List<StaffSchedule> getAvailableStaffForDay(UUID tenantId, String dayOfWeek, LocalDate date) {
        return staffScheduleRepository.findAvailableStaffForDay(tenantId, dayOfWeek, date);
    }

    public List<StaffSchedule> getSchedulesByDayOfWeek(UUID tenantId, String dayOfWeek) {
        return staffScheduleRepository.findByTenantIdAndDayOfWeek(tenantId, dayOfWeek);
    }

    @Transactional
    public StaffSchedule markAsUnavailable(UUID id, UUID tenantId) {
        StaffSchedule schedule = getScheduleById(id, tenantId);
        schedule.setIsAvailable(false);
        StaffSchedule saved = staffScheduleRepository.save(schedule);
        log.info("Marked schedule as unavailable: {}", saved.getId());
        return saved;
    }

    @Transactional
    public StaffSchedule markAsAvailable(UUID id, UUID tenantId) {
        StaffSchedule schedule = getScheduleById(id, tenantId);
        schedule.setIsAvailable(true);
        StaffSchedule saved = staffScheduleRepository.save(schedule);
        log.info("Marked schedule as available: {}", saved.getId());
        return saved;
    }

    @Transactional
    public StaffSchedule updateSchedule(UUID id, UUID tenantId, StaffSchedule updates) {
        StaffSchedule schedule = getScheduleById(id, tenantId);

        LocalTime newStartTime = updates.getStartTime() != null ? updates.getStartTime() : schedule.getStartTime();
        LocalTime newEndTime = updates.getEndTime() != null ? updates.getEndTime() : schedule.getEndTime();
        LocalDate newValidFrom = updates.getValidFrom() != null ? updates.getValidFrom() : schedule.getValidFrom();
        LocalDate newValidUntil = updates.getValidUntil() != null ? updates.getValidUntil() : schedule.getValidUntil();
        Integer newDayOfWeek = updates.getDayOfWeek() != null ? updates.getDayOfWeek() : schedule.getDayOfWeek();

        // Validate temporal ordering
        if (!newStartTime.isBefore(newEndTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        if (newValidFrom != null && newValidUntil != null && newValidFrom.isAfter(newValidUntil)) {
            throw new IllegalArgumentException("Valid from date must be before or equal to valid until date");
        }

        // Check for overlapping schedules (excluding current schedule)
        long overlapping = staffScheduleRepository.countOverlappingSchedules(
                schedule.getUser().getId(),
                tenantId,
                String.valueOf(newDayOfWeek),
                newStartTime,
                newEndTime,
                newValidFrom,
                newValidUntil != null ? newValidUntil : LocalDate.of(9999, 12, 31),
                schedule.getId()
        );

        if (overlapping > 0) {
            throw new IllegalStateException("Schedule conflict with updated times");
        }

        if (updates.getDayOfWeek() != null) schedule.setDayOfWeek(updates.getDayOfWeek());
        if (updates.getStartTime() != null) schedule.setStartTime(updates.getStartTime());
        if (updates.getEndTime() != null) schedule.setEndTime(updates.getEndTime());
        if (updates.getValidFrom() != null) schedule.setValidFrom(updates.getValidFrom());
        if (updates.getValidUntil() != null) schedule.setValidUntil(updates.getValidUntil());
        if (updates.getIsAvailable() != null) schedule.setIsAvailable(updates.getIsAvailable());
        if (updates.getNotes() != null) schedule.setNotes(updates.getNotes());

        return staffScheduleRepository.save(schedule);
    }

    @Transactional
    public void softDeleteSchedule(UUID id, UUID tenantId) {
        StaffSchedule schedule = getScheduleById(id, tenantId);
        schedule.softDelete();
        staffScheduleRepository.save(schedule);
        log.info("Soft deleted staff schedule: {}", id);
    }

    public long countSchedulesForStaff(UUID staffId, UUID tenantId) {
        return staffScheduleRepository.countByStaffIdAndTenantId(staffId, tenantId);
    }
}
