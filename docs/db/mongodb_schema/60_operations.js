ensureCollection("house_visits", {
  bsonType: "object",
  required: [
    "_id",
    "organization_id",
    "clinic_id",
    "branch_id",
    "patient_id",
    "scheduled_at",
    "status",
    "created_at",
    "updated_at"
  ],
  properties: {
    _id: uuid,
    organization_id: uuid,
    clinic_id: uuid,
    branch_id: uuid,
    patient_id: uuid,
    provider_id: uuid,
    scheduled_at: dateType,
    status: { bsonType: "string", enum: houseVisitStatuses },
    address: text,
    latitude: decimalType,
    longitude: decimalType,
    notes: text,
    events: { bsonType: "array", items: houseVisitEventSchema },
    created_at: dateType,
    updated_at: dateType
  }
});
ensureIndex("house_visits", { provider_id: 1 }, { name: "idx_house_visits_provider" });

ensureCollection("rosters", {
  bsonType: "object",
  required: [
    "_id",
    "organization_id",
    "clinic_id",
    "branch_id",
    "week_start",
    "status",
    "created_at",
    "updated_at"
  ],
  properties: {
    _id: uuid,
    organization_id: uuid,
    clinic_id: uuid,
    branch_id: uuid,
    week_start: dateType,
    status: { bsonType: "string", enum: rosterStatuses },
    entries: { bsonType: "array", items: rosterEntrySchema },
    created_at: dateType,
    updated_at: dateType
  }
});
ensureIndex("rosters", { branch_id: 1, week_start: 1 }, { unique: true, name: "uk_rosters_branch_week" });

ensureCollection("reminder_queue", {
  bsonType: "object",
  required: [
    "_id",
    "appointment_id",
    "channels",
    "status",
    "details",
    "created_at",
    "updated_at"
  ],
  properties: {
    _id: uuid,
    appointment_id: uuid,
    channels: {
      bsonType: "array",
      items: { bsonType: "string", enum: reminderChannels }
    },
    status: { bsonType: "string", enum: reminderStatuses },
    scheduled_for: dateType,
    sent_at: dateType,
    failure_reason: text,
    details: objectType,
    messages: { bsonType: "array", items: reminderMessageSchema },
    created_at: dateType,
    updated_at: dateType
  }
});
ensureIndex("reminder_queue", { status: 1 }, { name: "idx_reminder_queue_status" });
ensureIndex("reminder_queue", { appointment_id: 1 }, { name: "idx_reminder_queue_appt" });
ensureIndex("reminder_queue", { "messages.status": 1 }, { name: "idx_reminder_messages_status" });
