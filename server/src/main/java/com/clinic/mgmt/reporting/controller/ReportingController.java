package com.clinic.mgmt.reporting.controller;

import java.time.Instant;
import java.util.List;
import com.clinic.mgmt.reporting.dto.AppointmentAnalyticsReportResponse;
import com.clinic.mgmt.reporting.dto.BillingOutstandingReportResponse;
import com.clinic.mgmt.reporting.dto.DailySummaryReportResponse;
import com.clinic.mgmt.reporting.dto.HouseVisitKpiReportResponse;
import com.clinic.mgmt.reporting.dto.InventoryLevelReportResponse;
import com.clinic.mgmt.reporting.dto.OrderStatusReportResponse;
import com.clinic.mgmt.reporting.dto.PatientActivityReportResponse;
import com.clinic.mgmt.reporting.dto.PaymentMixReportResponse;
import com.clinic.mgmt.reporting.dto.RevenueAnalysisReportResponse;
import com.clinic.mgmt.reporting.dto.StaffPerformanceReportResponse;
import com.clinic.mgmt.reporting.dto.TreatmentOutcomesReportResponse;
import com.clinic.mgmt.reporting.service.ReportingService;
import com.clinic.mgmt.security.SecurityUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportingController {

	private final ReportingService service;

	public ReportingController(ReportingService service) {
		this.service = service;
	}

	@GetMapping("/daily-summary")
	@PreAuthorize("hasAuthority('reports.read')")
	public List<DailySummaryReportResponse> dailySummary(
			@RequestParam(value = "clinic_id", required = false) String clinicId,
			@RequestParam(value = "branch_id", required = false) String branchId,
			@RequestParam(value = "date_from", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
			@RequestParam(value = "date_to", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo
	) {
		return service.dailySummary(
				SecurityUtils.currentOrganizationId(),
				clinicId,
				branchId,
				dateFrom,
				dateTo
		);
	}

	@GetMapping("/revenue-analysis")
	@PreAuthorize("hasAuthority('reports.read')")
	public List<RevenueAnalysisReportResponse> revenueAnalysis(
			@RequestParam(value = "clinic_id", required = false) String clinicId,
			@RequestParam(value = "branch_id", required = false) String branchId,
			@RequestParam(value = "date_from", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
			@RequestParam(value = "date_to", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo,
			@RequestParam(value = "group_by", required = false) String groupBy
	) {
		return service.revenueAnalysis(
				SecurityUtils.currentOrganizationId(),
				clinicId,
				branchId,
				dateFrom,
				dateTo,
				groupBy
		);
	}

	@GetMapping("/payment-mix")
	@PreAuthorize("hasAuthority('reports.read')")
	public List<PaymentMixReportResponse> paymentMix(
			@RequestParam(value = "clinic_id", required = false) String clinicId,
			@RequestParam(value = "branch_id", required = false) String branchId,
			@RequestParam(value = "date_from", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
			@RequestParam(value = "date_to", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo
	) {
		return service.paymentMix(
				SecurityUtils.currentOrganizationId(),
				clinicId,
				branchId,
				dateFrom,
				dateTo
		);
	}

	@GetMapping("/billing-outstanding")
	@PreAuthorize("hasAuthority('reports.read')")
	public List<BillingOutstandingReportResponse> billingOutstanding(
			@RequestParam(value = "clinic_id", required = false) String clinicId,
			@RequestParam(value = "branch_id", required = false) String branchId,
			@RequestParam(value = "as_of", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant asOf
	) {
		return service.billingOutstanding(
				SecurityUtils.currentOrganizationId(),
				clinicId,
				branchId,
				asOf
		);
	}

	@GetMapping("/staff-performance")
	@PreAuthorize("hasAuthority('reports.read')")
	public List<StaffPerformanceReportResponse> staffPerformance(
			@RequestParam(value = "clinic_id", required = false) String clinicId,
			@RequestParam(value = "staff_type", required = false) String staffType,
			@RequestParam(value = "date_from", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
			@RequestParam(value = "date_to", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo
	) {
		return service.staffPerformance(
				SecurityUtils.currentOrganizationId(),
				clinicId,
				staffType,
				dateFrom,
				dateTo
		);
	}

	@GetMapping("/patient-activity")
	@PreAuthorize("hasAuthority('reports.read')")
	public List<PatientActivityReportResponse> patientActivity(
			@RequestParam(value = "clinic_id", required = false) String clinicId,
			@RequestParam(value = "branch_id", required = false) String branchId,
			@RequestParam(value = "date_from", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
			@RequestParam(value = "date_to", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo
	) {
		return service.patientActivity(
				SecurityUtils.currentOrganizationId(),
				clinicId,
				branchId,
				dateFrom,
				dateTo
		);
	}

	@GetMapping("/appointment-analytics")
	@PreAuthorize("hasAuthority('reports.read')")
	public List<AppointmentAnalyticsReportResponse> appointmentAnalytics(
			@RequestParam(value = "clinic_id", required = false) String clinicId,
			@RequestParam(value = "provider_id", required = false) String providerId,
			@RequestParam(value = "date_from", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
			@RequestParam(value = "date_to", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo
	) {
		return service.appointmentAnalytics(
				SecurityUtils.currentOrganizationId(),
				clinicId,
				providerId,
				dateFrom,
				dateTo
		);
	}

	@GetMapping("/treatment-outcomes")
	@PreAuthorize("hasAuthority('reports.read')")
	public List<TreatmentOutcomesReportResponse> treatmentOutcomes(
			@RequestParam(value = "clinic_id", required = false) String clinicId,
			@RequestParam(value = "treatment_type_id", required = false) String treatmentTypeId,
			@RequestParam(value = "date_from", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
			@RequestParam(value = "date_to", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo
	) {
		return service.treatmentOutcomes(
				SecurityUtils.currentOrganizationId(),
				clinicId,
				treatmentTypeId,
				dateFrom,
				dateTo
		);
	}

	@GetMapping("/inventory-levels")
	@PreAuthorize("hasAuthority('reports.read')")
	public List<InventoryLevelReportResponse> inventoryLevels(
			@RequestParam(value = "clinic_id", required = false) String clinicId,
			@RequestParam(value = "branch_id", required = false) String branchId,
			@RequestParam(value = "below_reorder", required = false) Boolean belowReorder
	) {
		return service.inventoryLevels(
				SecurityUtils.currentOrganizationId(),
				clinicId,
				branchId,
				belowReorder
		);
	}

	@GetMapping("/order-status")
	@PreAuthorize("hasAuthority('reports.read')")
	public List<OrderStatusReportResponse> orderStatus(
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "supplier_id", required = false) String supplierId,
			@RequestParam(value = "date_from", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
			@RequestParam(value = "date_to", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo
	) {
		return service.orderStatus(
				SecurityUtils.currentOrganizationId(),
				status,
				supplierId,
				dateFrom,
				dateTo
		);
	}

	@GetMapping("/house-visit-kpis")
	@PreAuthorize("hasAuthority('reports.read')")
	public List<HouseVisitKpiReportResponse> houseVisitKpis(
			@RequestParam(value = "clinic_id", required = false) String clinicId,
			@RequestParam(value = "provider_id", required = false) String providerId,
			@RequestParam(value = "date_from", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
			@RequestParam(value = "date_to", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo
	) {
		return service.houseVisitKpis(
				SecurityUtils.currentOrganizationId(),
				clinicId,
				providerId,
				dateFrom,
				dateTo
		);
	}
}
