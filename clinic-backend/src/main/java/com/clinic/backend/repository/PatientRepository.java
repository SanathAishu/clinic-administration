package com.clinic.backend.repository;

import com.clinic.backend.entity.Patient;
import com.clinic.common.enums.Gender;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    // Uniqueness enforcement per tenant
    Optional<Patient> findByEmailAndTenantId(String email, UUID tenantId);

    Optional<Patient> findByPhoneAndTenantId(String phone, UUID tenantId);

    Optional<Patient> findByAbhaIdAndTenantId(String abhaId, UUID tenantId);

    Optional<Patient> findByAbhaNumberAndTenantId(String abhaNumber, UUID tenantId);

    boolean existsByEmailAndTenantId(String email, UUID tenantId);

    boolean existsByPhoneAndTenantId(String phone, UUID tenantId);

    boolean existsByAbhaIdAndTenantId(String abhaId, UUID tenantId);

    // Tenant-scoped queries
    Page<Patient> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Optional<Patient> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    // Search queries
    @Query("SELECT p FROM Patient p WHERE p.tenantId = :tenantId AND p.deletedAt IS NULL AND " +
           "(LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "p.phone LIKE CONCAT('%', :search, '%'))")
    Page<Patient> searchPatients(@Param("tenantId") UUID tenantId, @Param("search") String search, Pageable pageable);

    // Demographics queries
    List<Patient> findByTenantIdAndGenderAndDeletedAtIsNull(UUID tenantId, Gender gender);

    @Query("SELECT p FROM Patient p WHERE p.tenantId = :tenantId AND p.dateOfBirth BETWEEN :startDate AND :endDate AND p.deletedAt IS NULL")
    List<Patient> findByAgeRange(@Param("tenantId") UUID tenantId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // ABHA (Ayushman Bharat Health Account) queries
    @Query("SELECT p FROM Patient p WHERE p.tenantId = :tenantId AND (p.abhaId IS NOT NULL OR p.abhaNumber IS NOT NULL) AND p.deletedAt IS NULL")
    List<Patient> findPatientsWithAbha(@Param("tenantId") UUID tenantId);

    // Counting for capacity constraints
    long countByTenantIdAndDeletedAtIsNull(UUID tenantId);

    // Recent patients
    @Query("SELECT p FROM Patient p WHERE p.tenantId = :tenantId AND p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    Page<Patient> findRecentPatients(@Param("tenantId") UUID tenantId, Pageable pageable);
}
