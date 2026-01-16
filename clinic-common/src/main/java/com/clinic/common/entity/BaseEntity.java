package com.clinic.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();

        // Temporal invariant: createdAt <= updatedAt (Sequences & Recurrence)
        if (createdAt != null && createdAt.isAfter(updatedAt)) {
            throw new IllegalStateException(
                "Invariant violation: createdAt cannot be after updatedAt"
            );
        }
    }

    /**
     * Equivalence relation implementation (Discrete Math: Reflexive, Symmetric, Transitive)
     * Two entities are equal if they have the same ID
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // Reflexive
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity that = (BaseEntity) o;
        return Objects.equals(id, that.id); // Symmetric and Transitive by UUID equality
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
