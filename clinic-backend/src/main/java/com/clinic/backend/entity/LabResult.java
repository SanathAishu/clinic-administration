package com.clinic.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lab_results", indexes = {
    @Index(name = "idx_lab_results_test", columnList = "lab_test_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabResult {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_test_id", nullable = false)
    @NotNull
    private LabTest labTest;

    // Result Details
    @Column(name = "parameter_name", nullable = false)
    @NotBlank(message = "Parameter name is required")
    @Size(max = 255)
    private String parameterName;

    @Column(name = "result_value", nullable = false)
    @NotBlank(message = "Result value is required")
    @Size(max = 255)
    private String resultValue;

    @Column(name = "unit", length = 50)
    @Size(max = 50)
    private String unit;

    @Column(name = "reference_range", length = 100)
    @Size(max = 100)
    private String referenceRange;

    @Column(name = "is_abnormal")
    private Boolean isAbnormal = false;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    // Metadata
    @Column(name = "result_date", nullable = false)
    @NotNull
    private Instant resultDate = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entered_by")
    private User enteredBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (resultDate == null) {
            resultDate = Instant.now();
        }
    }
}
