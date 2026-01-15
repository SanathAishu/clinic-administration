package com.clinic.backend.mapper;

import com.clinic.common.dto.response.AuditLogResponseDTO;
import com.clinic.common.entity.core.AuditLog;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    AuditLogResponseDTO toResponseDTO(AuditLog auditLog);

    List<AuditLogResponseDTO> toResponseDTOList(List<AuditLog> auditLogs);

    // No toEntity mapping - AuditLog is created internally, not from API requests
}
