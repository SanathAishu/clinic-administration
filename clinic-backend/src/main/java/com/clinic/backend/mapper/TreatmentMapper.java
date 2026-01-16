package com.clinic.backend.mapper;

import com.clinic.backend.dto.treatment.CreateTreatmentRequest;
import com.clinic.backend.dto.treatment.TreatmentDTO;
import com.clinic.backend.dto.treatment.UpdateTreatmentRequest;
import com.clinic.common.entity.operational.Treatment;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface TreatmentMapper {

    Treatment toEntity(CreateTreatmentRequest request);

    TreatmentDTO toDTO(Treatment treatment);

    List<TreatmentDTO> toDTOList(List<Treatment> treatments);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntityFromRequest(UpdateTreatmentRequest request, @MappingTarget Treatment treatment);
}
