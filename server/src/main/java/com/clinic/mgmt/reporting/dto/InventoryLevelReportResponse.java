package com.clinic.mgmt.reporting.dto;

import java.math.BigDecimal;

public class InventoryLevelReportResponse {

	private String inventoryItemId;

	private String name;

	private BigDecimal quantityOnHand;

	private BigDecimal reorderLevel;

	private Boolean belowReorder;

	public String getInventoryItemId() {
		return inventoryItemId;
	}

	public void setInventoryItemId(String inventoryItemId) {
		this.inventoryItemId = inventoryItemId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public BigDecimal getQuantityOnHand() {
		return quantityOnHand;
	}

	public void setQuantityOnHand(BigDecimal quantityOnHand) {
		this.quantityOnHand = quantityOnHand;
	}

	public BigDecimal getReorderLevel() {
		return reorderLevel;
	}

	public void setReorderLevel(BigDecimal reorderLevel) {
		this.reorderLevel = reorderLevel;
	}

	public Boolean getBelowReorder() {
		return belowReorder;
	}

	public void setBelowReorder(Boolean belowReorder) {
		this.belowReorder = belowReorder;
	}
}
