// Seed baseline roles, permissions, and role-permissions.
// Run with: mongosh "mongodb://localhost:27017/clinic_admin" docs/db/seed_permissions.js

const env = typeof process !== "undefined" && process.env ? process.env : {};
const organizationId = env.ORG_ID || "";
const organizationName = env.ORG_NAME || "";
const organizationCode = env.ORG_CODE || "";
if (!organizationId) {
  throw new Error("Set ORG_ID before running, e.g. ORG_ID=... mongosh ... seed_permissions.js");
}

const now = new Date();

function newId() {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  const bytes = Array.from({ length: 16 }, () => Math.floor(Math.random() * 256));
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  const hex = bytes.map((b) => b.toString(16).padStart(2, "0")).join("");
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

const permissions = [];
const permissionCodes = new Set();

function addPermission(resource, action, name, description) {
  const permissionCode = `${resource}.${action}`;
  if (permissionCodes.has(permissionCode)) {
    throw new Error(`Duplicate permission code: ${permissionCode}`);
  }
  permissions.push({
    permission_code: permissionCode,
    resource,
    action,
    name,
    description: description || ""
  });
  permissionCodes.add(permissionCode);
}

function addCrud(resource, label) {
  addPermission(resource, "read", `${label} Read`);
  addPermission(resource, "create", `${label} Create`);
  addPermission(resource, "update", `${label} Update`);
  addPermission(resource, "delete", `${label} Delete`);
}

function addReadCreate(resource, label) {
  addPermission(resource, "read", `${label} Read`);
  addPermission(resource, "create", `${label} Create`);
}

function addReadUpdate(resource, label) {
  addPermission(resource, "read", `${label} Read`);
  addPermission(resource, "update", `${label} Update`);
}

function addReadOnly(resource, label) {
  addPermission(resource, "read", `${label} Read`);
}

[
  ["lookup_values", "Lookup Values"],
  ["organizations", "Organizations"],
  ["clinics", "Clinics"],
  ["branches", "Branches"],
  ["departments", "Departments"],
  ["roles", "Roles"],
  ["permissions", "Permissions"],
  ["users", "Users"],
  ["staff", "Staff"],
  ["staff_leaves", "Staff Leaves"],
  ["rosters", "Rosters"],
  ["patients", "Patients"],
  ["patient_medical_history", "Patient Medical History"],
  ["allergies", "Allergies"],
  ["diagnoses", "Diagnoses"],
  ["appointments", "Appointments"],
  ["treatment_types", "Treatment Types"],
  ["treatments", "Treatments"],
  ["treatment_sessions", "Treatment Sessions"],
  ["treatment_packages", "Treatment Packages"],
  ["prescriptions", "Prescriptions"],
  ["invoices", "Invoices"],
  ["discounts", "Discounts"],
  ["expenses", "Expenses"],
  ["suppliers", "Suppliers"],
  ["inventory_items", "Inventory Items"],
  ["orders", "Orders"],
  ["house_visits", "House Visits"]
].forEach(([resource, label]) => addCrud(resource, label));

[
  ["reminders", "Reminders"],
  ["inventory_transactions", "Inventory Transactions"],
  ["revenue_ledger", "Revenue Ledger"],
  ["payments", "Payments"]
].forEach(([resource, label]) => addReadCreate(resource, label));

[
  ["settings", "Settings"],
  ["staff_availability", "Staff Availability"]
].forEach(([resource, label]) => addReadUpdate(resource, label));

[
  ["audit_log", "Audit Log"],
  ["financial_summary", "Financial Summary"],
  ["inventory_levels", "Inventory Levels"],
  ["reports", "Reports"],
  ["receipts", "Receipts"]
].forEach(([resource, label]) => addReadOnly(resource, label));

[
  ["users", "assign_roles", "Users Assign Roles"],
  ["users", "status", "Users Status Change"],
  ["patients", "duplicates", "Patients Duplicate Check"],
  ["patients", "timeline", "Patients Timeline Read"],
  ["appointments", "status", "Appointments Status Change"],
  ["appointments", "reschedule", "Appointments Reschedule"],
  ["appointments", "available_slots", "Appointments Available Slots"],
  ["appointments", "optimize", "Appointments Optimize"],
  ["appointments", "queue_stats", "Appointments Queue Stats"],
  ["treatments", "results", "Treatments Results Read"],
  ["treatment_packages", "consume", "Treatment Packages Consume"],
  ["treatment_packages", "status", "Treatment Packages Status Change"],
  ["prescriptions", "fulfill", "Prescriptions Fulfill"],
  ["prescriptions", "adjust", "Prescriptions Adjust"],
  ["prescriptions", "approve", "Prescriptions Approve"],
  ["prescriptions", "status", "Prescriptions Status Change"],
  ["invoices", "status", "Invoices Status Change"],
  ["invoices", "next_number", "Invoices Next Number"],
  ["discounts", "approve", "Discounts Approve"],
  ["discounts", "history", "Discounts History Read"],
  ["rosters", "generate", "Rosters Generate"],
  ["rosters", "publish", "Rosters Publish"],
  ["reminders", "retry", "Reminders Retry"],
  ["orders", "status", "Orders Status Change"],
  ["orders", "receive", "Orders Receive"],
  ["orders", "issue", "Orders Issue"],
  ["orders", "return", "Orders Return"],
  ["house_visits", "status", "House Visits Status Change"],
  ["house_visits", "events", "House Visits Events"],
  ["house_visits", "location", "House Visits Location Update"],
  ["house_visits", "attachments", "House Visits Attachments"],
  ["house_visits", "routes", "House Visits Routes Read"],
  ["house_visits", "optimize", "House Visits Routes Optimize"],
  ["staff", "performance", "Staff Performance Read"],
  ["refunds", "create", "Refunds Create"]
].forEach(([resource, action, name]) => addPermission(resource, action, name));

function upsertPermission(permission) {
  const filter = {
    organization_id: organizationId,
    permission_code: permission.permission_code
  };
  db.permissions.updateOne(
    filter,
    {
      $set: {
        organization_id: organizationId,
        permission_code: permission.permission_code,
        name: permission.name,
        resource: permission.resource,
        action: permission.action,
        description: permission.description,
        active: true,
        updated_at: now
      },
      $setOnInsert: {
        _id: newId(),
        created_at: now
      }
    },
    { upsert: true }
  );
  return db.permissions.findOne(filter);
}

function upsertRole(role) {
  const filter = { organization_id: organizationId, role_code: role.role_code };
  db.roles.updateOne(
    filter,
    {
      $set: {
        organization_id: organizationId,
        name: role.name,
        role_code: role.role_code,
        description: role.description,
        updated_at: now
      },
      $setOnInsert: {
        _id: newId(),
        created_at: now
      }
    },
    { upsert: true }
  );
  return db.roles.findOne(filter);
}

function upsertOrganization() {
  if (!organizationName || !organizationCode) {
    print("Skipping organization seed; set ORG_NAME and ORG_CODE to create.");
    return null;
  }
  db.organizations.updateOne(
    { _id: organizationId },
    {
      $set: {
        name: organizationName,
        code: organizationCode,
        active: true,
        updated_at: now
      },
      $setOnInsert: {
        _id: organizationId,
        created_at: now
      }
    },
    { upsert: true }
  );
  return db.organizations.findOne({ _id: organizationId });
}

const roleDefs = [
  { role_code: "admin", name: "Admin", description: "Full access to all modules." },
  { role_code: "doctor", name: "Doctor", description: "Clinical access with limited billing." },
  {
    role_code: "physiotherapist",
    name: "Physiotherapist",
    description: "Treatment delivery and appointment management."
  },
  { role_code: "reception", name: "Reception", description: "Front desk operations." }
];

const permissionDocs = {};
permissions.forEach((permission) => {
  const doc = upsertPermission(permission);
  permissionDocs[permission.permission_code] = doc;
});

const organizationDoc = upsertOrganization();

const roleDocs = {};
roleDefs.forEach((role) => {
  roleDocs[role.role_code] = upsertRole(role);
});

const grants = {
  admin: new Set(permissions.map((permission) => permission.permission_code)),
  doctor: new Set(),
  physiotherapist: new Set(),
  reception: new Set()
};

function grant(roleCode, resource, action) {
  grants[roleCode].add(`${resource}.${action}`);
}

function grantRead(roleCode, resources) {
  resources.forEach((resource) => grant(roleCode, resource, "read"));
}

function grantCreate(roleCode, resources) {
  resources.forEach((resource) => grant(roleCode, resource, "create"));
}

function grantUpdate(roleCode, resources) {
  resources.forEach((resource) => grant(roleCode, resource, "update"));
}

function grantDelete(roleCode, resources) {
  resources.forEach((resource) => grant(roleCode, resource, "delete"));
}

function grantReadCreateUpdate(roleCode, resources) {
  grantRead(roleCode, resources);
  grantCreate(roleCode, resources);
  grantUpdate(roleCode, resources);
}

const referenceResources = ["lookup_values", "organizations", "clinics", "branches", "departments"];

// Doctor
grantRead("doctor", referenceResources);
grantRead("doctor", ["users", "staff"]);
grantReadCreateUpdate("doctor", ["patients", "patient_medical_history", "allergies", "diagnoses"]);
grantRead("doctor", ["appointments", "treatment_types", "treatment_packages"]);
grantReadCreateUpdate("doctor", ["treatments", "treatment_sessions"]);
grantReadCreateUpdate("doctor", ["prescriptions"]);
grantRead("doctor", ["invoices", "payments", "discounts", "suppliers", "inventory_items", "orders"]);
grantReadCreateUpdate("doctor", ["house_visits"]);
grantRead("doctor", ["reports"]);
grant("doctor", "appointments", "status");
grant("doctor", "appointments", "available_slots");
grant("doctor", "appointments", "queue_stats");
grant("doctor", "treatments", "results");
grant("doctor", "treatment_packages", "consume");
grant("doctor", "treatment_packages", "status");
grant("doctor", "prescriptions", "adjust");
grant("doctor", "prescriptions", "approve");
grant("doctor", "prescriptions", "status");
grant("doctor", "house_visits", "status");
grant("doctor", "house_visits", "events");
grant("doctor", "house_visits", "location");
grant("doctor", "house_visits", "attachments");
grant("doctor", "house_visits", "routes");
grant("doctor", "house_visits", "optimize");

// Physiotherapist
grantRead("physiotherapist", referenceResources);
grantRead("physiotherapist", ["users", "staff"]);
grantReadCreateUpdate("physiotherapist", ["patients"]);
grantRead("physiotherapist", ["patient_medical_history", "allergies", "diagnoses"]);
grantRead("physiotherapist", ["appointments", "treatment_types", "treatment_packages"]);
grantReadCreateUpdate("physiotherapist", ["treatments", "treatment_sessions"]);
grantRead("physiotherapist", ["prescriptions", "invoices", "payments", "discounts"]);
grantRead("physiotherapist", ["suppliers", "inventory_items", "orders"]);
grantReadCreateUpdate("physiotherapist", ["house_visits"]);
grantRead("physiotherapist", ["reports"]);
grant("physiotherapist", "appointments", "status");
grant("physiotherapist", "appointments", "available_slots");
grant("physiotherapist", "appointments", "queue_stats");
grant("physiotherapist", "treatments", "results");
grant("physiotherapist", "treatment_packages", "consume");
grant("physiotherapist", "treatment_packages", "status");
grant("physiotherapist", "house_visits", "status");
grant("physiotherapist", "house_visits", "events");
grant("physiotherapist", "house_visits", "location");
grant("physiotherapist", "house_visits", "attachments");
grant("physiotherapist", "house_visits", "routes");
grant("physiotherapist", "house_visits", "optimize");

// Reception
grantRead("reception", referenceResources);
grantRead("reception", ["users", "staff"]);
grantReadCreateUpdate("reception", ["patients"]);
grantRead("reception", ["patient_medical_history", "allergies", "diagnoses"]);
grantRead("reception", ["appointments", "treatment_types", "treatments", "treatment_packages", "prescriptions"]);
grantCreate("reception", ["appointments"]);
grantUpdate("reception", ["appointments"]);
grantDelete("reception", ["appointments"]);
grantReadCreateUpdate("reception", ["invoices", "discounts"]);
grantRead("reception", ["payments", "receipts"]);
grantCreate("reception", ["payments"]);
grantRead("reception", ["suppliers", "inventory_items", "orders", "house_visits"]);
grantCreate("reception", ["orders"]);
grantUpdate("reception", ["orders"]);
grantRead("reception", ["reports"]);
grant("reception", "appointments", "status");
grant("reception", "appointments", "reschedule");
grant("reception", "appointments", "available_slots");
grant("reception", "appointments", "queue_stats");
grant("reception", "reminders", "read");
grant("reception", "reminders", "create");
grant("reception", "reminders", "retry");
grant("reception", "invoices", "status");
grant("reception", "invoices", "next_number");
grant("reception", "discounts", "history");

function assertPermission(code) {
  if (!permissionDocs[code]) {
    throw new Error(`Unknown permission code: ${code}`);
  }
}

Object.keys(grants).forEach((roleCode) => {
  grants[roleCode].forEach((code) => assertPermission(code));
});

Object.keys(grants).forEach((roleCode) => {
  const role = roleDocs[roleCode];
  if (!role) {
    throw new Error(`Role not found: ${roleCode}`);
  }
  grants[roleCode].forEach((code) => {
    const permission = permissionDocs[code];
    db.role_permissions.updateOne(
      {
        organization_id: organizationId,
        role_id: role._id,
        permission_id: permission._id
      },
      {
        $set: {
          organization_id: organizationId,
          role_id: role._id,
          permission_id: permission._id,
          updated_at: now
        },
        $setOnInsert: {
          _id: newId(),
          created_at: now
        }
      },
      { upsert: true }
    );
  });
});

print(`Seeded permissions: ${permissions.length}`);
print(`Seeded roles: ${roleDefs.length}`);
if (organizationDoc) {
  print(`Seeded organization: ${organizationDoc.name} (${organizationDoc.code})`);
}
