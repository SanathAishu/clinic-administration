ensureCollection("staff", {
  bsonType: "object",
  required: [
    "_id",
    "user_id",
    "organization_id",
    "clinic_id",
    "branch_id",
    "staff_type",
    "employment_status",
    "created_at",
    "updated_at"
  ],
  properties: {
    _id: uuid,
    user_id: uuid,
    organization_id: uuid,
    clinic_id: uuid,
    branch_id: uuid,
    department_id: uuid,
    staff_type: { bsonType: "string", enum: staffTypes },
    specialization: text,
    license_number: text,
    employment_status: { bsonType: "string", enum: activeInactive },
    availability: { bsonType: "array", items: staffAvailabilitySchema },
    leaves: { bsonType: "array", items: staffLeaveSchema },
    created_at: dateType,
    updated_at: dateType
  }
});
ensureIndex("staff", { clinic_id: 1 }, { name: "idx_staff_clinic" });
ensureIndex("staff", { branch_id: 1, user_id: 1 }, { unique: true, name: "uk_staff_branch_user" });
ensureIndex(
  "staff",
  { "availability.day_of_week": 1, "availability.start_time": 1, "availability.end_time": 1 },
  { name: "idx_staff_availability" }
);
ensureIndex(
  "staff",
  { "leaves.start_date": 1, "leaves.end_date": 1 },
  { name: "idx_staff_leaves_range" }
);

ensureCollection("patients", {
  bsonType: "object",
  required: [
    "_id",
    "organization_id",
    "clinic_id",
    "branch_id",
    "first_name",
    "full_name",
    "gender_lookup_type",
    "phone",
    "status",
    "created_at",
    "updated_at"
  ],
  properties: {
    _id: uuid,
    organization_id: uuid,
    clinic_id: uuid,
    branch_id: uuid,
    first_name: text,
    last_name: text,
    full_name: text,
    gender_lookup_type: { bsonType: "string", enum: ["gender"] },
    gender_code: text,
    date_of_birth: dateType,
    phone: text,
    email: text,
    address_line1: text,
    address_line2: text,
    city: text,
    state: text,
    postal_code: text,
    emergency_contact_name: text,
    emergency_contact_phone: text,
    status: { bsonType: "string", enum: patientStatuses },
    allergies: { bsonType: "array", items: allergySchema },
    created_at: dateType,
    updated_at: dateType
  }
});
ensureIndex("patients", { phone: 1 }, { name: "idx_patients_phone" });
ensureIndex("patients", { full_name: 1 }, { name: "idx_patients_name" });
ensureIndex("patients", { clinic_id: 1 }, { name: "idx_patients_clinic" });
ensureIndex(
  "patients",
  { clinic_id: 1, phone: 1 },
  {
    unique: true,
    partialFilterExpression: { status: { $ne: "archived" } },
    name: "uk_patients_clinic_phone_active"
  }
);

ensureCollection("patient_medical_history", {
  bsonType: "object",
  required: ["_id", "patient_id", "entry", "created_at"],
  properties: {
    _id: uuid,
    patient_id: uuid,
    entry: objectType,
    created_at: dateType
  }
});
ensureIndex(
  "patient_medical_history",
  { patient_id: 1, created_at: -1 },
  { name: "idx_patient_medical_history_patient" }
);

ensureCollection("diagnoses", {
  bsonType: "object",
  required: [
    "_id",
    "patient_id",
    "diagnosis",
    "diagnosed_at",
    "created_at",
    "updated_at"
  ],
  properties: {
    _id: uuid,
    patient_id: uuid,
    diagnosed_by: uuid,
    diagnosis: text,
    notes: text,
    diagnosed_at: dateType,
    created_at: dateType,
    updated_at: dateType
  }
});
ensureIndex("diagnoses", { patient_id: 1, diagnosed_at: -1 }, { name: "idx_diagnoses_patient" });
