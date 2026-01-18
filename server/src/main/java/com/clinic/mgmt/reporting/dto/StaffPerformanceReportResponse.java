package com.clinic.mgmt.reporting.dto;

public class StaffPerformanceReportResponse {

	private String providerId;

	private String staffName;

	private Long scheduled;

	private Long inProgress;

	private Long completed;

	private Long cancelled;

	private Long noShow;

	public String getProviderId() {
		return providerId;
	}

	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}

	public String getStaffName() {
		return staffName;
	}

	public void setStaffName(String staffName) {
		this.staffName = staffName;
	}

	public Long getScheduled() {
		return scheduled;
	}

	public void setScheduled(Long scheduled) {
		this.scheduled = scheduled;
	}

	public Long getInProgress() {
		return inProgress;
	}

	public void setInProgress(Long inProgress) {
		this.inProgress = inProgress;
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

	public Long getNoShow() {
		return noShow;
	}

	public void setNoShow(Long noShow) {
		this.noShow = noShow;
	}
}
