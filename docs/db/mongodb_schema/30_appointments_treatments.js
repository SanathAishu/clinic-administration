ensureCollection("appointments", {
  bsonType: "object",
  required: [
    "_id",
    "organization_id",
    "clinic_id",
    "branch_id",
    "patient_id",
    "scheduled_start",
    "scheduled_end",
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
    department_id: uuid,
    scheduled_start: dateType,
    scheduled_end: dateType,
    status: { bsonType: "string", enum: appointmentStatuses },
    reason: text,
    notes: text,
    created_by: uuid,
    created_at: dateType,
    updated_at: dateType
  }
});
ensureIndex("appointments", { patient_id: 1 }, { name: "idx_appointments_patient" });
ensureIndex("appointments", { provider_id: 1 }, { name: "idx_appointments_provider" });
ensureIndex("appointments", { clinic_id: 1 }, { name: "idx_appointments_clinic" });
ensureIndex("appointments", { scheduled_start: 1 }, { name: "idx_appointments_start" });

ensureCollection("treatment_types", {
  bsonType: "object",
  required: [
    "_id",
    "organization_id",
    "clinic_id",
    "name",
    "category_lookup_type",
    "duration_minutes",
    "base_rate",
    "active",
    "created_at",
    "updated_at"
  ],
  properties: {
    _id: uuid,
    organization_id: uuid,
    clinic_id: uuid,
    name: text,
    category_lookup_type: { bsonType: "string", enum: ["treatment_category"] },
    category_code: text,
    duration_minutes: { bsonType: "int", minimum: 1 },
    base_rate: decimalType,
    active: bool,
    created_at: dateType,
    updated_at: dateType
  }
});
ensureIndex("treatment_types", { clinic_id: 1, name: 1 }, { unique: true, name: "uk_treatment_types_clinic_name" });

ensureCollection("treatments", {
  bsonType: "object",
  required: [
    "_id",
    "organization_id",
    "clinic_id",
    "branch_id",
    "patient_id",
    "treatment_type_id",
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
    appointment_id: uuid,
    treatment_type_id: uuid,
    provider_id: uuid,
    status: { bsonType: "string", enum: treatmentStatuses },
    notes: text,
    sessions: { bsonType: "array", items: treatmentSessionSchema },
    created_at: dateType,
    updated_at: dateType
  }
});
ensureIndex("treatments", { patient_id: 1 }, { name: "idx_treatments_patient" });

ensureCollection("treatment_packages", {
  bsonType: "object",
  required: [
    "_id",
    "patient_id",
    "treatment_type_id",
    "total_sessions",
    "remaining_sessions",
    "status",
    "created_at",
    "updated_at"
  ],
  properties: {
    _id: uuid,
    patient_id: uuid,
    treatment_type_id: uuid,
    total_sessions: { bsonType: "int", minimum: 0 },
    remaining_sessions: { bsonType: "int", minimum: 0 },
    start_date: dateType,
    end_date: dateType,
    status: { bsonType: "string", enum: packageStatuses },
    created_at: dateType,
    updated_at: dateType
  }
});

ensureCollection("prescriptions", {
  bsonType: "object",
  required: ["_id", "patient_id", "status", "created_at", "updated_at"],
  properties: {
    _id: uuid,
    patient_id: uuid,
    diagnosis_id: uuid,
    prescribed_by: uuid,
    status: { bsonType: "string", enum: prescriptionStatuses },
    notes: text,
    start_date: dateType,
    end_date: dateType,
    items: { bsonType: "array", items: prescriptionItemSchema },
    created_at: dateType,
    updated_at: dateType
  }
});
ensureIndex("prescriptions", { patient_id: 1 }, { name: "idx_prescriptions_patient" });
