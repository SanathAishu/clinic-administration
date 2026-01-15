package com.clinic.backend.repository;

import com.clinic.backend.entity.ConsentRecord;
import com.clinic.common.enums.ConsentStatus;
import com.clinic.common.enums.ConsentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsentRecordRepository extends JpaRepository<ConsentRecord, UUID> {

    // Tenant-scoped queries
    Page<ConsentRecord> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<ConsentRecord> findByIdAndTenantId(UUID id, UUID tenantId);

    // Patient consent records (DPDP Act 2023 compliance)
    @Query("SELECT c FROM ConsentRecord c WHERE c.patient.id = :patientId AND c.tenantId = :tenantId " +
           "ORDER BY c.grantedAt DESC")
    Page<ConsentRecord> findPatientConsents(@Param("patientId") UUID patientId,
                                             @Param("tenantId") UUID tenantId,
                                             Pageable pageable);

    List<ConsentRecord> findByPatientIdAndTenantIdOrderByGrantedAtDesc(UUID patientId, UUID tenantId);

    // Consent type queries
    List<ConsentRecord> findByPatientIdAndTenantIdAndConsentType(UUID patientId, UUID tenantId, ConsentType consentType);

    Optional<ConsentRecord> findByPatientIdAndTenantIdAndConsentTypeAndStatus(UUID patientId, UUID tenantId,
                                                                               ConsentType consentType, ConsentStatus status);

    // Status-based queries
    List<ConsentRecord> findByPatientIdAndTenantIdAndStatus(UUID patientId, UUID tenantId, ConsentStatus status);

    @Query("SELECT c FROM ConsentRecord c WHERE c.tenantId = :tenantId AND c.status = :status " +
           "ORDER BY c.grantedAt DESC")
    Page<ConsentRecord> findByStatus(@Param("tenantId") UUID tenantId,
                                      @Param("status") ConsentStatus status,
                                      Pageable pageable);

    // Active consents (not revoked, not expired)
    @Query("SELECT c FROM ConsentRecord c WHERE c.patient.id = :patientId AND c.tenantId = :tenantId AND " +
           "c.status = 'GRANTED' AND (c.expiresAt IS NULL OR c.expiresAt > :now)")
    List<ConsentRecord> findActiveConsentsForPatient(@Param("patientId") UUID patientId,
                                                      @Param("tenantId") UUID tenantId,
                                                      @Param("now") Instant now);

    // Specific purpose consent
    @Query("SELECT c FROM ConsentRecord c WHERE c.patient.id = :patientId AND c.tenantId = :tenantId AND " +
           "c.consentType = :type AND c.status = 'GRANTED' AND " +
           "(c.expiresAt IS NULL OR c.expiresAt > :now)")
    Optional<ConsentRecord> findActiveConsentByType(@Param("patientId") UUID patientId,
                                                     @Param("tenantId") UUID tenantId,
                                                     @Param("type") ConsentType type,
                                                     @Param("now") Instant now);

    // Expired consents
    @Query("SELECT c FROM ConsentRecord c WHERE c.tenantId = :tenantId AND c.expiresAt IS NOT NULL AND " +
           "c.expiresAt < :now AND c.status = 'GRANTED'")
    List<ConsentRecord> findExpiredConsents(@Param("tenantId") UUID tenantId, @Param("now") Instant now);

    // Consents expiring soon
    @Query("SELECT c FROM ConsentRecord c WHERE c.tenantId = :tenantId AND c.status = 'GRANTED' AND " +
           "c.expiresAt BETWEEN :now AND :futureDate")
    List<ConsentRecord> findExpiringConsents(@Param("tenantId") UUID tenantId,
                                              @Param("now") Instant now,
                                              @Param("futureDate") Instant futureDate);

    // Consent version queries
    @Query("SELECT c FROM ConsentRecord c WHERE c.tenantId = :tenantId AND c.consentType = :type AND " +
           "c.consentVersion = :version ORDER BY c.grantedAt DESC")
    List<ConsentRecord> findByConsentTypeAndVersion(@Param("tenantId") UUID tenantId,
                                                     @Param("type") ConsentType type,
                                                     @Param("version") String version);

    // Granted by user
    @Query("SELECT c FROM ConsentRecord c WHERE c.grantedBy.id = :userId AND c.tenantId = :tenantId " +
           "ORDER BY c.grantedAt DESC")
    Page<ConsentRecord> findByGrantedBy(@Param("userId") UUID userId,
                                         @Param("tenantId") UUID tenantId,
                                         Pageable pageable);

    // IP-based audit (security monitoring)
    @Query("SELECT c FROM ConsentRecord c WHERE c.ipAddress = :ipAddress AND c.tenantId = :tenantId AND " +
           "c.grantedAt >= :since ORDER BY c.grantedAt DESC")
    List<ConsentRecord> findByIpAddressSince(@Param("ipAddress") String ipAddress,
                                              @Param("tenantId") UUID tenantId,
                                              @Param("since") Instant since);

    // Counting
    long countByPatientIdAndTenantIdAndStatus(UUID patientId, UUID tenantId, ConsentStatus status);

    @Query("SELECT COUNT(c) FROM ConsentRecord c WHERE c.patient.id = :patientId AND c.tenantId = :tenantId AND " +
           "c.status = 'GRANTED' AND (c.expiresAt IS NULL OR c.expiresAt > :now)")
    long countActiveConsentsForPatient(@Param("patientId") UUID patientId,
                                        @Param("tenantId") UUID tenantId,
                                        @Param("now") Instant now);
}
