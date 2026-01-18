package com.clinic.mgmt.reporting.dto;

public class HouseVisitKpiReportResponse {

	private String providerId;

	private Long scheduled;

	private Long enRoute;

	private Long completed;

	private Long cancelled;

	private Long total;

	public String getProviderId() {
		return providerId;
	}

	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}

	public Long getScheduled() {
		return scheduled;
	}

	public void setScheduled(Long scheduled) {
		this.scheduled = scheduled;
	}

	public Long getEnRoute() {
		return enRoute;
	}

	public void setEnRoute(Long enRoute) {
		this.enRoute = enRoute;
	}

	public Long getCompleted() {
		return completed;
	}

	public void setCompleted(Long completed) {
		this.completed = completed;
	}

	public Long getCancelled() {
		return cancelled;
	}

	public void setCancelled(Long cancelled) {
		this.cancelled = cancelled;
	}

	public Long getTotal() {
		return total;
	}

	public void setTotal(Long total) {
		this.total = total;
	}
}
