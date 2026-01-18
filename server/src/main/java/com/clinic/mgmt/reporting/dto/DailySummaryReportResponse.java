package com.clinic.mgmt.reporting.dto;

import java.math.BigDecimal;

public class DailySummaryReportResponse {

	private String day;

	private BigDecimal revenueTotal;

	private BigDecimal expenseTotal;

	private BigDecimal profitLoss;

	public String getDay() {
		return day;
	}

	public void setDay(String day) {
		this.day = day;
	}

	public BigDecimal getRevenueTotal() {
		return revenueTotal;
	}

	public void setRevenueTotal(BigDecimal revenueTotal) {
		this.revenueTotal = revenueTotal;
	}

	public BigDecimal getExpenseTotal() {
		return expenseTotal;
	}

	public void setExpenseTotal(BigDecimal expenseTotal) {
		this.expenseTotal = expenseTotal;
	}

	public BigDecimal getProfitLoss() {
		return profitLoss;
	}

	public void setProfitLoss(BigDecimal profitLoss) {
		this.profitLoss = profitLoss;
	}
}
