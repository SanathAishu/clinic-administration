package com.clinic.mgmt.identity.repository;

import java.util.List;
import com.clinic.mgmt.identity.domain.RolePermission;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RolePermissionRepository extends MongoRepository<RolePermission, String> {

	List<RolePermission> findByRoleId(String roleId);

	List<RolePermission> findByRoleIdIn(List<String> roleIds);

	boolean existsByRoleId(String roleId);

	boolean existsByPermissionId(String permissionId);

	void deleteByRoleId(String roleId);

	void deleteByRoleIdAndPermissionId(String roleId, String permissionId);

	void deleteByPermissionId(String permissionId);
}
