// Shared schema fragments and helpers for Clinic Management MongoDB collections.

const uuid = { bsonType: "string", description: "UUID string" };
const text = { bsonType: "string" };
const bool = { bsonType: "bool" };
const intType = { bsonType: "int" };
const intOrLong = { bsonType: ["int", "long"] };
const decimalType = { bsonType: ["decimal", "double", "int", "long"] };
const dateType = { bsonType: "date" };
const objectType = { bsonType: "object" };
const timeType = { bsonType: "string" };

const positiveQuantity = {
  bsonType: ["decimal", "double", "int", "long"],
  minimum: 0,
  exclusiveMinimum: true
};

const activeInactive = ["active", "inactive"];
const staffTypes = ["doctor", "physiotherapist", "admin", "reception"];
const patientStatuses = ["active", "inactive", "archived"];
const appointmentStatuses = ["scheduled", "in_progress", "completed", "cancelled", "no_show"];
const treatmentStatuses = ["planned", "in_progress", "completed", "cancelled"];
const packageStatuses = ["active", "completed", "cancelled"];
const prescriptionStatuses = ["draft", "active", "completed", "cancelled"];
const invoiceStatuses = ["draft", "issued", "partially_paid", "paid", "void"];
const invoiceItemTypes = ["treatment", "package", "product", "misc"];
const paymentStatuses = ["received", "failed", "refunded"];
const orderStatuses = ["placed", "received", "issued", "returned", "cancelled"];
const houseVisitStatuses = ["scheduled", "en_route", "completed", "cancelled"];
const houseVisitEventStatuses = [
  "scheduled",
  "en_route",
  "arrived",
  "started",
  "paused",
  "completed",
  "cancelled"
];
const staffLeaveStatuses = ["pending", "approved", "rejected"];
const rosterStatuses = ["draft", "published", "archived"];
const reminderStatuses = ["queued", "sent", "failed", "bounced"];
const reminderChannels = ["sms", "whatsapp", "email"];
const revenueSources = ["invoice", "payment", "adjustment"];

const allergySchema = {
  bsonType: "object",
  required: ["id", "allergen", "active", "created_at", "updated_at"],
  properties: {
    id: uuid,
    allergen: text,
    reaction: text,
    severity: text,
    active: bool,
    created_at: dateType,
    updated_at: dateType
  }
};

const staffAvailabilitySchema = {
  bsonType: "object",
  required: ["id", "day_of_week", "start_time", "end_time", "created_at", "updated_at"],
  properties: {
    id: uuid,
    day_of_week: { bsonType: "int", minimum: 0, maximum: 6 },
    start_time: timeType,
    end_time: timeType,
    created_at: dateType,
    updated_at: dateType
  }
};

const staffLeaveSchema = {
  bsonType: "object",
  required: ["id", "start_date", "end_date", "status", "created_at", "updated_at"],
  properties: {
    id: uuid,
    start_date: dateType,
    end_date: dateType,
    reason: text,
    status: { bsonType: "string", enum: staffLeaveStatuses },
    created_at: dateType,
    updated_at: dateType
  }
};

const treatmentSessionSchema = {
  bsonType: "object",
  required: ["id", "session_at", "created_at"],
  properties: {
    id: uuid,
    session_at: dateType,
    duration_minutes: intOrLong,
    outcome_rating: { bsonType: "int", minimum: 1, maximum: 5 },
    notes: text,
    created_at: dateType
  }
};

const prescriptionItemSchema = {
  bsonType: "object",
  required: ["id", "quantity", "created_at"],
  properties: {
    id: uuid,
    treatment_type_id: uuid,
    quantity: { bsonType: "int", minimum: 1 },
    instructions: text,
    created_at: dateType
  }
};

const invoiceItemSchema = {
  bsonType: "object",
  required: ["id", "item_type", "description", "quantity", "unit_price", "amount", "created_at"],
  properties: {
    id: uuid,
    item_type: { bsonType: "string", enum: invoiceItemTypes },
    ref_id: uuid,
    description: text,
    quantity: positiveQuantity,
    unit_price: decimalType,
    amount: decimalType,
    created_at: dateType
  }
};

const discountSchema = {
  bsonType: "object",
  required: ["id", "reason_lookup_type", "amount", "created_at"],
  properties: {
    id: uuid,
    reason_lookup_type: { bsonType: "string", enum: ["discount_reason"] },
    reason_code: text,
    amount: decimalType,
    approved_by: uuid,
    created_at: dateType
  }
};

const paymentSchema = {
  bsonType: "object",
  required: ["id", "amount", "method_lookup_type", "status", "paid_at", "created_at"],
  properties: {
    id: uuid,
    amount: decimalType,
    method_lookup_type: { bsonType: "string", enum: ["payment_method"] },
    method_code: text,
    status: { bsonType: "string", enum: paymentStatuses },
    paid_at: dateType,
    reference: text,
    created_at: dateType
  }
};

const orderItemSchema = {
  bsonType: "object",
  required: ["id", "quantity", "unit_price", "created_at"],
  properties: {
    id: uuid,
    inventory_item_id: uuid,
    description: text,
    quantity: positiveQuantity,
    unit_price: decimalType,
    created_at: dateType
  }
};

const orderStatusSchema = {
  bsonType: "object",
  required: ["id", "status", "changed_at"],
  properties: {
    id: uuid,
    status: { bsonType: "string", enum: orderStatuses },
    changed_at: dateType,
    changed_by: uuid,
    notes: text
  }
};

const rosterEntrySchema = {
  bsonType: "object",
  required: ["id", "staff_id", "shift_date", "start_time", "end_time", "created_at"],
  properties: {
    id: uuid,
    staff_id: uuid,
    shift_date: dateType,
    start_time: timeType,
    end_time: timeType,
    created_at: dateType
  }
};

const reminderMessageSchema = {
  bsonType: "object",
  required: [
    "id",
    "channel",
    "status",
    "attempt",
    "payload",
    "response",
    "created_at",
    "updated_at"
  ],
  properties: {
    id: uuid,
    channel: { bsonType: "string", enum: reminderChannels },
    provider: text,
    provider_message_id: text,
    status: { bsonType: "string", enum: reminderStatuses },
    attempt: intOrLong,
    last_attempt_at: dateType,
    payload: objectType,
    response: objectType,
    created_at: dateType,
    updated_at: dateType
  }
};

const houseVisitEventSchema = {
  bsonType: "object",
  required: ["id", "status", "recorded_at"],
  properties: {
    id: uuid,
    status: { bsonType: "string", enum: houseVisitEventStatuses },
    latitude: decimalType,
    longitude: decimalType,
    notes: text,
    recorded_at: dateType,
    created_by: uuid
  }
};

const existing = new Set(db.getCollectionNames());

function ensureCollection(name, schema) {
  if (existing.has(name)) {
    return;
  }
  db.createCollection(name, {
    validator: { $jsonSchema: schema },
    validationLevel: "strict",
    validationAction: "error"
  });
  existing.add(name);
}

function ensureIndex(name, keys, options) {
  db.getCollection(name).createIndex(keys, options || {});
}
