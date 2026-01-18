package com.clinic.mgmt.reporting.dto;

import java.math.BigDecimal;

public class RevenueAnalysisReportResponse {

	private String clinicId;

	private String branchId;

	private String itemType;

	private BigDecimal revenue;

	private Long invoiceCount;

	public String getClinicId() {
		return clinicId;
	}

	public void setClinicId(String clinicId) {
		this.clinicId = clinicId;
	}

	public String getBranchId() {
		return branchId;
	}

	public void setBranchId(String branchId) {
		this.branchId = branchId;
	}

	public String getItemType() {
		return itemType;
	}

	public void setItemType(String itemType) {
		this.itemType = itemType;
	}

	public BigDecimal getRevenue() {
		return revenue;
	}

	public void setRevenue(BigDecimal revenue) {
		this.revenue = revenue;
	}

	public Long getInvoiceCount() {
		return invoiceCount;
	}

	public void setInvoiceCount(Long invoiceCount) {
		this.invoiceCount = invoiceCount;
	}
}
