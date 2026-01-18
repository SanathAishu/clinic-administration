package com.clinic.mgmt.treatment.domain;

import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document("treatment_types")
public class TreatmentType {

	@Id
	private String id;

	@Field("organization_id")
	private String organizationId;

	@Field("clinic_id")
	private String clinicId;

	private String name;

	@Field("category_lookup_type")
	private String categoryLookupType;

	@Field("category_code")
	private String categoryCode;

	@Field("duration_minutes")
	private Integer durationMinutes;

	@Field("base_rate")
	private BigDecimal baseRate;

	private Boolean active;

	@CreatedDate
	@Field("created_at")
	private Instant createdAt;

	@LastModifiedDate
	@Field("updated_at")
	private Instant updatedAt;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getOrganizationId() {
		return organizationId;
	}

	public void setOrganizationId(String organizationId) {
		this.organizationId = organizationId;
	}

	public String getClinicId() {
		return clinicId;
	}

	public void setClinicId(String clinicId) {
		this.clinicId = clinicId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCategoryLookupType() {
		return categoryLookupType;
	}

	public void setCategoryLookupType(String categoryLookupType) {
		this.categoryLookupType = categoryLookupType;
	}

	public String getCategoryCode() {
		return categoryCode;
	}

	public void setCategoryCode(String categoryCode) {
		this.categoryCode = categoryCode;
	}

	public Integer getDurationMinutes() {
		return durationMinutes;
	}

	public void setDurationMinutes(Integer durationMinutes) {
		this.durationMinutes = durationMinutes;
	}

	public BigDecimal getBaseRate() {
		return baseRate;
	}

	public void setBaseRate(BigDecimal baseRate) {
		this.baseRate = baseRate;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
