// Cleanup role_permissions for inactive and missing permissions.
// Run with: mongosh "mongodb://mongo:mongo@localhost:27017/clinic_admin?authSource=admin" docs/db/cleanup_role_permissions.js

const inactivePermissionIds = db.permissions
  .find({ active: false }, { _id: 1 })
  .toArray()
  .map((doc) => doc._id);

if (!inactivePermissionIds.length) {
  print("No inactive permissions found. No role_permissions removed for inactive permissions.");
} else {
  const inactiveResult = db.role_permissions.deleteMany({
    permission_id: { $in: inactivePermissionIds }
  });
  print(`Inactive permissions: ${inactivePermissionIds.length}`);
  print(`Removed role_permissions (inactive permissions): ${inactiveResult.deletedCount}`);
}

const permissionIds = db.permissions.distinct("_id");

if (!permissionIds.length) {
  print("No permissions found. Skipping missing-permission cleanup to avoid deleting all role_permissions.");
} else {
  const missingResult = db.role_permissions.deleteMany({
    permission_id: { $nin: permissionIds }
  });
  print(`Known permissions: ${permissionIds.length}`);
  print(`Removed role_permissions (missing permissions): ${missingResult.deletedCount}`);
}
