package com.clinic.backend.mapper;

import com.clinic.common.dto.request.CreateTenantRequest;
import com.clinic.common.dto.request.UpdateTenantRequest;
import com.clinic.common.dto.response.TenantResponseDTO;
import com.clinic.common.entity.core.Tenant;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TenantMapper {

    TenantResponseDTO toResponseDTO(Tenant tenant);

    List<TenantResponseDTO> toResponseDTOList(List<Tenant> tenants);

    @Mapping(target = "city", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "pincode", ignore = true)
    @Mapping(target = "gstin", ignore = true)
    @Mapping(target = "clinicRegistrationNumber", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Tenant toEntity(CreateTenantRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "subdomain", ignore = true) // Cannot change subdomain after creation
    @Mapping(target = "city", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "pincode", ignore = true)
    @Mapping(target = "gstin", ignore = true)
    @Mapping(target = "clinicRegistrationNumber", ignore = true)
    @Mapping(target = "status", ignore = true) // Status changed via service methods
    @Mapping(target = "subscriptionStartDate", ignore = true) // Managed by service
    @Mapping(target = "subscriptionEndDate", ignore = true) // Managed by service
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntityFromRequest(UpdateTenantRequest request, @MappingTarget Tenant tenant);
}
