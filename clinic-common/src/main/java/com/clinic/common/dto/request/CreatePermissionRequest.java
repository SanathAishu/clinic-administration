package com.clinic.common.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePermissionRequest {

    @NotBlank(message = "Permission name is required")
    @Size(max = 100, message = "Permission name cannot exceed 100 characters")
    private String name;

    @NotBlank(message = "Resource is required")
    @Size(max = 100, message = "Resource cannot exceed 100 characters")
    @Pattern(regexp = "^[A-Z_]+$", message = "Resource must be uppercase with underscores (e.g., PATIENT_RECORD)")
    private String resource;

    @NotBlank(message = "Action is required")
    @Size(max = 50, message = "Action cannot exceed 50 characters")
    @Pattern(regexp = "^[A-Z_]+$", message = "Action must be uppercase with underscores (e.g., CREATE, READ, UPDATE)")
    private String action;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
}
