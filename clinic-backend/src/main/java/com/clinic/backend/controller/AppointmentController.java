package com.clinic.backend.controller;

import com.clinic.backend.mapper.AppointmentMapper;
import com.clinic.backend.repository.PatientRepository;
import com.clinic.backend.repository.UserRepository;
import com.clinic.backend.service.AppointmentService;
import com.clinic.common.dto.request.CancelAppointmentRequest;
import com.clinic.common.dto.request.CreateAppointmentRequest;
import com.clinic.common.dto.request.UpdateAppointmentRequest;
import com.clinic.common.dto.response.AppointmentResponseDTO;
import com.clinic.common.dto.view.AppointmentDetailViewDTO;
import com.clinic.common.dto.view.AppointmentListViewDTO;
import com.clinic.common.dto.view.AvailableSlotDTO;
import com.clinic.common.dto.view.TodayAppointmentViewDTO;
import com.clinic.common.entity.clinical.Appointment;
import com.clinic.common.entity.core.User;
import com.clinic.common.entity.patient.Patient;
import com.clinic.common.enums.AppointmentStatus;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Appointment management implementing CQRS pattern.
 *
 * CQRS Architecture:
 * - READ endpoints: Use database views for optimized queries
 * - WRITE endpoints: Use JPA entities for data manipulation
 *
 * All endpoints are tenant-scoped using TenantContext.
 */
