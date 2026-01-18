package com.clinic.mgmt.reporting.dto;

public class OrderStatusReportResponse {

	private String status;

	private Long orderCount;

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Long getOrderCount() {
		return orderCount;
	}

	public void setOrderCount(Long orderCount) {
		this.orderCount = orderCount;
	}
}
