package com.clinic.backend.mapper;

import com.clinic.common.dto.request.CreateRoleRequest;
import com.clinic.common.dto.request.UpdateRoleRequest;
import com.clinic.common.dto.response.RoleResponseDTO;
import com.clinic.common.entity.core.Role;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = {PermissionMapper.class})
public interface RoleMapper {

    RoleResponseDTO toResponseDTO(Role role);

    List<RoleResponseDTO> toResponseDTOList(List<Role> roles);

    @Mapping(target = "permissions", ignore = true) // Will be set by service
    @Mapping(target = "isSystemRole", ignore = true)
    @Mapping(target = "users", ignore = true)
    Role toEntity(CreateRoleRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "permissions", ignore = true) // Will be updated by service
    @Mapping(target = "isSystemRole", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "tenant", ignore = true)
    @Mapping(target = "users", ignore = true)
    void updateEntityFromRequest(UpdateRoleRequest request, @MappingTarget Role role);
}
