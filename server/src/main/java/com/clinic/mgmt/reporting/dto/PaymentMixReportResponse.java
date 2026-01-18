package com.clinic.mgmt.reporting.dto;

import java.math.BigDecimal;

public class PaymentMixReportResponse {

	private String methodCode;

	private BigDecimal amountTotal;

	private Long paymentCount;

	public String getMethodCode() {
		return methodCode;
	}

	public void setMethodCode(String methodCode) {
		this.methodCode = methodCode;
	}

	public BigDecimal getAmountTotal() {
		return amountTotal;
	}

	public void setAmountTotal(BigDecimal amountTotal) {
		this.amountTotal = amountTotal;
	}

	public Long getPaymentCount() {
		return paymentCount;
	}

	public void setPaymentCount(Long paymentCount) {
		this.paymentCount = paymentCount;
	}
}
