package com.clinic.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionResponseDTO {

    private UUID id;
    private String name;
    private String resource;
    private String action;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
}
