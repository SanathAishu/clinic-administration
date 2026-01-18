# MongoDB Collections & Query Patterns (Draft)

Scope: server-side MongoDB collections, validators, indexes, and query patterns for CRUD, sequencing,
and analytics, aligned with `docs/api-endpoints.md`. Schema definitions live in
`docs/db/mongodb_schema/*.js` and are loaded via `docs/db/mongodb_schema.js`. Use transactions for
multi-collection updates (billing, inventory, financial summaries).

## Phase 1 Collection Mapping

**Core reference**
- `lookup_values` -> upsert with `findOneAndUpdate` keyed by `{ lookup_type, code }`.
- `organizations`, `clinics`, `departments`, `branches` -> upsert by `{ organization_id, code }` or
  `{ clinic_id, code }`, enforced by unique indexes.
- `roles`, `users` -> user upsert with `role_ids`/`role_codes` arrays; enforce unique email/phone.
- `permissions` -> upsert by `{ organization_id, permission_code }`; keep `resource` and `action`.
- `role_permissions` -> mapping with unique `{ role_id, permission_id }`.

**Staff & access**
- `staff` -> embedded availability/leaves; enforce unique `{ branch_id, user_id }`.
- `settings` -> upsert by `{ scope, scope_id, section }` for configuration.
- `audit_log` -> append-only inserts with indexed `user_id` and `action`.

**Patients & clinical**
- `patients` -> enforce unique `{ clinic_id, phone }` for non-archived patients.
- `patient_medical_history` -> append history entries; index `{ patient_id, created_at }`.
- `diagnoses` -> upsert and list per patient, sorted by `diagnosed_at`.

**Appointments & reminders**
- `appointments` -> create/update with status transitions; index by patient, provider, clinic, and
  `scheduled_start`.
- `reminder_queue` -> enqueue reminders; index by status and appointment.

**Treatments & packages**
- `treatment_types` -> reference data per clinic.
- `treatments` -> embedded `sessions`; query by patient and status.
- `treatment_packages` -> track remaining sessions with atomic updates.

**Prescriptions**
- `prescriptions` -> embedded items; query by patient/status.

**Billing & payments**
- `invoices` -> embedded items/discounts/payments; query by patient and invoice_number.
- `invoice_sequences` -> per clinic/month counters for invoice numbers.

**Accounting**
- `expenses`, `revenue_ledger`, `financial_summary` -> daily rollups and reporting.

**Orders & inventory**
- `suppliers`, `inventory_items`, `inventory_transactions`, `orders` -> embed order items and
  status history; maintain inventory via transactions.

**House visits**
- `house_visits` -> embedded events; query by provider and `scheduled_at`.

**Rostering**
- `rosters` -> embedded entries; unique by `{ branch_id, week_start }`.

## Query Patterns

### Upserts
- Use `findOneAndUpdate` with `$set` + `$setOnInsert`, `upsert: true`, and `returnDocument: "after"`.
- Validate invariants in the service layer before writes (status transitions, counters, limits).

### Patient Search
- Primary search by exact `phone` and `clinic_id`.
- Secondary search by normalized `full_name` and date ranges.
- Use compound indexes as defined in `docs/db/mongodb_schema/*.js`.

### Timelines and Analytics
- Use aggregation pipelines with `$match`, `$lookup`, `$group`, and `$sort` for:
  - Patient timelines (diagnoses, treatments, prescriptions, invoices).
  - Billing summaries (daily totals, payment mix, outstanding balances).
  - Staff performance (consultations, outcomes, utilization).
- Prefer materialized summaries in `financial_summary` for daily rollups.

### RBAC Permission Resolution
- Resolve effective permissions from user roles via `role_permissions` + `permissions`.

```javascript
const userId = "USER_UUID";

db.users.aggregate([
  { $match: { _id: userId } },
  { $project: { role_ids: 1 } },
  {
    $lookup: {
      from: "role_permissions",
      localField: "role_ids",
      foreignField: "role_id",
      as: "role_permissions"
    }
  },
  { $unwind: "$role_permissions" },
  {
    $lookup: {
      from: "permissions",
      localField: "role_permissions.permission_id",
      foreignField: "_id",
      as: "permission"
    }
  },
  { $unwind: "$permission" },
  { $match: { "permission.active": true } },
  {
    $group: {
      _id: "$permission.permission_code",
      permission: { $first: "$permission" }
    }
  },
  {
    $project: {
      _id: 0,
      permission_code: "$permission.permission_code",
      resource: "$permission.resource",
      action: "$permission.action",
      name: "$permission.name"
    }
  },
  { $sort: { permission_code: 1 } }
]);
```

Baseline seed script: `docs/db/seed_permissions.js`.

### Reporting Aggregations (Samples)

#### `GET /api/v1/reports/daily-summary` (revenue vs expenses)
```javascript
const branchId = "BRANCH_UUID";
const start = new Date("2026-01-01T00:00:00Z");
const end = new Date("2026-02-01T00:00:00Z");

db.revenue_ledger.aggregate([
  { $match: { branch_id: branchId, entry_date: { $gte: start, $lt: end } } },
  {
    $project: {
      day: { $dateToString: { format: "%Y-%m-%d", date: "$entry_date" } },
      revenue: "$amount",
      expense: { $literal: 0 }
    }
  },
  {
    $unionWith: {
      coll: "expenses",
      pipeline: [
        { $match: { branch_id: branchId, expense_date: { $gte: start, $lt: end } } },
        {
          $project: {
            day: { $dateToString: { format: "%Y-%m-%d", date: "$expense_date" } },
            revenue: { $literal: 0 },
            expense: "$amount"
          }
        }
      ]
    }
  },
  {
    $group: {
      _id: "$day",
      revenue_total: { $sum: "$revenue" },
      expense_total: { $sum: "$expense" }
    }
  },
  { $addFields: { profit_loss: { $subtract: ["$revenue_total", "$expense_total"] } } },
  { $sort: { _id: 1 } }
]);
```

