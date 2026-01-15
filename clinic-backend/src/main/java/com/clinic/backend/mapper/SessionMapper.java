package com.clinic.backend.mapper;

import com.clinic.common.dto.response.SessionResponseDTO;
import com.clinic.common.entity.core.Session;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SessionMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "issuedAt", source = "createdAt")
    @Mapping(target = "isActive", expression = "java(session.isValid())")
    SessionResponseDTO toResponseDTO(Session session);

    List<SessionResponseDTO> toResponseDTOList(List<Session> sessions);

    // No toEntity mapping - Session is created internally during authentication
}
