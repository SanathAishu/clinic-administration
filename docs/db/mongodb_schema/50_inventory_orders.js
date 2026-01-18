ensureCollection("suppliers", {
  bsonType: "object",
  required: ["_id", "name", "active", "created_at", "updated_at"],
  properties: {
    _id: uuid,
    name: text,
    phone: text,
    email: text,
    address: text,
    active: bool,
    created_at: dateType,
    updated_at: dateType
  }
});

ensureCollection("inventory_items", {
  bsonType: "object",
  required: [
    "_id",
    "organization_id",
    "clinic_id",
    "branch_id",
    "name",
    "quantity_on_hand",
    "reorder_level",
    "active",
    "created_at",
    "updated_at"
  ],
  properties: {
    _id: uuid,
    organization_id: uuid,
    clinic_id: uuid,
    branch_id: uuid,
    name: text,
    sku: text,
    unit: text,
    quantity_on_hand: { bsonType: ["decimal", "double", "int", "long"], minimum: 0 },
    reorder_level: { bsonType: ["decimal", "double", "int", "long"], minimum: 0 },
    active: bool,
    created_at: dateType,
    updated_at: dateType
  }
});
ensureIndex("inventory_items", { clinic_id: 1, name: 1 }, { unique: true, name: "uk_inventory_items_clinic_name" });

ensureCollection("inventory_transactions", {
  bsonType: "object",
  required: [
    "_id",
    "organization_id",
    "clinic_id",
    "branch_id",
    "inventory_item_id",
    "quantity_delta",
    "reason",
    "created_at"
  ],
  properties: {
    _id: uuid,
    organization_id: uuid,
    clinic_id: uuid,
    branch_id: uuid,
    inventory_item_id: uuid,
    quantity_delta: decimalType,
    reason: text,
    ref_type: text,
    ref_id: uuid,
    notes: text,
    created_by: uuid,
    created_at: dateType
  }
});
ensureIndex("inventory_transactions", { inventory_item_id: 1 }, { name: "idx_inventory_transactions_item" });
ensureIndex("inventory_transactions", { created_at: 1 }, { name: "idx_inventory_transactions_created" });

ensureCollection("orders", {
  bsonType: "object",
  required: [
    "_id",
    "organization_id",
    "clinic_id",
    "branch_id",
    "status",
    "ordered_at",
    "created_at",
    "updated_at"
  ],
  properties: {
    _id: uuid,
    organization_id: uuid,
    clinic_id: uuid,
    branch_id: uuid,
    patient_id: uuid,
    supplier_id: uuid,
    status: { bsonType: "string", enum: orderStatuses },
    ordered_at: dateType,
    expected_at: dateType,
    items: { bsonType: "array", items: orderItemSchema },
    status_history: { bsonType: "array", items: orderStatusSchema },
    created_at: dateType,
    updated_at: dateType
  }
});