#### `GET /api/v1/reports/revenue-analysis` (by clinic and item type)
```javascript
const start = new Date("2026-01-01T00:00:00Z");
const end = new Date("2026-02-01T00:00:00Z");

db.invoices.aggregate([
  {
    $match: {
      issue_date: { $gte: start, $lt: end },
      status: { $in: ["issued", "partially_paid", "paid"] }
    }
  },
  { $unwind: "$items" },
  {
    $group: {
      _id: { clinic_id: "$clinic_id", item_type: "$items.item_type" },
      revenue: { $sum: "$items.amount" },
      invoice_ids: { $addToSet: "$_id" }
    }
  },
  {
    $project: {
      _id: 0,
      clinic_id: "$_id.clinic_id",
      item_type: "$_id.item_type",
      revenue: 1,
      invoice_count: { $size: "$invoice_ids" }
    }
  },
  { $sort: { revenue: -1 } }
]);
```

#### `GET /api/v1/reports/payment-mix` (by payment method)
```javascript
const start = new Date("2026-01-01T00:00:00Z");
const end = new Date("2026-02-01T00:00:00Z");

db.invoices.aggregate([
  { $unwind: "$payments" },
  {
    $match: {
      "payments.status": "received",
      "payments.paid_at": { $gte: start, $lt: end }
    }
  },
  {
    $group: {
      _id: "$payments.method_code",
      amount_total: { $sum: "$payments.amount" },
      payment_count: { $sum: 1 }
    }
  },
  {
    $project: {
      _id: 0,
      method_code: "$_id",
      amount_total: 1,
      payment_count: 1
    }
  },
  { $sort: { amount_total: -1 } }
]);
```

#### `GET /api/v1/reports/staff-performance` (appointments by provider)
```javascript
const start = new Date("2026-01-01T00:00:00Z");
const end = new Date("2026-02-01T00:00:00Z");

db.appointments.aggregate([
  { $match: { scheduled_start: { $gte: start, $lt: end } } },
  {
    $group: {
      _id: "$provider_id",
      scheduled: { $sum: { $cond: [{ $eq: ["$status", "scheduled"] }, 1, 0] } },
      in_progress: { $sum: { $cond: [{ $eq: ["$status", "in_progress"] }, 1, 0] } },
      completed: { $sum: { $cond: [{ $eq: ["$status", "completed"] }, 1, 0] } },
      cancelled: { $sum: { $cond: [{ $eq: ["$status", "cancelled"] }, 1, 0] } },
      no_show: { $sum: { $cond: [{ $eq: ["$status", "no_show"] }, 1, 0] } }
    }
  },
  { $lookup: { from: "staff", localField: "_id", foreignField: "_id", as: "staff" } },
  { $unwind: { path: "$staff", preserveNullAndEmptyArrays: true } },
  { $lookup: { from: "users", localField: "staff.user_id", foreignField: "_id", as: "user" } },
  { $unwind: { path: "$user", preserveNullAndEmptyArrays: true } },
  {
    $project: {
      _id: 0,
      provider_id: "$_id",
      staff_name: "$user.full_name",
      scheduled: 1,
      in_progress: 1,
      completed: 1,
      cancelled: 1,
      no_show: 1
    }
  },
  { $sort: { completed: -1 } }
]);
```

#### `GET /api/v1/reports/appointment-analytics` (daily status counts)
```javascript
const start = new Date("2026-01-01T00:00:00Z");
const end = new Date("2026-02-01T00:00:00Z");

db.appointments.aggregate([
  { $match: { scheduled_start: { $gte: start, $lt: end } } },
  {
    $project: {
      day: { $dateToString: { format: "%Y-%m-%d", date: "$scheduled_start" } },
      status: 1
    }
  },
  {
    $group: {
      _id: { day: "$day", status: "$status" },
      count: { $sum: 1 }
    }
  },
  {
    $group: {
      _id: "$_id.day",
      total: { $sum: "$count" },
      counts: { $push: { status: "$_id.status", count: "$count" } }
    }
  },
  { $sort: { _id: 1 } }
]);
```

### Sequencing
- Generate invoice numbers via `invoice_sequences` using `findOneAndUpdate` + `$inc` inside a
  transaction scoped to `{ organization_id, clinic_id, branch_id, year, month }`.
- Enforce uniqueness with the `uk_invoice_sequences` index.

### Transactions
- Invoice creation: create invoice + write ledger entry + optional payment record atomically.
- Inventory receive/issue: update `inventory_items` + insert `inventory_transactions`.
- Order status updates: update order + adjust inventory if needed.

### Reminders
- `reminder_queue` is the source of truth for outbound messages.
- Use `messages.status` indexes for failed/sent tracking and retries.
- If automatic cleanup is needed, add a TTL index on a terminal status timestamp.

### House Visit Tracking
- Append events to `house_visits.events` and update visit status with guarded transitions.
- If geospatial queries are required, add a GeoJSON `location` field and a `2dsphere` index.

## Notes
- Use schema validators and enums from `docs/db/mongodb_schema/00_shared.js`.
- Prefer embedding for strongly owned data (sessions, items, status history); reference for shared
  entities (users, clinics, patients).
- Use deterministic state transitions and record audit events in `audit_log`.
