package com.clinic.backend.mapper;

import com.clinic.backend.dto.branch.CreateBranchRequest;
import com.clinic.backend.dto.branch.BranchDTO;
import com.clinic.backend.dto.branch.UpdateBranchRequest;
import com.clinic.common.entity.core.Branch;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface BranchMapper {

    Branch toEntity(CreateBranchRequest request);

    BranchDTO toDTO(Branch branch);

    List<BranchDTO> toDTOList(List<Branch> branches);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntityFromRequest(UpdateBranchRequest request, @MappingTarget Branch branch);
}
