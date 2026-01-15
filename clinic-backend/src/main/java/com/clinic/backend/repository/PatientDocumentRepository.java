package com.clinic.backend.repository;

import com.clinic.backend.entity.PatientDocument;
import com.clinic.common.enums.DocumentType;
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
public interface PatientDocumentRepository extends JpaRepository<PatientDocument, UUID> {

    // Tenant-scoped queries
    Page<PatientDocument> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Optional<PatientDocument> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    // Patient documents
    @Query("SELECT pd FROM PatientDocument pd WHERE pd.patient.id = :patientId AND pd.tenantId = :tenantId AND " +
           "pd.deletedAt IS NULL ORDER BY pd.uploadedAt DESC")
    Page<PatientDocument> findPatientDocuments(@Param("patientId") UUID patientId,
                                                @Param("tenantId") UUID tenantId,
                                                Pageable pageable);

    List<PatientDocument> findByPatientIdAndTenantIdAndDeletedAtIsNullOrderByUploadedAtDesc(UUID patientId, UUID tenantId);

    // Document type queries
    List<PatientDocument> findByPatientIdAndTenantIdAndDocumentTypeAndDeletedAtIsNull(UUID patientId, UUID tenantId, DocumentType documentType);

    @Query("SELECT pd FROM PatientDocument pd WHERE pd.tenantId = :tenantId AND pd.documentType = :type AND " +
           "pd.deletedAt IS NULL ORDER BY pd.uploadedAt DESC")
    Page<PatientDocument> findByDocumentType(@Param("tenantId") UUID tenantId,
                                              @Param("type") DocumentType type,
                                              Pageable pageable);

    // File name search
    @Query("SELECT pd FROM PatientDocument pd WHERE pd.patient.id = :patientId AND pd.tenantId = :tenantId AND " +
           "LOWER(pd.fileName) LIKE LOWER(CONCAT('%', :fileName, '%')) AND pd.deletedAt IS NULL")
    List<PatientDocument> searchPatientDocumentsByFileName(@Param("patientId") UUID patientId,
                                                            @Param("tenantId") UUID tenantId,
                                                            @Param("fileName") String fileName);

    // Title search
    @Query("SELECT pd FROM PatientDocument pd WHERE pd.patient.id = :patientId AND pd.tenantId = :tenantId AND " +
           "LOWER(pd.title) LIKE LOWER(CONCAT('%', :title, '%')) AND pd.deletedAt IS NULL")
    List<PatientDocument> searchPatientDocumentsByTitle(@Param("patientId") UUID patientId,
                                                         @Param("tenantId") UUID tenantId,
                                                         @Param("title") String title);

    // Reference-based queries
    @Query("SELECT pd FROM PatientDocument pd WHERE pd.referenceType = :referenceType AND pd.referenceId = :referenceId AND " +
           "pd.tenantId = :tenantId AND pd.deletedAt IS NULL ORDER BY pd.uploadedAt DESC")
    List<PatientDocument> findByReference(@Param("referenceType") String referenceType,
                                           @Param("referenceId") UUID referenceId,
                                           @Param("tenantId") UUID tenantId);

    // Storage path lookup (Bijective mapping: path ↔ document)
    Optional<PatientDocument> findByStoragePathAndTenantIdAndDeletedAtIsNull(String storagePath, UUID tenantId);

    // Checksum lookup (for duplicate detection)
    @Query("SELECT pd FROM PatientDocument pd WHERE pd.checksum = :checksum AND pd.patient.id = :patientId AND " +
           "pd.tenantId = :tenantId AND pd.deletedAt IS NULL")
    List<PatientDocument> findByChecksumForPatient(@Param("checksum") String checksum,
                                                    @Param("patientId") UUID patientId,
                                                    @Param("tenantId") UUID tenantId);

    // Uploaded by user
    @Query("SELECT pd FROM PatientDocument pd WHERE pd.uploadedBy.id = :userId AND pd.tenantId = :tenantId AND " +
           "pd.deletedAt IS NULL ORDER BY pd.uploadedAt DESC")
    Page<PatientDocument> findByUploadedBy(@Param("userId") UUID userId,
                                            @Param("tenantId") UUID tenantId,
                                            Pageable pageable);

    // Date range queries
    @Query("SELECT pd FROM PatientDocument pd WHERE pd.patient.id = :patientId AND pd.tenantId = :tenantId AND " +
           "pd.uploadedAt BETWEEN :startDate AND :endDate AND pd.deletedAt IS NULL ORDER BY pd.uploadedAt DESC")
    List<PatientDocument> findPatientDocumentsInDateRange(@Param("patientId") UUID patientId,
                                                           @Param("tenantId") UUID tenantId,
                                                           @Param("startDate") Instant startDate,
                                                           @Param("endDate") Instant endDate);

    // File size queries
    @Query("SELECT pd FROM PatientDocument pd WHERE pd.tenantId = :tenantId AND pd.fileSizeBytes > :minSize AND " +
           "pd.deletedAt IS NULL ORDER BY pd.fileSizeBytes DESC")
    List<PatientDocument> findLargeDocuments(@Param("tenantId") UUID tenantId, @Param("minSize") Long minSize);

    @Query("SELECT SUM(pd.fileSizeBytes) FROM PatientDocument pd WHERE pd.patient.id = :patientId AND " +
           "pd.tenantId = :tenantId AND pd.deletedAt IS NULL")
    Long calculateTotalStorageForPatient(@Param("patientId") UUID patientId, @Param("tenantId") UUID tenantId);

    @Query("SELECT SUM(pd.fileSizeBytes) FROM PatientDocument pd WHERE pd.tenantId = :tenantId AND pd.deletedAt IS NULL")
    Long calculateTotalStorageForTenant(@Param("tenantId") UUID tenantId);

    // MIME type queries
    List<PatientDocument> findByPatientIdAndTenantIdAndMimeTypeAndDeletedAtIsNull(UUID patientId, UUID tenantId, String mimeType);

    // Recent documents
    @Query("SELECT pd FROM PatientDocument pd WHERE pd.tenantId = :tenantId AND pd.uploadedAt >= :since AND " +
           "pd.deletedAt IS NULL ORDER BY pd.uploadedAt DESC")
    List<PatientDocument> findRecentDocuments(@Param("tenantId") UUID tenantId, @Param("since") Instant since);

    // Counting
    long countByPatientIdAndTenantIdAndDeletedAtIsNull(UUID patientId, UUID tenantId);

    long countByPatientIdAndTenantIdAndDocumentTypeAndDeletedAtIsNull(UUID patientId, UUID tenantId, DocumentType documentType);
}
