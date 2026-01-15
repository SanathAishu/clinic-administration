package com.clinic.backend.entity;

import com.clinic.common.entity.SoftDeletableEntity;
import com.clinic.common.enums.DocumentType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "patient_documents", indexes = {
    @Index(name = "idx_patient_docs_tenant", columnList = "tenant_id"),
    @Index(name = "idx_patient_docs_patient", columnList = "patient_id, uploaded_at"),
    @Index(name = "idx_patient_docs_type", columnList = "document_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientDocument extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    @NotNull
    private Patient patient;

    // Document Details
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    @NotNull
    private DocumentType documentType;

    @Column(name = "title", nullable = false)
    @NotBlank(message = "Title is required")
    @Size(max = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // File Information
    @Column(name = "file_name", nullable = false)
    @NotBlank(message = "File name is required")
    @Size(max = 255)
    private String fileName;

    @Column(name = "file_size_bytes", nullable = false)
    @NotNull
    @Min(value = 1, message = "File size must be at least 1 byte")
    @Max(value = 10485760, message = "File size cannot exceed 10MB")
    private Long fileSizeBytes;

    @Column(name = "mime_type", nullable = false, length = 100)
    @NotBlank(message = "MIME type is required")
    @Size(max = 100)
    private String mimeType;

    @Column(name = "storage_path", nullable = false, length = 500)
    @NotBlank(message = "Storage path is required")
    @Size(max = 500)
    private String storagePath;

    @Column(name = "checksum", nullable = false, length = 64)
    @NotBlank(message = "Checksum is required")
    @Size(max = 64)
    private String checksum;

    // Reference
    @Column(name = "reference_type", length = 100)
    @Size(max = 100)
    private String referenceType;

    @Column(name = "reference_id")
    private UUID referenceId;

    // Metadata
    @Column(name = "uploaded_at", nullable = false)
    @NotNull
    private Instant uploadedAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    @NotNull
    private User uploadedBy;

    // Helper methods
    public String getFileExtension() {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }

    public double getFileSizeMB() {
        return fileSizeBytes / (1024.0 * 1024.0);
    }

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (uploadedAt == null) {
            uploadedAt = Instant.now();
        }
    }
}
