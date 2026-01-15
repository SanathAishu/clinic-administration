package com.clinic.backend.mapper;

import com.clinic.common.dto.request.CreatePermissionRequest;
import com.clinic.common.dto.response.PermissionResponseDTO;
import com.clinic.common.entity.core.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PermissionMapper {

    PermissionResponseDTO toResponseDTO(Permission permission);

    List<PermissionResponseDTO> toResponseDTOList(List<Permission> permissions);

    @Mapping(target = "roles", ignore = true) // Managed by service layer
    Permission toEntity(CreatePermissionRequest request);
}
