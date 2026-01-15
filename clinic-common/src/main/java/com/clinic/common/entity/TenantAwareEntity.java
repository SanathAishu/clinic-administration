package com.clinic.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
public abstract class TenantAwareEntity extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
}
