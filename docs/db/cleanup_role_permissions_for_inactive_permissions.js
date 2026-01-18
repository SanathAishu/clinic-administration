// Remove role_permissions entries that reference inactive permissions.
// Run with: mongosh "mongodb://localhost:27017/clinic_admin" docs/db/cleanup_role_permissions_for_inactive_permissions.js

const inactivePermissionIds = db.permissions
  .find({ active: false }, { _id: 1 })
  .toArray()
  .map((doc) => doc._id);

if (!inactivePermissionIds.length) {
  print("No inactive permissions found. No role_permissions removed.");
} else {
  const result = db.role_permissions.deleteMany({ permission_id: { $in: inactivePermissionIds } });
  print(`Inactive permissions: ${inactivePermissionIds.length}`);
  print(`Removed role_permissions: ${result.deletedCount}`);
}
