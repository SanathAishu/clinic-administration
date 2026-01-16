package com.clinic.backend.mapper;

import com.clinic.common.dto.request.CreatePatientRequest;
import com.clinic.common.dto.request.UpdatePatientRequest;
import com.clinic.common.dto.response.PatientResponseDTO;
import com.clinic.common.entity.patient.Patient;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface PatientMapper {

    @Mapping(target = "fullName", expression = "java(patient.getFullName())")
    @Mapping(target = "age", expression = "java(patient.getAge())")
    @Mapping(target = "createdById", source = "createdBy.id")
    PatientResponseDTO toResponseDTO(Patient patient);

    List<PatientResponseDTO> toResponseDTOList(List<Patient> patients);

    @Mapping(target = "createdBy", ignore = true) // Will be set by service
    @Mapping(target = "primaryBranch", ignore = true) // Will be set by service from branchId
    Patient toEntity(CreatePatientRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "primaryBranch", ignore = true) // Will be set by service if branchId is provided
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntityFromRequest(UpdatePatientRequest request, @MappingTarget Patient patient);
}
