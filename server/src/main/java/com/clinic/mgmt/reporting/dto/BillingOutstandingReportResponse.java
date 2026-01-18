package com.clinic.mgmt.reporting.dto;

import java.math.BigDecimal;

public class BillingOutstandingReportResponse {

	private String clinicId;

	private String branchId;

	private String patientId;

	private BigDecimal balanceTotal;

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

	public String getPatientId() {
		return patientId;
	}

	public void setPatientId(String patientId) {
		this.patientId = patientId;
	}

	public BigDecimal getBalanceTotal() {
		return balanceTotal;
	}

	public void setBalanceTotal(BigDecimal balanceTotal) {
		this.balanceTotal = balanceTotal;
	}

	public Long getInvoiceCount() {
		return invoiceCount;
	}

	public void setInvoiceCount(Long invoiceCount) {
		this.invoiceCount = invoiceCount;
	}
}
