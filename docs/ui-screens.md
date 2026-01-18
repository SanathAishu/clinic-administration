# UI Screen Plan

## Web Portal (Admin/Reception/Doctor/Physio)
- **Login & Session**: Login, forgot password, 2FA (future), profile, change password.
- **Dashboard**: KPIs (appointments today, revenue, outstanding, no-show rate), quick actions (new patient, appointment, invoice).
- **Patients**: List/search (phone/name/status/location/date), create/edit, patient overview, timeline, allergies, diagnoses, medical history, documents.
- **Appointments**: Calendar (day/week/month), list with filters, create/reschedule modal, availability overlay, appointment detail (status changes, reminders).
- **Treatments**: Treatment catalog (types/rates/duration), patient treatments list, session logging with outcomes/notes, results view.
- **Prescriptions**: List, create/edit, approve, fulfill/adjust, patient-linked view.
- **Billing & Payments**: Invoice list (status filters), create/edit invoice, payment capture (method, partials), receipts, refund, discount apply/approve.
- **Discounts**: Discount requests/approvals list and detail.
- **Analytics/Reports**: Daily summary, revenue by location, payment mix, outstanding, staff performance, patient activity, appointment analytics.
- **Accounting**: Expenses list/create/edit, daily financials, P&L view.
- **Orders & Inventory**: Orders list/status/return/issue, order detail, inventory items list/edit, suppliers list/edit.
- **House Visits**: Schedule/list, route view (map), visit detail with location pings, attachments upload.
- **Staff**: Staff list/create/edit, availability calendar, leaves, roster view/generation, performance metrics.
- **Settings**: Notifications (channels/timing), billing (numbering, taxes), appointments (durations, buffers), lookup management (payment methods, statuses).
- **Audit Log**: Filterable table by user/action/resource/date.

## Mobile App (Doctor/Physio)
- **Login & Session**: Login, profile, change password.
- **Today View**: Appointments/house visits list with status and navigation.
- **Patient Quick Profile**: Demographics, key history, allergies, recent treatments.
- **Treatment Session Logging**: Quick form for notes, outcomes, next recommended date.
- **Prescriptions**: Create/adjust/approve (role-based).
- **House Visits**: Route list/map, start/complete visit, location ping, attachments (photo/doc upload), notes.
- **Notifications**: Inbox for reminders/alerts.
- **Offline Mode (basic)**: Cache assigned appointments/visits; queue updates.

## Role → Screen Mapping (summary)
- **Admin**: Dashboard, Patients, Appointments, Treatments, Prescriptions, Billing/Payments, Discounts, Reports/Analytics, Accounting, Orders/Inventory, House Visits, Staff, Settings, Audit Log, Lookups.
- **Reception**: Dashboard (limited KPIs), Patients, Appointments, Billing/Payments, Discounts (apply), Orders (basic), House Visits (schedule/view), Notifications, Reports (operational), Audit (view own actions).
- **Doctor**: Dashboard (clinical KPIs), Patients, Appointments, Treatments (log/view), Prescriptions (create/approve/adjust), House Visits, Notifications, Reports (clinical), Audit (view own actions).
- **Physiotherapist**: Dashboard (clinical KPIs), Patients, Appointments, Treatments (log/view), Prescriptions (if permitted), House Visits, Notifications, Reports (clinical), Audit (own actions).
- **Accounting/Finance**: Billing/Payments, Discounts (approve), Accounting, Reports/Analytics, Settings (billing/tax), Audit.
- **Inventory/Orders**: Orders & Inventory, Suppliers, Returns/Issues, Reports (inventory), Audit (own actions).

## Primary Actions per Key Screen
- **Dashboard**: Jump to today’s appointments/visits, quick create (patient/appointment/invoice), view KPIs.
- **Patients**: Search/filter, create/edit patient, view timeline, add history/allergy/diagnosis, upload docs.
- **Appointments**: Create/reschedule, change status (cancel/no-show/complete), view availability, send reminders.
- **Treatments**: Manage catalog, log treatment session, view outcomes, record next recommended date.
- **Prescriptions**: Create/approve/adjust, mark fulfill, view history per patient.
- **Billing & Payments**: Create/edit invoice, apply/approve discount, record payment/partial/refund, print/download receipt.
- **Discounts**: Submit/apply discount, approve/reject, view audit trail.
- **Accounting**: Add/edit expense, view daily financials, view P&L.
- **Orders & Inventory**: Create/update order, update status, return/issue items, adjust inventory, manage suppliers.
- **House Visits**: Schedule, assign, view route/map, start/complete visit, add notes/attachments, location ping.
- **Staff**: Add/edit staff, set availability, manage leaves, generate/edit roster, view performance.
- **Settings**: Configure notifications, billing numbering/taxes, appointment durations/buffers, manage lookups.
- **Audit Log**: Filter by user/action/resource/date, export.
