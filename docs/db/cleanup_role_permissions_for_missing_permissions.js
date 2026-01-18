// Remove role_permissions entries that reference missing permissions.
// Run with: mongosh "mongodb://mongo:mongo@localhost:27017/clinic_admin?authSource=admin" docs/db/cleanup_role_permissions_for_missing_permissions.js

const permissionIds = db.permissions.distinct("_id");

if (!permissionIds.length) {
  print("No permissions found. Skipping cleanup to avoid deleting all role_permissions.");
} else {
  const result = db.role_permissions.deleteMany({ permission_id: { $nin: permissionIds } });
  print(`Known permissions: ${permissionIds.length}`);
  print(`Removed role_permissions: ${result.deletedCount}`);
}
