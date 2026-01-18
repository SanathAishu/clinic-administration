package com.clinic.mgmt.reporting.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.stereotype.Service;
import com.clinic.mgmt.common.exception.InvalidRequestException;
import com.clinic.mgmt.common.tenant.TenantGuard;
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

@Service
public class ReportingService {

	private static final List<String> REVENUE_STATUSES = List.of("issued", "partially_paid", "paid");
	private static final List<String> OUTSTANDING_STATUSES = List.of("issued", "partially_paid");

	private final MongoTemplate mongoTemplate;

	public ReportingService(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public List<DailySummaryReportResponse> dailySummary(
			String organizationId,
			String clinicId,
			String branchId,
			Instant dateFrom,
			Instant dateTo
	) {
		validateDateRange(dateFrom, dateTo);
		String scopedOrganizationId = TenantGuard.resolveScope(null, organizationId);
		Document revenueMatch = baseMatch(scopedOrganizationId, clinicId, branchId);
		addDateRange(revenueMatch, "entry_date", dateFrom, dateTo);
		Document expenseMatch = baseMatch(scopedOrganizationId, clinicId, branchId);
		addDateRange(expenseMatch, "expense_date", dateFrom, dateTo);

		List<AggregationOperation> ops = new ArrayList<>();
		addMatchStage(ops, revenueMatch);
		ops.add(op(new Document("$project",
				new Document("day", new Document("$dateToString",
						new Document("format", "%Y-%m-%d").append("date", "$entry_date")))
						.append("revenue", "$amount")
						.append("expense", 0))));
		List<Document> unionPipeline = new ArrayList<>();
		if (!expenseMatch.isEmpty()) {
			unionPipeline.add(new Document("$match", expenseMatch));
		}
		unionPipeline.add(new Document("$project",
				new Document("day", new Document("$dateToString",
						new Document("format", "%Y-%m-%d").append("date", "$expense_date")))
						.append("revenue", 0)
						.append("expense", "$amount")));
		ops.add(op(new Document("$unionWith",
				new Document("coll", "expenses").append("pipeline", unionPipeline))));
		ops.add(op(new Document("$group", new Document("_id", "$day")
				.append("revenueTotal", new Document("$sum", "$revenue"))
				.append("expenseTotal", new Document("$sum", "$expense")))));
		ops.add(op(new Document("$addFields",
				new Document("profitLoss",
						new Document("$subtract", List.of("$revenueTotal", "$expenseTotal"))))));
		ops.add(op(new Document("$sort", new Document("_id", 1))));
		ops.add(op(new Document("$project", new Document("_id", 0)
				.append("day", "$_id")
				.append("revenueTotal", 1)
				.append("expenseTotal", 1)
				.append("profitLoss", 1))));
		Aggregation aggregation = Aggregation.newAggregation(ops);
		return mongoTemplate.aggregate(
				aggregation,
				"revenue_ledger",
				DailySummaryReportResponse.class
		).getMappedResults();
	}

	public List<RevenueAnalysisReportResponse> revenueAnalysis(
			String organizationId,
			String clinicId,
			String branchId,
			Instant dateFrom,
			Instant dateTo,
			String groupBy
	) {
		validateDateRange(dateFrom, dateTo);
		String normalizedGroupBy = normalizeGroupBy(groupBy);
		String scopedOrganizationId = TenantGuard.resolveScope(null, organizationId);
		Document match = baseMatch(scopedOrganizationId, clinicId, branchId);
		match.append("status", new Document("$in", REVENUE_STATUSES));
		addDateRange(match, "issue_date", dateFrom, dateTo);

		Document groupId = new Document();
		if ("clinic".equals(normalizedGroupBy)) {
			groupId.append("clinicId", "$clinic_id");
			groupId.append("itemType", "$items.item_type");
		} else if ("branch".equals(normalizedGroupBy)) {
			groupId.append("clinicId", "$clinic_id");
			groupId.append("branchId", "$branch_id");
			groupId.append("itemType", "$items.item_type");
		} else {
			groupId.append("itemType", "$items.item_type");
		}

		List<AggregationOperation> ops = new ArrayList<>();
		addMatchStage(ops, match);
		ops.add(op(new Document("$unwind", "$items")));
		ops.add(op(new Document("$group", new Document("_id", groupId)
				.append("revenue", new Document("$sum", "$items.amount"))
				.append("invoiceIds", new Document("$addToSet", "$_id")))));
		ops.add(op(new Document("$project", new Document("_id", 0)
				.append("clinicId", "$_id.clinicId")
				.append("branchId", "$_id.branchId")
				.append("itemType", "$_id.itemType")
				.append("revenue", 1)
				.append("invoiceCount", new Document("$size", "$invoiceIds")))));
		ops.add(op(new Document("$sort", new Document("revenue", -1))));
		Aggregation aggregation = Aggregation.newAggregation(ops);
		return mongoTemplate.aggregate(
				aggregation,
				"invoices",
				RevenueAnalysisReportResponse.class
		).getMappedResults();
	}

	public List<PaymentMixReportResponse> paymentMix(
			String organizationId,
			String clinicId,
			String branchId,
			Instant dateFrom,
			Instant dateTo
	) {
		validateDateRange(dateFrom, dateTo);
		String scopedOrganizationId = TenantGuard.resolveScope(null, organizationId);
		Document match = baseMatch(scopedOrganizationId, clinicId, branchId);

		List<AggregationOperation> ops = new ArrayList<>();
		addMatchStage(ops, match);
		ops.add(op(new Document("$unwind", "$payments")));
		Document paymentMatch = new Document("payments.status", "received");
		addDateRange(paymentMatch, "payments.paid_at", dateFrom, dateTo);
		ops.add(op(new Document("$match", paymentMatch)));
		ops.add(op(new Document("$group", new Document("_id", "$payments.method_code")
				.append("amountTotal", new Document("$sum", "$payments.amount"))
				.append("paymentCount", new Document("$sum", 1)))));
		ops.add(op(new Document("$project", new Document("_id", 0)
				.append("methodCode", "$_id")
				.append("amountTotal", 1)
				.append("paymentCount", 1))));
		ops.add(op(new Document("$sort", new Document("amountTotal", -1))));
		Aggregation aggregation = Aggregation.newAggregation(ops);
		return mongoTemplate.aggregate(
				aggregation,
				"invoices",
				PaymentMixReportResponse.class
		).getMappedResults();
	}

	public List<BillingOutstandingReportResponse> billingOutstanding(
			String organizationId,
			String clinicId,
			String branchId,
			Instant asOf
	) {
		String scopedOrganizationId = TenantGuard.resolveScope(null, organizationId);
		Document match = baseMatch(scopedOrganizationId, clinicId, branchId);
		match.append("status", new Document("$in", OUTSTANDING_STATUSES));
		match.append("balance", new Document("$gt", 0));
		if (asOf != null) {
			match.append("issue_date", new Document("$lte", asOf));
		}

		Document groupId = new Document("clinicId", "$clinic_id")
				.append("branchId", "$branch_id")
				.append("patientId", "$patient_id");

		List<AggregationOperation> ops = new ArrayList<>();
		addMatchStage(ops, match);
		ops.add(op(new Document("$group", new Document("_id", groupId)
				.append("balanceTotal", new Document("$sum", "$balance"))
				.append("invoiceCount", new Document("$sum", 1)))));
		ops.add(op(new Document("$project", new Document("_id", 0)
				.append("clinicId", "$_id.clinicId")
				.append("branchId", "$_id.branchId")
				.append("patientId", "$_id.patientId")
				.append("balanceTotal", 1)
				.append("invoiceCount", 1))));
		ops.add(op(new Document("$sort", new Document("balanceTotal", -1))));
		Aggregation aggregation = Aggregation.newAggregation(ops);
		return mongoTemplate.aggregate(
				aggregation,
				"invoices",
				BillingOutstandingReportResponse.class
		).getMappedResults();
	}

	public List<StaffPerformanceReportResponse> staffPerformance(
			String organizationId,
			String clinicId,
			String staffType,
			Instant dateFrom,
			Instant dateTo
	) {
		validateDateRange(dateFrom, dateTo);
		String scopedOrganizationId = TenantGuard.resolveScope(null, organizationId);
		Document match = baseMatch(scopedOrganizationId, clinicId, null);
		addDateRange(match, "scheduled_start", dateFrom, dateTo);

		List<AggregationOperation> ops = new ArrayList<>();
		addMatchStage(ops, match);
		ops.add(op(new Document("$group", new Document("_id", "$provider_id")
				.append("scheduled", sumStatus("scheduled"))
				.append("inProgress", sumStatus("in_progress"))
				.append("completed", sumStatus("completed"))
				.append("cancelled", sumStatus("cancelled"))
				.append("noShow", sumStatus("no_show")))));
		ops.add(op(new Document("$lookup", new Document("from", "staff")
				.append("localField", "_id")
				.append("foreignField", "_id")
				.append("as", "staff"))));
		ops.add(op(new Document("$unwind", new Document("path", "$staff")
				.append("preserveNullAndEmptyArrays", true))));
		if (isPresent(staffType)) {
			ops.add(op(new Document("$match", new Document("staff.staff_type", staffType))));
		}
		ops.add(op(new Document("$lookup", new Document("from", "users")
				.append("localField", "staff.user_id")
				.append("foreignField", "_id")
				.append("as", "user"))));
		ops.add(op(new Document("$unwind", new Document("path", "$user")
				.append("preserveNullAndEmptyArrays", true))));
		ops.add(op(new Document("$project", new Document("_id", 0)
				.append("providerId", "$_id")
				.append("staffName", "$user.full_name")
				.append("scheduled", 1)
				.append("inProgress", 1)
				.append("completed", 1)
				.append("cancelled", 1)
				.append("noShow", 1))));
		ops.add(op(new Document("$sort", new Document("completed", -1))));
		Aggregation aggregation = Aggregation.newAggregation(ops);
		return mongoTemplate.aggregate(
				aggregation,
				"appointments",
				StaffPerformanceReportResponse.class
		).getMappedResults();
	}

	public List<PatientActivityReportResponse> patientActivity(
			String organizationId,
			String clinicId,
			String branchId,
			Instant dateFrom,
			Instant dateTo
	) {
		validateDateRange(dateFrom, dateTo);
		String scopedOrganizationId = TenantGuard.resolveScope(null, organizationId);
		Document match = baseMatch(scopedOrganizationId, clinicId, branchId);
		addDateRange(match, "created_at", dateFrom, dateTo);

		List<AggregationOperation> ops = new ArrayList<>();
		addMatchStage(ops, match);
		ops.add(op(new Document("$project",
				new Document("day", new Document("$dateToString",
						new Document("format", "%Y-%m-%d").append("date", "$created_at"))))));
		ops.add(op(new Document("$group", new Document("_id", "$day")
				.append("newPatients", new Document("$sum", 1)))));
		ops.add(op(new Document("$sort", new Document("_id", 1))));
		ops.add(op(new Document("$project", new Document("_id", 0)
				.append("day", "$_id")
				.append("newPatients", 1))));
		Aggregation aggregation = Aggregation.newAggregation(ops);
		return mongoTemplate.aggregate(
				aggregation,
				"patients",
				PatientActivityReportResponse.class
		).getMappedResults();
	}

	public List<AppointmentAnalyticsReportResponse> appointmentAnalytics(
			String organizationId,
			String clinicId,
			String providerId,
			Instant dateFrom,
			Instant dateTo
	) {
		validateDateRange(dateFrom, dateTo);
		String scopedOrganizationId = TenantGuard.resolveScope(null, organizationId);
		Document match = baseMatch(scopedOrganizationId, clinicId, null);
		if (isPresent(providerId)) {
			match.append("provider_id", providerId);
		}
		addDateRange(match, "scheduled_start", dateFrom, dateTo);

		List<AggregationOperation> ops = new ArrayList<>();
		addMatchStage(ops, match);
		ops.add(op(new Document("$project", new Document("status", 1)
				.append("day", new Document("$dateToString",
						new Document("format", "%Y-%m-%d").append("date", "$scheduled_start"))))));
		ops.add(op(new Document("$group", new Document("_id", new Document("day", "$day")
				.append("status", "$status"))
				.append("count", new Document("$sum", 1)))));
		ops.add(op(new Document("$group", new Document("_id", "$_id.day")
				.append("total", new Document("$sum", "$count"))
				.append("counts", new Document("$push",
						new Document("status", "$_id.status").append("count", "$count"))))));
		ops.add(op(new Document("$sort", new Document("_id", 1))));
		ops.add(op(new Document("$project", new Document("_id", 0)
				.append("day", "$_id")
				.append("total", 1)
				.append("counts", 1))));
		Aggregation aggregation = Aggregation.newAggregation(ops);
		return mongoTemplate.aggregate(
				aggregation,
				"appointments",
				AppointmentAnalyticsReportResponse.class
		).getMappedResults();
	}

	public List<TreatmentOutcomesReportResponse> treatmentOutcomes(
			String organizationId,
			String clinicId,
			String treatmentTypeId,
			Instant dateFrom,
			Instant dateTo
	) {
		validateDateRange(dateFrom, dateTo);
		String scopedOrganizationId = TenantGuard.resolveScope(null, organizationId);
		Document match = baseMatch(scopedOrganizationId, clinicId, null);
		if (isPresent(treatmentTypeId)) {
			match.append("treatment_type_id", treatmentTypeId);
		}
		addDateRange(match, "created_at", dateFrom, dateTo);

		List<AggregationOperation> ops = new ArrayList<>();
		addMatchStage(ops, match);
		ops.add(op(new Document("$project", new Document("status", 1)
				.append("treatmentTypeId", "$treatment_type_id"))));
		ops.add(op(new Document("$group", new Document("_id", new Document("treatmentTypeId", "$treatmentTypeId")
				.append("status", "$status"))
				.append("count", new Document("$sum", 1)))));
		ops.add(op(new Document("$group", new Document("_id", "$_id.treatmentTypeId")
				.append("total", new Document("$sum", "$count"))
				.append("counts", new Document("$push",
						new Document("status", "$_id.status").append("count", "$count"))))));
		ops.add(op(new Document("$sort", new Document("total", -1))));
		ops.add(op(new Document("$project", new Document("_id", 0)
				.append("treatmentTypeId", "$_id")
				.append("total", 1)
				.append("counts", 1))));
		Aggregation aggregation = Aggregation.newAggregation(ops);
		return mongoTemplate.aggregate(
				aggregation,
				"treatments",
				TreatmentOutcomesReportResponse.class
		).getMappedResults();
	}

	public List<InventoryLevelReportResponse> inventoryLevels(
			String organizationId,
			String clinicId,
			String branchId,
			Boolean belowReorder
	) {
		String scopedOrganizationId = TenantGuard.resolveScope(null, organizationId);
		Document match = baseMatch(scopedOrganizationId, clinicId, branchId);

		List<AggregationOperation> ops = new ArrayList<>();
		addMatchStage(ops, match);
		ops.add(op(new Document("$addFields",
				new Document("belowReorder",
						new Document("$lt", List.of("$quantity_on_hand", "$reorder_level"))))));
		if (belowReorder != null) {
			ops.add(op(new Document("$match", new Document("belowReorder", belowReorder))));
		}
		ops.add(op(new Document("$project", new Document("_id", 0)
				.append("inventoryItemId", "$_id")
				.append("name", 1)
				.append("quantityOnHand", "$quantity_on_hand")
				.append("reorderLevel", "$reorder_level")
				.append("belowReorder", 1))));
		ops.add(op(new Document("$sort", new Document("name", 1))));
		Aggregation aggregation = Aggregation.newAggregation(ops);
		return mongoTemplate.aggregate(
				aggregation,
				"inventory_items",
				InventoryLevelReportResponse.class
		).getMappedResults();
	}

	public List<OrderStatusReportResponse> orderStatus(
			String organizationId,
			String status,
			String supplierId,
			Instant dateFrom,
			Instant dateTo
	) {
		validateDateRange(dateFrom, dateTo);
		String scopedOrganizationId = TenantGuard.resolveScope(null, organizationId);
		Document match = baseMatch(scopedOrganizationId, null, null);
		if (isPresent(status)) {
			match.append("status", status);
		}
		if (isPresent(supplierId)) {
			match.append("supplier_id", supplierId);
		}
		addDateRange(match, "ordered_at", dateFrom, dateTo);

		List<AggregationOperation> ops = new ArrayList<>();
		addMatchStage(ops, match);
		ops.add(op(new Document("$group", new Document("_id", "$status")
				.append("orderCount", new Document("$sum", 1)))));
		ops.add(op(new Document("$project", new Document("_id", 0)
				.append("status", "$_id")
				.append("orderCount", 1))));
		ops.add(op(new Document("$sort", new Document("orderCount", -1))));
		Aggregation aggregation = Aggregation.newAggregation(ops);
		return mongoTemplate.aggregate(
				aggregation,
				"orders",
				OrderStatusReportResponse.class
		).getMappedResults();
	}

	public List<HouseVisitKpiReportResponse> houseVisitKpis(
			String organizationId,
			String clinicId,
			String providerId,
			Instant dateFrom,
			Instant dateTo
	) {
		validateDateRange(dateFrom, dateTo);
		String scopedOrganizationId = TenantGuard.resolveScope(null, organizationId);
		Document match = baseMatch(scopedOrganizationId, clinicId, null);
		if (isPresent(providerId)) {
			match.append("provider_id", providerId);
		}
		addDateRange(match, "scheduled_at", dateFrom, dateTo);

		List<AggregationOperation> ops = new ArrayList<>();
		addMatchStage(ops, match);
		ops.add(op(new Document("$group", new Document("_id", "$provider_id")
				.append("scheduled", sumStatus("scheduled"))
				.append("enRoute", sumStatus("en_route"))
				.append("completed", sumStatus("completed"))
				.append("cancelled", sumStatus("cancelled")))));
		ops.add(op(new Document("$addFields", new Document("total",
				new Document("$add", List.of("$scheduled", "$enRoute", "$completed", "$cancelled"))))));
		ops.add(op(new Document("$project", new Document("_id", 0)
				.append("providerId", "$_id")
				.append("scheduled", 1)
				.append("enRoute", 1)
				.append("completed", 1)
				.append("cancelled", 1)
				.append("total", 1))));
		ops.add(op(new Document("$sort", new Document("total", -1))));
		Aggregation aggregation = Aggregation.newAggregation(ops);
		return mongoTemplate.aggregate(
				aggregation,
				"house_visits",
				HouseVisitKpiReportResponse.class
		).getMappedResults();
	}

	private AggregationOperation op(Document operation) {
		return context -> context.getMappedObject(operation);
	}

	private void addMatchStage(List<AggregationOperation> ops, Document match) {
		if (!match.isEmpty()) {
			ops.add(op(new Document("$match", match)));
		}
	}

	private Document baseMatch(String scopedOrganizationId, String clinicId, String branchId) {
		Document match = new Document();
		if (isPresent(scopedOrganizationId)) {
			match.append("organization_id", scopedOrganizationId);
		}
		if (isPresent(clinicId)) {
			match.append("clinic_id", clinicId);
		}
		if (isPresent(branchId)) {
			match.append("branch_id", branchId);
		}
		return match;
	}

	private void addDateRange(Document match, String field, Instant dateFrom, Instant dateTo) {
		if (dateFrom == null && dateTo == null) {
			return;
		}
		Document range = new Document();
		if (dateFrom != null) {
			range.append("$gte", dateFrom);
		}
		if (dateTo != null) {
			range.append("$lte", dateTo);
		}
		match.append(field, range);
	}

	private boolean isPresent(String value) {
		return value != null && !value.isBlank();
	}

	private void validateDateRange(Instant dateFrom, Instant dateTo) {
		if (dateFrom != null && dateTo != null && dateTo.isBefore(dateFrom)) {
			throw new InvalidRequestException("date_to must be after date_from");
		}
	}

	private String normalizeGroupBy(String groupBy) {
		if (groupBy == null || groupBy.isBlank()) {
			return "clinic";
		}
		String normalized = groupBy.trim().toLowerCase(Locale.ROOT);
		if ("clinic".equals(normalized) || "branch".equals(normalized) || "item_type".equals(normalized)) {
			return normalized;
		}
		throw new InvalidRequestException("group_by must be one of clinic, branch, item_type");
	}

	private Document sumStatus(String status) {
		return new Document("$sum",
				new Document("$cond", List.of(new Document("$eq", List.of("$status", status)), 1, 0)));
	}
}
