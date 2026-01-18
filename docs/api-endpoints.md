# Clinic Management API Endpoints (v1)

Base path: `/api/v1`. All endpoints require JWT except login/refresh. List endpoints support
`page`, `page_size`, `sort`, and `q` with module-specific filters. Date filters use ISO-8601.
`DELETE` is soft-delete unless explicitly stated.

## Authentication
- POST `/api/v1/auth/login`
- POST `/api/v1/auth/logout`
- POST `/api/v1/auth/refresh-token`
- GET `/api/v1/auth/me`
- POST `/api/v1/auth/change-password`

## Lookups & Reference Data
- GET, POST `/api/v1/lookup-values` (filters: lookup_type, active)
- GET, PUT, DELETE `/api/v1/lookup-values/{id}`

## Organizations & Locations
- GET, POST `/api/v1/organizations` (filters: active)
- GET, PUT, DELETE `/api/v1/organizations/{id}`
- GET, POST `/api/v1/clinics` (filters: organization_id, active)
- GET, PUT, DELETE `/api/v1/clinics/{id}`
- GET, POST `/api/v1/branches` (filters: organization_id, clinic_id, active)
- GET, PUT, DELETE `/api/v1/branches/{id}`
- GET, POST `/api/v1/departments` (filters: clinic_id, active)
- GET, PUT, DELETE `/api/v1/departments/{id}`

## Users & Roles
- GET, POST `/api/v1/roles`
- GET, PUT, DELETE `/api/v1/roles/{id}`
- GET, POST `/api/v1/permissions` (filters: organization_id, active, resource, action)
- GET, PUT, DELETE `/api/v1/permissions/{id}`
- GET, POST `/api/v1/users` (filters: status, role_code, clinic_id, branch_id)
- GET, PUT, DELETE `/api/v1/users/{id}`
- PUT `/api/v1/users/{id}/roles`
- POST `/api/v1/users/{id}/status` (activate, deactivate)
- GET `/api/v1/roles/{id}/permissions`
- PUT `/api/v1/roles/{id}/permissions` (replace full set)
- POST `/api/v1/roles/{id}/permissions` (add)
- DELETE `/api/v1/roles/{id}/permissions/{permissionId}`
- GET `/api/v1/users/{id}/permissions` (effective permissions)

## Staff
- GET, POST `/api/v1/staff` (filters: clinic_id, branch_id, staff_type, status)
- GET, PUT, DELETE `/api/v1/staff/{id}`
- GET, PUT `/api/v1/staff/{id}/availability`
- GET, POST `/api/v1/staff/{id}/leaves`
- GET, PUT, DELETE `/api/v1/staff/leaves/{leaveId}`
- GET, POST `/api/v1/rosters` (filters: branch_id, week_start, status)
- GET, PUT, DELETE `/api/v1/rosters/{id}`
- POST `/api/v1/rosters/generate`
- POST `/api/v1/rosters/{id}/publish`
- GET `/api/v1/staff/performance` (filters: start_date, end_date, clinic_id, staff_type)

## Patients & Clinical
- GET `/api/v1/patients` (filters: phone, name, status, clinic_id, branch_id, date_from, date_to)
- POST `/api/v1/patients`
- GET, PUT, DELETE `/api/v1/patients/{id}`
- GET `/api/v1/patients/duplicates` (filters: phone, name)
- GET `/api/v1/patients/{id}/timeline`
- GET, POST `/api/v1/patients/{id}/medical-history`
- GET, PUT, DELETE `/api/v1/patients/{id}/medical-history/{entryId}`
- GET, POST `/api/v1/patients/{id}/allergies`
- GET, PUT, DELETE `/api/v1/patients/{id}/allergies/{allergyId}`
- GET, POST `/api/v1/patients/{id}/diagnoses`
- GET, PUT, DELETE `/api/v1/diagnoses/{diagnosisId}`
- GET `/api/v1/patients/{id}/treatments`
- GET `/api/v1/patients/{id}/prescriptions`
- GET `/api/v1/patients/{id}/invoices`

## Appointments & Reminders
- GET, POST `/api/v1/appointments` (filters: status, scheduled_from, scheduled_to, clinic_id, branch_id, patient_id, provider_id)
- GET, PUT, DELETE `/api/v1/appointments/{id}`
- POST `/api/v1/appointments/{id}/status` (scheduled, in_progress, completed, cancelled, no_show)
- POST `/api/v1/appointments/{id}/reschedule`
- GET `/api/v1/appointments/available-slots` (filters: clinic_id, provider_id, date_from, date_to, duration)
- POST `/api/v1/appointments/optimize`
- GET `/api/v1/appointments/queue-stats` (filters: clinic_id, date_from, date_to)
- POST `/api/v1/appointments/{id}/reminders` (channels: sms, whatsapp, email)
- GET `/api/v1/reminders` (filters: status, channel, scheduled_from, scheduled_to)
- GET `/api/v1/reminders/{id}`
- POST `/api/v1/reminders/{id}/retry`

## Treatments & Packages
- GET, POST `/api/v1/treatment-types` (filters: clinic_id, active)
- GET, PUT, DELETE `/api/v1/treatment-types/{id}`
- GET, POST `/api/v1/treatments` (filters: patient_id, status, clinic_id, provider_id)
- GET, PUT, DELETE `/api/v1/treatments/{id}`
- GET, POST `/api/v1/treatments/{id}/sessions`
- PUT, DELETE `/api/v1/treatments/{id}/sessions/{sessionId}`
- GET `/api/v1/treatments/{id}/results`
- GET, POST `/api/v1/treatment-packages`
- GET, PUT, DELETE `/api/v1/treatment-packages/{id}`
- POST `/api/v1/treatment-packages/{id}/consume`
- POST `/api/v1/treatment-packages/{id}/status` (complete, cancel)

