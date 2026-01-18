package com.clinic.mgmt.lookup.domain;

import java.util.Map;
import com.clinic.mgmt.common.domain.BaseEntity;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document("lookup_values")
@CompoundIndex(
		name = "uk_lookup_values_type_code",
		def = "{'lookup_type': 1, 'code': 1}",
		unique = true
)
public class LookupValue extends BaseEntity {

	@Field("lookup_type")
	private String lookupType;

	private String code;

	private String label;

	@Field("sort_order")
	private Long sortOrder;

	private Boolean active;

	private Map<String, Object> meta;

	public String getLookupType() {
		return lookupType;
	}

	public void setLookupType(String lookupType) {
		this.lookupType = lookupType;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public Long getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(Long sortOrder) {
		this.sortOrder = sortOrder;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public Map<String, Object> getMeta() {
		return meta;
	}

	public void setMeta(Map<String, Object> meta) {
		this.meta = meta;
	}

}
