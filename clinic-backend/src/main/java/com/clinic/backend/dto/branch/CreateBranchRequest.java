package com.clinic.backend.dto.branch;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBranchRequest {

    @NotBlank(message = "Branch code is required")
    @Pattern(regexp = "^[A-Z0-9_-]{2,50}$",
             message = "Branch code must be 2-50 uppercase letters, numbers, underscores or hyphens")
    private String branchCode;

    @NotBlank(message = "Branch name is required")
    @Size(max = 255, message = "Branch name cannot exceed 255 characters")
    private String name;

    @Size(max = 500, message = "Address cannot exceed 500 characters")
    private String address;

    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;

    @Size(max = 100, message = "State cannot exceed 100 characters")
    private String state;

    @Size(max = 10, message = "Pincode cannot exceed 10 characters")
    private String pincode;

    @Size(max = 100, message = "Country cannot exceed 100 characters")
    @Builder.Default
    private String country = "India";

    @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$", message = "Phone number must be valid")
    private String phone;

    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    private String email;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @Size(max = 1000, message = "Operating hours cannot exceed 1000 characters")
    private String operatingHours;

    @Size(max = 1000, message = "Facilities cannot exceed 1000 characters")
    private String facilities;

    @Min(value = 1, message = "Max patients per day must be at least 1")
    private Integer maxPatientsPerDay;

    @Min(value = 1, message = "Max concurrent appointments must be at least 1")
    private Integer maxConcurrentAppointments;

    @NotNull(message = "Active status is required")
    @Builder.Default
    private Boolean isActive = true;

    @NotNull(message = "Main branch flag is required")
    @Builder.Default
    private Boolean isMainBranch = false;
}
