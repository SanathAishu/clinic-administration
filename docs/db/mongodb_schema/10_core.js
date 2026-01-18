ensureCollection("lookup_values", {
  bsonType: "object",
  required: [
    "_id",
    "lookup_type",
    "code",
    "label",
    "sort_order",
    "active",
    "meta",
    "created_at",
    "updated_at"
  ],
  properties: {
    _id: uuid,
    lookup_type: text,
    code: text,
    label: text,
    sort_order: intOrLong,
    active: bool,
    meta: objectType,
    created_at: dateType,
    updated_at: dateType
  }
});
ensureIndex("lookup_values", { lookup_type: 1, code: 1 }, { unique: true, name: "uk_lookup_values_type_code" });

ensureCollection("organizations", {
  bsonType: "object",
  required: ["_id", "name", "code", "active", "created_at", "updated_at"],
  properties: {
    _id: uuid,
    name: text,
    code: text,
    active: bool,
    created_at: dateType,
    updated_at: dateType
  }
});
ensureIndex("organizations", { code: 1 }, { unique: true, name: "uk_organizations_code" });

ensureCollection("clinics", {
  bsonType: "object",
  required: [
    "_id",
    "organization_id",
    "name",
    "code",
    "timezone",
    "active",
    "created_at",
    "updated_at"
  ],
  properties: {
    _id: uuid,
    organization_id: uuid,
    name: text,
    code: text,
    address_line1: text,
    address_line2: text,
    city: text,
    state: text,
    postal_code: text,
    phone: text,
    email: text,
    timezone: text,
    active: bool,
    created_at: dateType,
    updated_at: dateType
  }
});
ensureIndex("clinics", { organization_id: 1, code: 1 }, { unique: true, name: "uk_clinics_org_code" });

ensureCollection("departments", {
  bsonType: "object",
  required: ["_id", "clinic_id", "name", "code", "active", "created_at", "updated_at"],
  properties: {
    _id: uuid,
    clinic_id: uuid,
    name: text,
    code: text,
    active: bool,
    created_at: dateType,
    updated_at: dateType
  }
});
ensureIndex("departments", { clinic_id: 1, code: 1 }, { unique: true, name: "uk_departments_clinic_code" });

ensureCollection("branches", {
  bsonType: "object",
  required: [
    "_id",
    "organization_id",
    "clinic_id",
    "name",
    "code",
    "active",
    "created_at",
    "updated_at"
  ],
  properties: {
    _id: uuid,
    organization_id: uuid,
    clinic_id: uuid,
    name: text,
    code: text,
    address_line1: text,
    address_line2: text,
    city: text,
    state: text,
    postal_code: text,
    phone: text,
    email: text,
    active: bool,
    created_at: dateType,
    updated_at: dateType
  }
});
ensureIndex("branches", { organization_id: 1, code: 1 }, { unique: true, name: "uk_branches_org_code" });
ensureIndex("branches", { clinic_id: 1, code: 1 }, { unique: true, name: "uk_branches_clinic_code" });

ensureCollection("roles", {
  bsonType: "object",
  required: ["_id", "organization_id", "name", "role_code", "created_at", "updated_at"],
  properties: {
    _id: uuid,
    organization_id: uuid,
    name: text,
    role_code: text,
    description: text,
    created_at: dateType,
    updated_at: dateType
  }
});
ensureIndex("roles", { organization_id: 1, name: 1 }, { unique: true, name: "uk_roles_org_name" });
ensureIndex("roles", { organization_id: 1, role_code: 1 }, { unique: true, name: "uk_roles_org_code" });

ensureCollection("permissions", {
  bsonType: "object",
  required: [
    "_id",
    "organization_id",
    "name",
    "permission_code",
    "scope",
    "resource",
    "action",
    "active",
    "created_at",
    "updated_at"
  ],
  properties: {
    _id: uuid,
    organization_id: uuid,
    name: text,
    permission_code: text,
    scope: { bsonType: "string", enum: ["tenant", "system"] },
    resource: text,
    action: text,
    description: text,
    active: bool,
    created_at: dateType,
    updated_at: dateType
  }
});
ensureIndex("permissions", { organization_id: 1, permission_code: 1 }, { unique: true, name: "uk_permissions_org_code" });
ensureIndex("permissions", { organization_id: 1, resource: 1, action: 1 }, { name: "idx_permissions_resource_action" });

ensureCollection("role_permissions", {
  bsonType: "object",
  required: ["_id", "organization_id", "role_id", "permission_id", "created_at", "updated_at"],
  properties: {
    _id: uuid,
    organization_id: uuid,
    role_id: uuid,
    permission_id: uuid,
    created_at: dateType,
    updated_at: dateType
  }
});
ensureIndex("role_permissions", { role_id: 1, permission_id: 1 }, { unique: true, name: "uk_role_permissions_role_permission" });
ensureIndex("role_permissions", { permission_id: 1 }, { name: "idx_role_permissions_permission" });

ensureCollection("users", {
  bsonType: "object",
  required: ["_id", "organization_id", "full_name", "status", "created_at", "updated_at"],
  properties: {
    _id: uuid,
    organization_id: uuid,
    full_name: text,
    email: text,
    phone: text,
    password_hash: text,
    status: { bsonType: "string", enum: activeInactive },
    role_ids: { bsonType: "array", items: uuid },
    role_codes: { bsonType: "array", items: text },
    created_at: dateType,
    updated_at: dateType
  }
});
ensureIndex("users", { phone: 1 }, { name: "idx_users_phone" });
ensureIndex("users", { email: 1 }, { name: "idx_users_email" });
ensureIndex("users", { role_codes: 1 }, { name: "idx_users_role_codes" });
