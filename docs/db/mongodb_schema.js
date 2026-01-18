// MongoDB schema for Clinic Management (best-practice document model).
// Run with: mongosh "mongodb://mongo:mongo@localhost:27017/clinic_admin?authSource=admin" docs/db/mongodb_schema.js
// Embedded collections: user_roles, allergies, prescription_items, invoice_items, discounts, payments,
// order_items, order_status_history, staff_availability, staff_leaves, roster_entries, reminder_messages,
// treatment_sessions, house_visit_events.

const path = require("path");

function resolveSchemaDir() {
  if (typeof __dirname === "string") {
    return path.join(__dirname, "mongodb_schema");
  }
  if (typeof __filename === "string") {
    return path.join(path.dirname(__filename), "mongodb_schema");
  }
  if (typeof process !== "undefined" && Array.isArray(process.argv)) {
    const argv = process.argv.slice().reverse();
    const scriptArg = argv.find((arg) => arg && arg.endsWith("mongodb_schema.js"));
    if (scriptArg) {
      return path.join(path.dirname(scriptArg), "mongodb_schema");
    }
  }
  return "docs/db/mongodb_schema";
}

const schemaDir = resolveSchemaDir();
const schemaFiles = [
  "00_shared.js",
  "10_core.js",
  "20_staff_patients.js",
  "30_appointments_treatments.js",
  "40_billing_finance.js",
  "50_inventory_orders.js",
  "60_operations.js",
  "70_system.js"
];

schemaFiles.forEach((schemaFile) => {
  load(path.join(schemaDir, schemaFile));
});
