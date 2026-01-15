package com.clinic.backend.security;

import com.clinic.backend.repository.UserRepository;
import com.clinic.common.entity.core.Permission;
import com.clinic.common.entity.core.Role;
import com.clinic.common.entity.core.User;
import com.clinic.common.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Custom UserDetailsService implementation for loading users from database.
 * Supports loading by user ID (for JWT authentication) or by email (for login).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Load user by username (user ID as string for JWT authentication)
     * This is called by the JWT filter after extracting user ID from token
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            UUID userId = UUID.fromString(username);
            return loadUserById(userId);
        } catch (IllegalArgumentException e) {
            // Not a UUID, try loading by email (requires tenant context)
            UUID tenantId = TenantContext.getCurrentTenant();
            if (tenantId == null) {
                throw new UsernameNotFoundException("User not found and no tenant context available");
            }
            return loadUserByEmailAndTenant(username, tenantId);
        }
    }

    /**
     * Load user by ID (Bijective lookup: userId -> User)
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(UUID userId) throws UsernameNotFoundException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));

        return buildUserPrincipal(user);
    }

    /**
     * Load user by email and tenant (Injective function within tenant scope)
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserByEmailAndTenant(String email, UUID tenantId) throws UsernameNotFoundException {
        User user = userRepository.findByEmailAndTenantIdForAuthentication(email, tenantId)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + email + " in tenant: " + tenantId));

        return buildUserPrincipal(user);
    }

    /**
     * Build UserPrincipal from User entity
     * Extracts roles and permissions for authorization
     */
    private UserPrincipal buildUserPrincipal(User user) {
        // Extract role names
        Set<Role> roles = user.getRoles();
        List<String> roleNames = roles.stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        // Extract permission names from all roles (Set union - no duplicates)
        List<String> permissionNames = roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .distinct()
                .collect(Collectors.toList());

        return UserPrincipal.builder()
                .id(user.getId())
                .tenantId(user.getTenantId())
                .email(user.getEmail())
                .password(user.getPasswordHash())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .status(user.getStatus().name())
                .roles(roleNames)
                .permissions(permissionNames)
                .build();
    }
}