## Prescriptions
- GET, POST `/api/v1/prescriptions` (filters: patient_id, status, date_from, date_to)
- GET, PUT, DELETE `/api/v1/prescriptions/{id}`
- POST `/api/v1/prescriptions/{id}/fulfill`
- POST `/api/v1/prescriptions/{id}/adjust`
- POST `/api/v1/prescriptions/{id}/approve`
- POST `/api/v1/prescriptions/{id}/status` (cancel, complete)

## Billing & Payments
- GET, POST `/api/v1/invoices` (filters: patient_id, status, clinic_id, branch_id, date_from, date_to)
- GET, PUT, DELETE `/api/v1/invoices/{id}`
- POST `/api/v1/invoices/{id}/status` (issue, void)
- POST `/api/v1/invoices/{id}/payments`
- GET `/api/v1/invoices/{id}/payments`
- POST `/api/v1/invoices/{id}/refunds`
- GET `/api/v1/invoices/{id}/receipts`
- GET `/api/v1/invoices/next-number` (filters: clinic_id, branch_id, year, month)
- GET, POST `/api/v1/discounts` (filters: patient_id, status, date_from, date_to)
- GET, PUT, DELETE `/api/v1/discounts/{id}`
- POST `/api/v1/discounts/{id}/approve`
- GET `/api/v1/discounts/history`

## Accounting
- GET, POST `/api/v1/expenses` (filters: clinic_id, branch_id, date_from, date_to)
- GET, PUT, DELETE `/api/v1/expenses/{id}`
- GET, POST `/api/v1/revenue-ledger` (filters: clinic_id, branch_id, date_from, date_to)
- GET `/api/v1/financial-summary` (filters: clinic_id, branch_id, date_from, date_to)
- GET `/api/v1/accounting/daily-summary` (filters: clinic_id, branch_id, date_from, date_to)
- GET `/api/v1/accounting/profit-loss` (filters: clinic_id, branch_id, date_from, date_to)

## Orders & Inventory
- GET, POST `/api/v1/suppliers` (filters: active)
- GET, PUT, DELETE `/api/v1/suppliers/{id}`
- GET, POST `/api/v1/inventory/items` (filters: clinic_id, branch_id, active)
- GET, PUT, DELETE `/api/v1/inventory/items/{id}`
- GET, POST `/api/v1/inventory/transactions` (filters: inventory_item_id, date_from, date_to)
- GET `/api/v1/inventory/levels` (filters: clinic_id, branch_id, below_reorder)
- GET, POST `/api/v1/orders` (filters: status, supplier_id, patient_id, date_from, date_to)
- GET, PUT, DELETE `/api/v1/orders/{id}`
- POST `/api/v1/orders/{id}/status`
- POST `/api/v1/orders/{id}/receive`
- POST `/api/v1/orders/{id}/issue`
- POST `/api/v1/orders/{id}/return`

## House Visits
- GET, POST `/api/v1/house-visits` (filters: status, provider_id, scheduled_from, scheduled_to)
- GET, PUT, DELETE `/api/v1/house-visits/{id}`
- POST `/api/v1/house-visits/{id}/status`
- POST `/api/v1/house-visits/{id}/events`
- POST `/api/v1/house-visits/{id}/location`
- POST `/api/v1/house-visits/{id}/attachments`
- GET `/api/v1/house-visits/routes` (filters: date, provider_id)
- POST `/api/v1/house-visits/routes/optimize`

## Settings
- GET `/api/v1/settings` (filters: scope, scope_id)
- GET, PUT `/api/v1/settings/{section}`

## Audit & Logs
- GET `/api/v1/audit` (filters: user_id, action, resource, date_from, date_to)

## Reports
- GET `/api/v1/reports/daily-summary` (filters: clinic_id, branch_id, date_from, date_to)
- GET `/api/v1/reports/revenue-analysis` (filters: clinic_id, branch_id, date_from, date_to, group_by)
- GET `/api/v1/reports/payment-mix` (filters: clinic_id, branch_id, date_from, date_to)
- GET `/api/v1/reports/billing-outstanding` (filters: clinic_id, branch_id, as_of)
- GET `/api/v1/reports/staff-performance` (filters: clinic_id, staff_type, date_from, date_to)
- GET `/api/v1/reports/patient-activity` (filters: clinic_id, branch_id, date_from, date_to)
- GET `/api/v1/reports/appointment-analytics` (filters: clinic_id, provider_id, date_from, date_to)
- GET `/api/v1/reports/treatment-outcomes` (filters: clinic_id, treatment_type_id, date_from, date_to)
- GET `/api/v1/reports/inventory-levels` (filters: clinic_id, branch_id, below_reorder)
- GET `/api/v1/reports/order-status` (filters: status, supplier_id, date_from, date_to)
- GET `/api/v1/reports/house-visit-kpis` (filters: clinic_id, provider_id, date_from, date_to)

## Notes
- Schema definitions live in `docs/db/mongodb_schema/*.js`.
- Aggregation/report patterns live in `docs/mongodb-functions.md`.
