package com.clinic.mgmt.reporting.dto;

import java.util.List;

public class TreatmentOutcomesReportResponse {

	private String treatmentTypeId;

	private Long total;

	private List<ReportStatusCount> counts;

	public String getTreatmentTypeId() {
		return treatmentTypeId;
	}

	public void setTreatmentTypeId(String treatmentTypeId) {
		this.treatmentTypeId = treatmentTypeId;
	}

	public Long getTotal() {
		return total;
	}

	public void setTotal(Long total) {
		this.total = total;
	}

	public List<ReportStatusCount> getCounts() {
		return counts;
	}

	public void setCounts(List<ReportStatusCount> counts) {
		this.counts = counts;
	}
}
