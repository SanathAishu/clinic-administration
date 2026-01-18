ensureCollection("settings", {
  bsonType: "object",
  required: ["_id", "scope", "section", "settings", "created_at", "updated_at"],
  properties: {
    _id: uuid,
    scope: { bsonType: "string", enum: ["system", "clinic"] },
    scope_id: uuid,
    section: text,
    settings: objectType,
    created_at: dateType,
    updated_at: dateType
  }
});
ensureIndex("settings", { scope: 1, scope_id: 1, section: 1 }, { unique: true, name: "uk_settings_scope" });

ensureCollection("audit_log", {
  bsonType: "object",
  required: ["_id", "action", "payload", "created_at"],
  properties: {
    _id: uuid,
    user_id: uuid,
    action: text,
    resource: text,
    payload: objectType,
    ip_address: text,
    user_agent: text,
    created_at: dateType
  }
});
ensureIndex("audit_log", { user_id: 1 }, { name: "idx_audit_log_user" });
ensureIndex("audit_log", { action: 1 }, { name: "idx_audit_log_action" });
