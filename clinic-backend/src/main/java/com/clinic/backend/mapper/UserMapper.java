package com.clinic.backend.mapper;

import com.clinic.common.dto.request.CreateUserRequest;
import com.clinic.common.dto.response.UserResponseDTO;
import com.clinic.common.entity.core.User;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = {RoleMapper.class})
public interface UserMapper {

    @Mapping(target = "fullName", expression = "java(user.getFullName())")
    @Mapping(target = "isLocked", expression = "java(user.isLocked())")
    UserResponseDTO toResponseDTO(User user);

    List<UserResponseDTO> toResponseDTOList(List<User> users);

    @Mapping(target = "passwordHash", ignore = true) // Will be set by service after encoding
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "tenant", ignore = true) // Set by service from tenant context
    @Mapping(target = "loginAttempts", ignore = true)
    @Mapping(target = "lockedUntil", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "passwordChangedAt", ignore = true)
    User toEntity(CreateUserRequest request);
}
