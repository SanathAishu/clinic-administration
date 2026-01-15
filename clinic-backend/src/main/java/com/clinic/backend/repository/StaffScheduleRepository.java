package com.clinic.backend.repository;

import com.clinic.common.entity.operational.StaffSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffScheduleRepository extends JpaRepository<StaffSchedule, UUID> {

    // Tenant-scoped queries
    Optional<StaffSchedule> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    // User schedule queries
    List<StaffSchedule> findByUserIdAndTenantIdAndDeletedAtIsNull(UUID userId, UUID tenantId);

    List<StaffSchedule> findByUserIdAndTenantIdAndDayOfWeekAndDeletedAtIsNull(UUID userId, UUID tenantId, Integer dayOfWeek);

    // Active schedules for date
    @Query("SELECT s FROM StaffSchedule s WHERE s.user.id = :userId AND s.tenantId = :tenantId AND " +
           "s.validFrom <= :date AND (s.validUntil IS NULL OR s.validUntil >= :date) AND " +
           "s.isAvailable = true AND s.deletedAt IS NULL")
    List<StaffSchedule> findActiveSchedulesForUserOnDate(@Param("userId") UUID userId,
                                                          @Param("tenantId") UUID tenantId,
                                                          @Param("date") LocalDate date);

    // Day of week schedules
    @Query("SELECT s FROM StaffSchedule s WHERE s.tenantId = :tenantId AND s.dayOfWeek = :dayOfWeek AND " +
           "s.validFrom <= :date AND (s.validUntil IS NULL OR s.validUntil >= :date) AND " +
           "s.isAvailable = true AND s.deletedAt IS NULL")
    List<StaffSchedule> findSchedulesForDay(@Param("tenantId") UUID tenantId,
                                             @Param("dayOfWeek") Integer dayOfWeek,
                                             @Param("date") LocalDate date);

    // Availability check
    @Query("SELECT s FROM StaffSchedule s WHERE s.user.id = :userId AND s.tenantId = :tenantId AND " +
           "s.dayOfWeek = :dayOfWeek AND s.startTime <= :time AND s.endTime > :time AND " +
           "s.validFrom <= :date AND (s.validUntil IS NULL OR s.validUntil >= :date) AND " +
           "s.isAvailable = true AND s.deletedAt IS NULL")
    List<StaffSchedule> findSchedulesForUserAtTime(@Param("userId") UUID userId,
                                                    @Param("tenantId") UUID tenantId,
                                                    @Param("dayOfWeek") Integer dayOfWeek,
                                                    @Param("date") LocalDate date,
                                                    @Param("time") LocalTime time);

    // Temporal overlap check (enforced at application level)
    @Query("SELECT COUNT(s) FROM StaffSchedule s WHERE s.user.id = :userId AND s.tenantId = :tenantId AND " +
           "s.dayOfWeek = :dayOfWeek AND s.validFrom <= :date AND (s.validUntil IS NULL OR s.validUntil >= :date) AND " +
           "((s.startTime < :endTime AND s.endTime > :startTime)) AND " +
           "s.isAvailable = true AND s.deletedAt IS NULL AND s.id != :excludeId")
    long countOverlappingSchedules(@Param("userId") UUID userId,
                                    @Param("tenantId") UUID tenantId,
                                    @Param("dayOfWeek") Integer dayOfWeek,
                                    @Param("date") LocalDate date,
                                    @Param("startTime") LocalTime startTime,
                                    @Param("endTime") LocalTime endTime,
                                    @Param("excludeId") UUID excludeId);

    // Availability queries
    List<StaffSchedule> findByTenantIdAndIsAvailableAndDeletedAtIsNull(UUID tenantId, Boolean isAvailable);

    @Query("SELECT s FROM StaffSchedule s WHERE s.user.id = :userId AND s.tenantId = :tenantId AND " +
           "s.isAvailable = false AND s.deletedAt IS NULL")
    List<StaffSchedule> findUserUnavailableSchedules(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);

    // Break time schedules
    @Query("SELECT s FROM StaffSchedule s WHERE s.tenantId = :tenantId AND " +
           "s.breakStartTime IS NOT NULL AND s.breakEndTime IS NOT NULL AND s.deletedAt IS NULL")
    List<StaffSchedule> findSchedulesWithBreaks(@Param("tenantId") UUID tenantId);

    // Temporary schedules (with validity period)
    @Query("SELECT s FROM StaffSchedule s WHERE s.tenantId = :tenantId AND s.validUntil IS NOT NULL AND " +
           "s.deletedAt IS NULL ORDER BY s.validUntil ASC")
    List<StaffSchedule> findTemporarySchedules(@Param("tenantId") UUID tenantId);

    // Expired schedules
    @Query("SELECT s FROM StaffSchedule s WHERE s.tenantId = :tenantId AND s.validUntil < :date AND s.deletedAt IS NULL")
    List<StaffSchedule> findExpiredSchedules(@Param("tenantId") UUID tenantId, @Param("date") LocalDate date);

    // Counting
    long countByUserIdAndTenantIdAndDeletedAtIsNull(UUID userId, UUID tenantId);

    // Additional methods for service (note: service uses "staff" terminology but entity uses "user")
    @Query("SELECT s FROM StaffSchedule s WHERE s.user.id = :staffId AND s.tenantId = :tenantId AND " +
           "s.deletedAt IS NULL ORDER BY s.validFrom DESC")
    org.springframework.data.domain.Page<StaffSchedule> findByStaffIdAndTenantIdAndDeletedAtIsNull(
            @Param("staffId") UUID staffId,
            @Param("tenantId") UUID tenantId,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT s FROM StaffSchedule s WHERE s.user.id = :staffId AND s.tenantId = :tenantId AND " +
           "s.validFrom <= :date AND (s.validUntil IS NULL OR s.validUntil >= :date) AND " +
           "s.isAvailable = true AND s.deletedAt IS NULL")
    List<StaffSchedule> findActiveSchedulesForStaff(@Param("staffId") UUID staffId,
                                                     @Param("tenantId") UUID tenantId,
                                                     @Param("date") LocalDate date);

    @Query("SELECT s FROM StaffSchedule s WHERE s.tenantId = :tenantId AND CAST(s.dayOfWeek AS string) = :dayOfWeek AND " +
           "s.validFrom <= :date AND (s.validUntil IS NULL OR s.validUntil >= :date) AND " +
           "s.isAvailable = true AND s.deletedAt IS NULL")
    List<StaffSchedule> findAvailableStaffForDay(@Param("tenantId") UUID tenantId,
                                                  @Param("dayOfWeek") String dayOfWeek,
                                                  @Param("date") LocalDate date);

    @Query("SELECT s FROM StaffSchedule s WHERE s.tenantId = :tenantId AND CAST(s.dayOfWeek AS string) = :dayOfWeek AND " +
           "s.deletedAt IS NULL ORDER BY s.startTime ASC")
    List<StaffSchedule> findByTenantIdAndDayOfWeek(@Param("tenantId") UUID tenantId, @Param("dayOfWeek") String dayOfWeek);

    @Query("SELECT COUNT(s) FROM StaffSchedule s WHERE s.user.id = :staffId AND s.tenantId = :tenantId AND " +
           "CAST(s.dayOfWeek AS string) = CAST(:dayOfWeek AS string) AND " +
           "s.validFrom <= :validUntil AND (s.validUntil IS NULL OR s.validUntil >= :validFrom) AND " +
           "((s.startTime < :endTime AND s.endTime > :startTime)) AND " +
           "s.isAvailable = true AND s.deletedAt IS NULL AND s.id != :excludeId")
    long countOverlappingSchedules(@Param("staffId") UUID staffId,
                                    @Param("tenantId") UUID tenantId,
                                    @Param("dayOfWeek") String dayOfWeek,
                                    @Param("startTime") LocalTime startTime,
                                    @Param("endTime") LocalTime endTime,
                                    @Param("validFrom") LocalDate validFrom,
                                    @Param("validUntil") LocalDate validUntil,
                                    @Param("excludeId") UUID excludeId);

    @Query("SELECT COUNT(s) FROM StaffSchedule s WHERE s.user.id = :staffId AND s.tenantId = :tenantId AND s.deletedAt IS NULL")
    long countByStaffIdAndTenantId(@Param("staffId") UUID staffId, @Param("tenantId") UUID tenantId);
}
