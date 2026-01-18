package com.clinic.mgmt.reporting.dto;

import java.util.List;

public class AppointmentAnalyticsReportResponse {

	private String day;

	private Long total;

	private List<ReportStatusCount> counts;

	public String getDay() {
		return day;
	}

	public void setDay(String day) {
		this.day = day;
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