@Slf4j
@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Appointment management endpoints")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AppointmentMapper appointmentMapper;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;

    // ================================
    // READ Endpoints (View-based CQRS)
    // ================================

    /**
     * GET /api/appointments
     * List appointments with pagination (uses v_appointment_list view).
     */
    @GetMapping
    @Operation(
            summary = "List appointments",
            description = "Get paginated list of appointments using v_appointment_list view"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved appointments"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<AppointmentListViewDTO>> listAppointments(
            @Parameter(description = "Filter by status")
            @RequestParam(required = false) AppointmentStatus status,
            @PageableDefault(size = 20, sort = "appointmentTime", direction = Sort.Direction.DESC)
            Pageable pageable) {

        UUID tenantId = getTenantId();
        log.debug("Listing appointments for tenant: {}, status: {}", tenantId, status);

        Page<AppointmentListViewDTO> appointments;
        if (status != null) {
            appointments = appointmentService.getAppointmentListByStatus(tenantId, status, pageable);
        } else {
            appointments = appointmentService.getAppointmentList(tenantId, pageable);
        }

        return ResponseEntity.ok(appointments);
    }

    /**
     * GET /api/appointments/{id}
     * Get detailed appointment information (uses v_appointment_detail view).
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Get appointment details",
            description = "Get detailed appointment information using v_appointment_detail view"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved appointment"),
            @ApiResponse(responseCode = "404", description = "Appointment not found")
    })
    public ResponseEntity<AppointmentDetailViewDTO> getAppointmentDetail(
            @Parameter(description = "Appointment ID", required = true)
            @PathVariable UUID id) {

        UUID tenantId = getTenantId();
        log.debug("Fetching appointment detail: {} for tenant: {}", id, tenantId);

        AppointmentDetailViewDTO appointment = appointmentService.getAppointmentDetail(tenantId, id);
        return ResponseEntity.ok(appointment);
    }

    /**
     * GET /api/appointments/today
     * Get today's appointments for dashboard (uses v_today_appointments view).
     */
    @GetMapping("/today")
    @Operation(
            summary = "Get today's appointments",
            description = "Get today's appointments for dashboard display using v_today_appointments view"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved today's appointments")
    public ResponseEntity<List<TodayAppointmentViewDTO>> getTodayAppointments() {

        UUID tenantId = getTenantId();
        log.debug("Fetching today's appointments for tenant: {}", tenantId);

        List<TodayAppointmentViewDTO> appointments = appointmentService.getTodayAppointments(tenantId);
        return ResponseEntity.ok(appointments);
    }

    /**
     * GET /api/appointments/doctor/{doctorId}
     * Get appointments for a specific doctor.
     */
    @GetMapping("/doctor/{doctorId}")
    @Operation(
            summary = "Get doctor's appointments",
            description = "Get paginated appointments for a specific doctor"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved doctor's appointments"),
            @ApiResponse(responseCode = "404", description = "Doctor not found")
    })
    public ResponseEntity<Page<AppointmentListViewDTO>> getDoctorAppointments(
            @Parameter(description = "Doctor ID", required = true)
            @PathVariable UUID doctorId,
            @PageableDefault(size = 20, sort = "appointmentTime", direction = Sort.Direction.DESC)
            Pageable pageable) {

        UUID tenantId = getTenantId();
        log.debug("Fetching appointments for doctor: {} in tenant: {}", doctorId, tenantId);

        // Validate doctor exists
        userRepository.findByIdAndTenantIdAndDeletedAtIsNull(doctorId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found: " + doctorId));

        Page<AppointmentListViewDTO> appointments = appointmentService.getDoctorAppointmentList(tenantId, doctorId, pageable);
        return ResponseEntity.ok(appointments);
    }

    /**
     * GET /api/appointments/patient/{patientId}
     * Get appointments for a specific patient.
     */
    @GetMapping("/patient/{patientId}")
    @Operation(
            summary = "Get patient's appointments",
            description = "Get paginated appointments for a specific patient"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved patient's appointments"),
            @ApiResponse(responseCode = "404", description = "Patient not found")
    })
    public ResponseEntity<Page<AppointmentListViewDTO>> getPatientAppointments(
            @Parameter(description = "Patient ID", required = true)
            @PathVariable UUID patientId,
            @PageableDefault(size = 20, sort = "appointmentTime", direction = Sort.Direction.DESC)
            Pageable pageable) {

        UUID tenantId = getTenantId();
        log.debug("Fetching appointments for patient: {} in tenant: {}", patientId, tenantId);

        // Validate patient exists
        patientRepository.findByIdAndTenantIdAndDeletedAtIsNull(patientId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + patientId));

        Page<AppointmentListViewDTO> appointments = appointmentService.getPatientAppointmentList(tenantId, patientId, pageable);
        return ResponseEntity.ok(appointments);
    }

    /**
     * GET /api/appointments/available-slots
     * Get available time slots for a doctor on a specific date.
     */
    @GetMapping("/available-slots")
    @Operation(
            summary = "Get available time slots",
            description = "Calculate available time slots for a doctor on a specific date"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved available slots"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "404", description = "Doctor not found")
    })
    public ResponseEntity<List<AvailableSlotDTO>> getAvailableSlots(
            @Parameter(description = "Doctor ID", required = true)
            @RequestParam UUID doctorId,
            @Parameter(description = "Date for available slots (YYYY-MM-DD)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Slot duration in minutes (default: 30)")
            @RequestParam(defaultValue = "30") int duration) {

        UUID tenantId = getTenantId();
        log.debug("Fetching available slots for doctor: {} on date: {} in tenant: {}", doctorId, date, tenantId);

        // Validate doctor exists
        userRepository.findByIdAndTenantIdAndDeletedAtIsNull(doctorId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found: " + doctorId));

        // Validate date is not in the past
        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot get available slots for past dates");
        }

        List<AvailableSlotDTO> slots = appointmentService.getAvailableSlots(tenantId, doctorId, date, duration);
        return ResponseEntity.ok(slots);
    }

    // ================================
    // WRITE Endpoints (Entity-based)
    // ================================

    /**
     * POST /api/appointments
     * Create a new appointment.
     */
    @PostMapping
    @Operation(
            summary = "Create appointment",
            description = "Create a new appointment after validating doctor availability"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Appointment created successfully",
                    content = @Content(schema = @Schema(implementation = AppointmentResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "409", description = "Schedule conflict")
    })
    public ResponseEntity<AppointmentResponseDTO> createAppointment(
            @Valid @RequestBody CreateAppointmentRequest request) {

        UUID tenantId = getTenantId();
        log.info("Creating appointment for patient: {} with doctor: {} in tenant: {}",
                request.getPatientId(), request.getDoctorId(), tenantId);

        // Fetch patient
        Patient patient = patientRepository.findByIdAndTenantIdAndDeletedAtIsNull(request.getPatientId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + request.getPatientId()));

        // Fetch doctor
        User doctor = userRepository.findByIdAndTenantIdAndDeletedAtIsNull(request.getDoctorId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found: " + request.getDoctorId()));

        // Map request to entity
        Appointment appointment = appointmentMapper.toEntity(request);
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);

        // Set created by (in real app, get from security context)
        // For now, use the doctor as the creator
        appointment.setCreatedBy(doctor);

        // Create appointment (service handles overlap validation)
        Appointment created = appointmentService.createAppointment(appointment, tenantId);

        AppointmentResponseDTO response = appointmentMapper.toResponseDTO(created);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/appointments/{id}
     * Update an existing appointment.
     */
    @PutMapping("/{id}")
    @Operation(
            summary = "Update appointment",
            description = "Update an existing appointment (not allowed for COMPLETED or CANCELLED)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Appointment updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or cannot update"),
            @ApiResponse(responseCode = "404", description = "Appointment not found"),
            @ApiResponse(responseCode = "409", description = "Schedule conflict")
    })
    public ResponseEntity<AppointmentResponseDTO> updateAppointment(
            @Parameter(description = "Appointment ID", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAppointmentRequest request) {

        UUID tenantId = getTenantId();
        log.info("Updating appointment: {} in tenant: {}", id, tenantId);

        // Map updates to entity
        Appointment updates = new Appointment();
        updates.setAppointmentTime(request.getAppointmentTime());
        updates.setDurationMinutes(request.getDurationMinutes());
        updates.setConsultationType(request.getConsultationType());
        updates.setReason(request.getReason());

        Appointment updated = appointmentService.updateAppointment(id, tenantId, updates);

        AppointmentResponseDTO response = appointmentMapper.toResponseDTO(updated);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/appointments/{id}
     * Soft delete (cancel) an appointment.
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete appointment",
            description = "Soft delete (cancel) an appointment"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Appointment deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Appointment not found")
    })
    public ResponseEntity<Void> deleteAppointment(
            @Parameter(description = "Appointment ID", required = true)
            @PathVariable UUID id) {

        UUID tenantId = getTenantId();
        log.info("Soft deleting appointment: {} in tenant: {}", id, tenantId);

        appointmentService.softDeleteAppointment(id, tenantId);
        return ResponseEntity.noContent().build();
    }

    // ================================
    // Status Management Endpoints
    // ================================

    /**
     * POST /api/appointments/{id}/confirm
     * Confirm a scheduled appointment.
     */
    @PostMapping("/{id}/confirm")
    @Operation(
            summary = "Confirm appointment",
            description = "Transition appointment from SCHEDULED to CONFIRMED"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Appointment confirmed"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "404", description = "Appointment not found")
    })
    public ResponseEntity<AppointmentResponseDTO> confirmAppointment(
            @Parameter(description = "Appointment ID", required = true)
            @PathVariable UUID id) {

        UUID tenantId = getTenantId();
        log.info("Confirming appointment: {} in tenant: {}", id, tenantId);

        Appointment confirmed = appointmentService.confirmAppointment(id, tenantId);

        AppointmentResponseDTO response = appointmentMapper.toResponseDTO(confirmed);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/appointments/{id}/start
     * Start a confirmed appointment (begin consultation).
     */
    @PostMapping("/{id}/start")
    @Operation(
            summary = "Start appointment",
            description = "Transition appointment from CONFIRMED to IN_PROGRESS"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Appointment started"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "404", description = "Appointment not found")
    })
    public ResponseEntity<AppointmentResponseDTO> startAppointment(
            @Parameter(description = "Appointment ID", required = true)
            @PathVariable UUID id) {

        UUID tenantId = getTenantId();
        log.info("Starting appointment: {} in tenant: {}", id, tenantId);

        Appointment started = appointmentService.startAppointment(id, tenantId);

        AppointmentResponseDTO response = appointmentMapper.toResponseDTO(started);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/appointments/{id}/complete
     * Complete an in-progress appointment.
     */
    @PostMapping("/{id}/complete")
    @Operation(
            summary = "Complete appointment",
            description = "Transition appointment from IN_PROGRESS to COMPLETED"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Appointment completed"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "404", description = "Appointment not found")
    })
    public ResponseEntity<AppointmentResponseDTO> completeAppointment(
            @Parameter(description = "Appointment ID", required = true)
            @PathVariable UUID id) {

        UUID tenantId = getTenantId();
        log.info("Completing appointment: {} in tenant: {}", id, tenantId);

        Appointment completed = appointmentService.completeAppointment(id, tenantId);

        AppointmentResponseDTO response = appointmentMapper.toResponseDTO(completed);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/appointments/{id}/cancel
     * Cancel an appointment with reason.
     */
    @PostMapping("/{id}/cancel")
    @Operation(
            summary = "Cancel appointment",
            description = "Cancel a SCHEDULED or CONFIRMED appointment with reason"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Appointment cancelled"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition or missing reason"),
            @ApiResponse(responseCode = "404", description = "Appointment not found")
    })
    public ResponseEntity<AppointmentResponseDTO> cancelAppointment(
            @Parameter(description = "Appointment ID", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody CancelAppointmentRequest request) {

        UUID tenantId = getTenantId();
        log.info("Cancelling appointment: {} in tenant: {} with reason: {}", id, tenantId, request.getReason());

        // Get current user as canceller (in real app, get from security context)
        // For now, we'll use a placeholder - the appointment's doctor
        Appointment appointment = appointmentService.getAppointmentById(id, tenantId);
        User cancelledBy = appointment.getDoctor();

        Appointment cancelled = appointmentService.cancelAppointment(id, tenantId, cancelledBy, request.getReason());

        AppointmentResponseDTO response = appointmentMapper.toResponseDTO(cancelled);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/appointments/{id}/no-show
     * Mark appointment as no-show.
     */
    @PostMapping("/{id}/no-show")
    @Operation(
            summary = "Mark as no-show",
            description = "Mark a CONFIRMED appointment as NO_SHOW when patient doesn't arrive"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Appointment marked as no-show"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "404", description = "Appointment not found")
    })
    public ResponseEntity<AppointmentResponseDTO> markNoShow(
            @Parameter(description = "Appointment ID", required = true)
            @PathVariable UUID id) {

        UUID tenantId = getTenantId();
        log.info("Marking appointment as no-show: {} in tenant: {}", id, tenantId);

        Appointment noShow = appointmentService.markNoShow(id, tenantId);

        AppointmentResponseDTO response = appointmentMapper.toResponseDTO(noShow);
        return ResponseEntity.ok(response);
    }

    // ================================
    // Helper Methods
    // ================================

    /**
     * Get current tenant ID from TenantContext.
     * Throws exception if not set.
     */
    private UUID getTenantId() {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context not set");
        }
        return tenantId;
    }
}
