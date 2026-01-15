package com.clinic.backend.entity;

import com.clinic.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "permissions", indexes = {
    @Index(name = "idx_permissions_resource", columnList = "resource")
}, uniqueConstraints = {
    @UniqueConstraint(name = "permissions_resource_action_unique", columnNames = {"resource", "action"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    @NotBlank(message = "Permission name is required")
    @Size(max = 100)
    private String name;

    @Column(name = "resource", nullable = false, length = 100)
    @NotBlank(message = "Resource is required")
    @Size(max = 100)
    private String resource;

    @Column(name = "action", nullable = false, length = 50)
    @NotBlank(message = "Action is required")
    @Size(max = 50)
    private String action;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToMany(mappedBy = "permissions")
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
}
