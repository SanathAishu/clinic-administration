// Backfill missing permission scope values.
// Run with: mongosh "mongodb://mongo:mongo@localhost:27017/clinic_admin?authSource=admin" docs/db/backfill_permission_scope.js

const now = new Date();

const missingScope = { $or: [{ scope: { $exists: false } }, { scope: null }] };

const systemResult = db.permissions.updateMany(
  { ...missingScope, resource: "system" },
  { $set: { scope: "system", updated_at: now } }
);

const tenantResult = db.permissions.updateMany(
  { ...missingScope, resource: { $ne: "system" } },
  { $set: { scope: "tenant", updated_at: now } }
);

print(`Backfilled system scope: ${systemResult.modifiedCount}`);
print(`Backfilled tenant scope: ${tenantResult.modifiedCount}`);
