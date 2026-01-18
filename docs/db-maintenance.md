# Database Maintenance Runbook

Scope: MongoDB maintenance scripts under `docs/db/`. Use these on staging first and take a backup
before running in production.

## Prereqs
- MongoDB container running via `docker-compose.yml`.
- Credentials: `mongo` / `mongo`.
- Target DB: `clinic_admin`.

## Safety Checklist
- Confirm recent backup or snapshot exists.
- Run on staging before production.
- Capture a count baseline for collections you will modify.

## Run Methods
Local `mongosh` (requires `mongosh` installed):
```bash
mongosh "mongodb://mongo:mongo@localhost:27017/clinic_admin?authSource=admin" \
  docs/db/mongodb_schema.js
```

Docker exec (no local `mongosh` required):
```bash
docker exec -i clinic_admin_mongo \
  mongosh "mongodb://mongo:mongo@localhost:27017/clinic_admin?authSource=admin" \
  < docs/db/mongodb_schema.js
```

## Scripts and When to Run

1) Schema validators and indexes
- Script: `docs/db/mongodb_schema.js`
- When: new environment, or after schema/index changes.
- Command:
```bash
docker exec -i clinic_admin_mongo \
  mongosh "mongodb://mongo:mongo@localhost:27017/clinic_admin?authSource=admin" \
  < docs/db/mongodb_schema.js
```

2) Seed baseline roles/permissions
- Script: `docs/db/seed_permissions.js`
- When: new environment or when permission catalog changes.
- Requires: `ORG_ID` (required), `ORG_NAME`, `ORG_CODE` (optional).
- Command:
```bash
ORG_ID="ORG_UUID" ORG_NAME="Demo Org" ORG_CODE="DEMO" \
  docker exec -i clinic_admin_mongo \
  mongosh "mongodb://mongo:mongo@localhost:27017/clinic_admin?authSource=admin" \
  < docs/db/seed_permissions.js
```

3) Backfill permission scope
- Script: `docs/db/backfill_permission_scope.js`
- When: after adding `scope` to permissions or importing older data.
- Command:
```bash
docker exec -i clinic_admin_mongo \
  mongosh "mongodb://mongo:mongo@localhost:27017/clinic_admin?authSource=admin" \
  < docs/db/backfill_permission_scope.js
```

4) Cleanup orphaned role permissions
- Script: `docs/db/cleanup_role_permissions.js`
- When: after deleting permissions/roles, or fixing data imports.
- Command:
```bash
docker exec -i clinic_admin_mongo \
  mongosh "mongodb://mongo:mongo@localhost:27017/clinic_admin?authSource=admin" \
  < docs/db/cleanup_role_permissions.js
```

5) Cleanup role permissions for inactive permissions
- Script: `docs/db/cleanup_role_permissions_for_inactive_permissions.js`
- When: after deactivating permissions or bulk updates.
- Command:
```bash
docker exec -i clinic_admin_mongo \
  mongosh "mongodb://mongo:mongo@localhost:27017/clinic_admin?authSource=admin" \
  < docs/db/cleanup_role_permissions_for_inactive_permissions.js
```

6) Cleanup role permissions for missing permissions
- Script: `docs/db/cleanup_role_permissions_for_missing_permissions.js`
- When: after migrating or restoring from partial backups.
- Command:
```bash
docker exec -i clinic_admin_mongo \
  mongosh "mongodb://mongo:mongo@localhost:27017/clinic_admin?authSource=admin" \
  < docs/db/cleanup_role_permissions_for_missing_permissions.js
```

## Post-Run Checks
- Validate updated counts for `permissions` and `role_permissions`.
- Run a smoke test on `GET /api/v1/permissions` and role permission endpoints.
