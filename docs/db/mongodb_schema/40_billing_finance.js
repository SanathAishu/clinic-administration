ensureCollection("invoices", {
  bsonType: "object",
  required: [
    "_id",
    "organization_id",
    "clinic_id",
    "branch_id",
    "patient_id",
    "invoice_number",
    "status",
    "issue_date",
    "subtotal",
    "discount_total",
    "tax_total",
    "total",
    "balance",
    "created_at",
    "updated_at"
  ],
  properties: {
    _id: uuid,
    organization_id: uuid,
    clinic_id: uuid,
    branch_id: uuid,
    patient_id: uuid,
    invoice_number: text,
    status: { bsonType: "string", enum: invoiceStatuses },
    issue_date: dateType,
    due_date: dateType,
    subtotal: decimalType,
    discount_total: decimalType,
    tax_total: decimalType,
    total: decimalType,
    balance: decimalType,
    items: { bsonType: "array", items: invoiceItemSchema },
    discounts: { bsonType: "array", items: discountSchema },
    payments: { bsonType: "array", items: paymentSchema },
    created_at: dateType,
    updated_at: dateType
  }
});
ensureIndex("invoices", { patient_id: 1 }, { name: "idx_invoices_patient" });
ensureIndex("invoices", { clinic_id: 1, invoice_number: 1 }, { unique: true, name: "uk_invoices_clinic_number" });

ensureCollection("invoice_sequences", {
  bsonType: "object",
  required: [
    "_id",
    "organization_id",
    "clinic_id",
    "branch_id",
    "year",
    "month",
    "current_value",
    "updated_at"
  ],
  properties: {
    _id: uuid,
    organization_id: uuid,
    clinic_id: uuid,
    branch_id: uuid,
    year: intOrLong,
    month: { bsonType: "int", minimum: 1, maximum: 12 },
    current_value: intOrLong,
    updated_at: dateType
  }
});
ensureIndex(
  "invoice_sequences",
  { organization_id: 1, clinic_id: 1, branch_id: 1, year: 1, month: 1 },
  { unique: true, name: "uk_invoice_sequences" }
);

ensureCollection("expenses", {
  bsonType: "object",
  required: [
    "_id",
    "organization_id",
    "clinic_id",
    "branch_id",
    "category",
    "amount",
    "expense_date",
    "created_at",
    "updated_at"
  ],
  properties: {
    _id: uuid,
    organization_id: uuid,
    clinic_id: uuid,
    branch_id: uuid,
    category: text,
    amount: { bsonType: ["decimal", "double", "int", "long"], minimum: 0 },
    expense_date: dateType,
    notes: text,
    created_by: uuid,
    created_at: dateType,
    updated_at: dateType
  }
});
ensureIndex("expenses", { branch_id: 1, expense_date: 1 }, { name: "idx_expenses_branch_date" });

ensureCollection("revenue_ledger", {
  bsonType: "object",
  required: [
    "_id",
    "organization_id",
    "clinic_id",
    "branch_id",
    "source_type",
    "amount",
    "entry_date",
    "created_at"
  ],
  properties: {
    _id: uuid,
    organization_id: uuid,
    clinic_id: uuid,
    branch_id: uuid,
    source_type: { bsonType: "string", enum: revenueSources },
    source_id: uuid,
    invoice_id: uuid,
    payment_id: uuid,
    amount: decimalType,
    entry_date: dateType,
    description: text,
    created_at: dateType
  }
});
ensureIndex("revenue_ledger", { branch_id: 1, entry_date: 1 }, { name: "idx_revenue_ledger_branch_date" });

ensureCollection("financial_summary", {
  bsonType: "object",
  required: [
    "_id",
    "organization_id",
    "clinic_id",
    "branch_id",
    "summary_date",
    "revenue_total",
    "expense_total",
    "profit_loss",
    "generated_at"
  ],
  properties: {
    _id: uuid,
    organization_id: uuid,
    clinic_id: uuid,
    branch_id: uuid,
    summary_date: dateType,
    revenue_total: decimalType,
    expense_total: decimalType,
    profit_loss: decimalType,
    generated_at: dateType
  }
});
ensureIndex("financial_summary", { branch_id: 1, summary_date: 1 }, { unique: true, name: "uk_financial_summary_branch_date" });
