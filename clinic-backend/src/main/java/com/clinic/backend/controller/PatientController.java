package com.clinic.backend.controller;

import com.clinic.backend.mapper.PatientMapper;
import com.clinic.backend.service.PatientService;
import com.clinic.backend.service.PatientViewService;
import com.clinic.backend.service.UserService;
import com.clinic.common.dto.request.CreatePatientRequest;
import com.clinic.common.dto.request.UpdatePatientRequest;
import com.clinic.common.dto.response.PatientResponseDTO;
import com.clinic.common.dto.view.*;
import com.clinic.common.entity.core.User;
import com.clinic.common.entity.patient.Patient;
import com.clinic.backend.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for Patient operations.
 *
 * Implements CQRS pattern:
 * - READ operations: Use database views via PatientViewService
 * - WRITE operations: Use JPA entities via PatientService
 *
 * All operations are tenant-scoped via TenantContext.
 */
@Slf4j
@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@Tag(name = "Patients", description = "Patient management endpoints")
public class PatientController {

    private final PatientService patientService;
    private final PatientViewService patientViewService;
    private final PatientMapper patientMapper;
    private final UserService userService;

    // ============================
    // READ Operations (Views)
    // ============================

    /**
     * Get paginated list of patients.
     * Uses v_patient_list database view.
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('PATIENT_READ', 'PATIENT_LIST', 'ADMIN')")
    @Operation(
        summary = "List all patients",
        description = "Get a paginated list of active patients for the current tenant"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved patient list"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    public ResponseEntity<Page<PatientListViewDTO>> getAllPatients(
            @PageableDefault(size = 20, sort = "fullName", direction = Sort.Direction.ASC)
            Pageable pageable) {

        log.debug("GET /api/patients - fetching patient list");
        Page<PatientListViewDTO> patients = patientViewService.getPatientList(pageable);
        return ResponseEntity.ok(patients);
    }

    /**
     * Search patients by name, phone, email, or ABHA ID.
     * Uses v_patient_list database view.
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('PATIENT_READ', 'PATIENT_SEARCH', 'ADMIN')")
    @Operation(
        summary = "Search patients",
        description = "Search patients by name, phone, email, or ABHA ID"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved search results"),
        @ApiResponse(responseCode = "400", description = "Invalid search parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<PatientListViewDTO>> searchPatients(
            @Parameter(description = "Search term (name, phone, email, or ABHA ID)")
            @RequestParam("q") String searchTerm,
            @PageableDefault(size = 20, sort = "fullName", direction = Sort.Direction.ASC)
            Pageable pageable) {

        log.debug("GET /api/patients/search?q={}", searchTerm);

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Page<PatientListViewDTO> results = patientViewService.searchPatients(searchTerm.trim(), pageable);
        return ResponseEntity.ok(results);
    }

    /**
     * Get detailed patient information including latest vitals and summary counts.
     * Uses v_patient_detail database view.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PATIENT_READ', 'PATIENT_VIEW', 'ADMIN')")
    @Operation(
        summary = "Get patient details",
        description = "Get detailed patient information including latest vitals and summary counts"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved patient details",
                    content = @Content(schema = @Schema(implementation = PatientDetailViewDTO.class))),
        @ApiResponse(responseCode = "404", description = "Patient not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<PatientDetailViewDTO> getPatientDetail(
            @Parameter(description = "Patient ID")
            @PathVariable UUID id) {

        log.debug("GET /api/patients/{}", id);

        return patientViewService.getPatientDetail(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get patient's appointment history.
     * Uses v_patient_appointments database view.
     */
    @GetMapping("/{id}/appointments")
    @PreAuthorize("hasAnyAuthority('PATIENT_READ', 'APPOINTMENT_READ', 'ADMIN')")
    @Operation(
        summary = "Get patient appointments",
        description = "Get paginated appointment history for a patient"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved appointments"),
        @ApiResponse(responseCode = "404", description = "Patient not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<PatientAppointmentViewDTO>> getPatientAppointments(
            @Parameter(description = "Patient ID")
            @PathVariable UUID id,
            @PageableDefault(size = 20, sort = "appointmentTime", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.debug("GET /api/patients/{}/appointments", id);

        // Verify patient exists
        if (patientViewService.getPatientDetail(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Page<PatientAppointmentViewDTO> appointments = patientViewService.getPatientAppointments(id, pageable);
        return ResponseEntity.ok(appointments);
    }

    /**
     * Get patient's medical record history.
     * Uses v_patient_medical_history database view.
     */
    @GetMapping("/{id}/medical-history")
    @PreAuthorize("hasAnyAuthority('PATIENT_READ', 'MEDICAL_RECORD_READ', 'ADMIN')")
    @Operation(
        summary = "Get patient medical history",
        description = "Get paginated medical record history for a patient"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved medical history"),
        @ApiResponse(responseCode = "404", description = "Patient not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<PatientMedicalHistoryViewDTO>> getPatientMedicalHistory(
            @Parameter(description = "Patient ID")
            @PathVariable UUID id,
            @PageableDefault(size = 20, sort = "recordDate", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.debug("GET /api/patients/{}/medical-history", id);

        // Verify patient exists
        if (patientViewService.getPatientDetail(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Page<PatientMedicalHistoryViewDTO> medicalHistory = patientViewService.getPatientMedicalHistory(id, pageable);
        return ResponseEntity.ok(medicalHistory);
    }

    /**
     * Get patient's billing history.
     * Uses v_patient_billing_history database view.
     */
    @GetMapping("/{id}/billing")
    @PreAuthorize("hasAnyAuthority('PATIENT_READ', 'BILLING_READ', 'ADMIN')")
    @Operation(
        summary = "Get patient billing history",
        description = "Get paginated billing history for a patient"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved billing history"),
        @ApiResponse(responseCode = "404", description = "Patient not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<PatientBillingHistoryViewDTO>> getPatientBillingHistory(
            @Parameter(description = "Patient ID")
            @PathVariable UUID id,
            @PageableDefault(size = 20, sort = "invoiceDate", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.debug("GET /api/patients/{}/billing", id);

        // Verify patient exists
        if (patientViewService.getPatientDetail(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Page<PatientBillingHistoryViewDTO> billingHistory = patientViewService.getPatientBillingHistory(id, pageable);
        return ResponseEntity.ok(billingHistory);
    }

    // ============================
    // WRITE Operations (Entities)
    // ============================

    /**
     * Create a new patient.
     * Uses PatientService with JPA entity for write operation.
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('PATIENT_CREATE', 'ADMIN')")
    @Operation(
        summary = "Create patient",
        description = "Create a new patient record"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Patient created successfully",
                    content = @Content(schema = @Schema(implementation = PatientResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "409", description = "Conflict - duplicate email/phone/ABHA ID")
    })
    public ResponseEntity<PatientResponseDTO> createPatient(
            @Valid @RequestBody CreatePatientRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("POST /api/patients - creating patient: {}", request.getEmail());

        UUID tenantId = getCurrentTenantId();
        UUID userId = UUID.fromString(userDetails.getUsername());
        User currentUser = userService.getUserById(userId, tenantId);

        // Map request to entity
        Patient patient = patientMapper.toEntity(request);
        patient.setCreatedBy(currentUser);

        // Create patient
        Patient created = patientService.createPatient(patient, tenantId);

        // Map to response DTO
        PatientResponseDTO response = patientMapper.toResponseDTO(created);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an existing patient.
     * Uses PatientService with JPA entity for write operation.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PATIENT_UPDATE', 'ADMIN')")
    @Operation(
        summary = "Update patient",
        description = "Update an existing patient record"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Patient updated successfully",
                    content = @Content(schema = @Schema(implementation = PatientResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Patient not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "409", description = "Conflict - duplicate email/phone")
    })
    public ResponseEntity<PatientResponseDTO> updatePatient(
            @Parameter(description = "Patient ID")
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePatientRequest request) {

        log.debug("PUT /api/patients/{}", id);

        UUID tenantId = getCurrentTenantId();

        // Get existing patient
        Patient existingPatient = patientService.getPatientById(id, tenantId);

        // Apply updates using mapper
        patientMapper.updateEntityFromRequest(request, existingPatient);

        // Save updates
        Patient updated = patientService.updatePatient(id, tenantId, existingPatient);

        // Map to response DTO
        PatientResponseDTO response = patientMapper.toResponseDTO(updated);

        return ResponseEntity.ok(response);
    }

    /**
     * Soft delete a patient.
     * Uses PatientService with JPA entity for write operation.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PATIENT_DELETE', 'ADMIN')")
    @Operation(
        summary = "Delete patient",
        description = "Soft delete a patient (marks as inactive)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Patient deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Patient not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Void> deletePatient(
            @Parameter(description = "Patient ID")
            @PathVariable UUID id) {

        log.debug("DELETE /api/patients/{}", id);

        UUID tenantId = getCurrentTenantId();
        patientService.softDeletePatient(id, tenantId);

        return ResponseEntity.noContent().build();
    }

    // ============================
    // Helper Methods
    // ============================

    /**
     * Get the current tenant ID from TenantContext.
     * Throws IllegalStateException if tenant context is not set.
     */
    private UUID getCurrentTenantId() {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context not set. Ensure authentication is complete.");
        }
        return tenantId;
    }
}
