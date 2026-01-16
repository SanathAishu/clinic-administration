# REST API Reference

Complete API documentation for the Clinic Management System.

**Base URL:** `http://localhost:8080/api`
**Swagger UI:** `http://localhost:8080/swagger-ui.html`
**OpenAPI Spec:** `http://localhost:8080/v3/api-docs`

---

## Table of Contents

1. [Authentication](#1-authentication)
2. [Patients](#2-patients)
3. [Appointments](#3-appointments)
4. [Users](#4-users)
5. [Roles](#5-roles)
6. [Billing](#6-billing)
7. [Prescriptions](#7-prescriptions)
8. [Queue Management](#8-queue-management)
9. [Inventory Optimization](#9-inventory-optimization)
10. [Branches](#10-branches)
11. [Treatments](#11-treatments)
12. [Medical Orders](#12-medical-orders)
13. [Compliance Dashboard](#13-compliance-dashboard)
14. [Data Retention](#14-data-retention)
15. [Access Audit](#15-access-audit)
16. [Admin - Materialized Views](#16-admin---materialized-views)

---

## Authentication

All endpoints (except `/api/auth/*`) require a valid JWT token in the `Authorization` header:

```
Authorization: Bearer <jwt_token>
```

### Token Lifecycle

| Token Type | Duration | Purpose |
|------------|----------|---------|
| Access Token | 15 minutes | API authentication |
| Refresh Token | 7 days | Obtain new access tokens |

---

## 1. Authentication

**Base Path:** `/api/auth`

Stateless JWT authentication with refresh token support.

### Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/login` | User login | No |
| POST | `/refresh` | Refresh access token | No (refresh token in body) |
| POST | `/logout` | Logout (invalidate session) | Yes |
| GET | `/me` | Get current user profile | Yes |

### POST /api/auth/login

Authenticate user and receive JWT tokens.

**Request Body:**
```json
{
  "email": "doctor@clinic.com",
  "password": "SecurePassword123!"
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "doctor@clinic.com",
    "firstName": "Dr. Ramesh",
    "lastName": "Kumar",
    "roles": ["DOCTOR"]
  }
}
```

**Error Responses:**
- `401 Unauthorized` - Invalid credentials
- `423 Locked` - Account locked after failed attempts

### POST /api/auth/refresh

Get new access token using refresh token.

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

### POST /api/auth/logout

Invalidate current session.

**Headers:** `Authorization: Bearer <access_token>`

**Response:** `204 No Content`

### GET /api/auth/me

Get authenticated user's profile.

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "doctor@clinic.com",
  "firstName": "Dr. Ramesh",
  "lastName": "Kumar",
  "tenantId": "tenant-uuid",
  "roles": ["DOCTOR"],
  "permissions": ["PATIENT_READ", "PRESCRIPTION_WRITE"]
}
```

---

## 2. Patients

**Base Path:** `/api/patients`

Patient demographics, medical history, and ABHA integration.

### Endpoints

| Method | Endpoint | Description | Required Role |
|--------|----------|-------------|---------------|
| GET | `/` | List all patients (paginated) | PATIENT_READ |
| GET | `/{id}` | Get patient by ID | PATIENT_READ |
| GET | `/search` | Search patients | PATIENT_READ |
| GET | `/abha/{abhaId}` | Find patient by ABHA ID | PATIENT_READ |
| POST | `/` | Create new patient | PATIENT_WRITE |
| PUT | `/{id}` | Update patient | PATIENT_WRITE |
| DELETE | `/{id}` | Soft delete patient | ADMIN |

### GET /api/patients

List patients with pagination and filtering.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| page | int | Page number (default: 0) |
| size | int | Page size (default: 20) |
| sort | string | Sort field (e.g., `lastName,asc`) |

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": "patient-uuid",
      "firstName": "Priya",
      "lastName": "Sharma",
      "dateOfBirth": "1990-05-15",
      "gender": "FEMALE",
      "phoneNumber": "+91-98765-43210",
      "email": "priya.sharma@email.com",
      "abhaId": "12-3456-7890-1234",
      "bloodGroup": "B+",
      "emergencyContact": "+91-98765-43211",
      "createdAt": "2025-01-10T10:30:00Z"
    }
  ],
  "totalElements": 150,
  "totalPages": 8,
  "size": 20,
  "number": 0
}
```

### GET /api/patients/search

Search patients by name, phone, or email.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| q | string | Search term (required) |
| page | int | Page number |
| size | int | Page size |

**Example:** `GET /api/patients/search?q=priya&page=0&size=10`

### POST /api/patients

Create a new patient record.

**Request Body:**
```json
{
  "firstName": "Priya",
  "lastName": "Sharma",
  "dateOfBirth": "1990-05-15",
  "gender": "FEMALE",
  "phoneNumber": "+91-98765-43210",
  "email": "priya.sharma@email.com",
  "abhaId": "12-3456-7890-1234",
  "bloodGroup": "B+",
  "address": {
    "street": "123 Anna Nagar",
    "city": "Chennai",
    "state": "Tamil Nadu",
    "pincode": "600040"
  },
  "emergencyContact": "+91-98765-43211",
  "emergencyContactName": "Raj Sharma",
  "emergencyContactRelation": "Spouse"
}
```

**Response:** `201 Created` with patient object

---

## 3. Appointments

**Base Path:** `/api/appointments`

Appointment scheduling with token-based queue management.

### Endpoints

| Method | Endpoint | Description | Required Role |
|--------|----------|-------------|---------------|
| GET | `/` | List appointments | APPOINTMENT_READ |
| GET | `/{id}` | Get appointment by ID | APPOINTMENT_READ |
| GET | `/patient/{patientId}` | Get patient's appointments | APPOINTMENT_READ |
| GET | `/doctor/{doctorId}` | Get doctor's appointments | APPOINTMENT_READ |
| GET | `/date/{date}` | Get appointments by date | APPOINTMENT_READ |
| GET | `/status/{status}` | Filter by status | APPOINTMENT_READ |
| POST | `/` | Create appointment | APPOINTMENT_WRITE |
| PUT | `/{id}` | Update appointment | APPOINTMENT_WRITE |
| PATCH | `/{id}/status` | Update status | APPOINTMENT_WRITE |
| PATCH | `/{id}/check-in` | Patient check-in | RECEPTIONIST |
| PATCH | `/{id}/complete` | Mark as completed | DOCTOR |
| DELETE | `/{id}` | Cancel appointment | APPOINTMENT_WRITE |

### Appointment Status Flow

```
SCHEDULED --> CHECKED_IN --> IN_PROGRESS --> COMPLETED
          \-> CANCELLED   \-> NO_SHOW
```

### POST /api/appointments

Create a new appointment with automatic token generation.

**Request Body:**
```json
{
  "patientId": "patient-uuid",
  "doctorId": "doctor-uuid",
  "scheduledDate": "2025-01-20",
  "scheduledTime": "10:30:00",
  "appointmentType": "CONSULTATION",
  "reason": "Follow-up for diabetes management",
  "notes": "Patient requested morning slot"
}
```

**Response (201 Created):**
```json
{
  "id": "appointment-uuid",
  "patientId": "patient-uuid",
  "doctorId": "doctor-uuid",
  "tokenNumber": 15,
  "scheduledDate": "2025-01-20",
  "scheduledTime": "10:30:00",
  "status": "SCHEDULED",
  "estimatedWaitTime": 25,
  "createdAt": "2025-01-15T14:30:00Z"
}
```

### PATCH /api/appointments/{id}/check-in

Mark patient as checked in (updates queue position).

**Response (200 OK):**
```json
{
  "id": "appointment-uuid",
  "status": "CHECKED_IN",
  "checkedInAt": "2025-01-20T10:15:00Z",
  "queuePosition": 3,
  "estimatedWaitTime": 15
}
```

---

## 4. Users

**Base Path:** `/api/users`

Staff user management (doctors, nurses, admin, receptionists).

### Endpoints

| Method | Endpoint | Description | Required Role |
|--------|----------|-------------|---------------|
| GET | `/` | List all users | USER_READ |
| GET | `/{id}` | Get user by ID | USER_READ |
| GET | `/search` | Search users | USER_READ |
| GET | `/role/{roleName}` | Get users by role | USER_READ |
| GET | `/doctors` | List all doctors | USER_READ |
| GET | `/available-doctors` | Doctors available today | USER_READ |
| POST | `/` | Create user | ADMIN |
| PUT | `/{id}` | Update user | ADMIN |
| PATCH | `/{id}/activate` | Activate user | ADMIN |
| PATCH | `/{id}/deactivate` | Deactivate user | ADMIN |
| DELETE | `/{id}` | Soft delete user | ADMIN |

### POST /api/users

Create a new staff user.

**Request Body:**
```json
{
  "email": "dr.kumar@clinic.com",
  "password": "SecurePassword123!",
  "firstName": "Ramesh",
  "lastName": "Kumar",
  "phoneNumber": "+91-98765-12345",
  "roleIds": ["doctor-role-uuid"],
  "specialization": "General Medicine",
  "licenseNumber": "MCI-12345",
  "consultationFee": 500.00
}
```

**Response:** `201 Created` with user object (password excluded)

---

## 5. Roles

**Base Path:** `/api/roles`

Role-Based Access Control (RBAC) management.

### Endpoints

| Method | Endpoint | Description | Required Role |
|--------|----------|-------------|---------------|
| GET | `/` | List all roles | ROLE_READ |
| GET | `/{id}` | Get role details | ROLE_READ |
| GET | `/system` | List system roles | ROLE_READ |
| GET | `/tenant` | List tenant-specific roles | ROLE_READ |
| GET | `/search` | Search roles | ROLE_READ |
| POST | `/` | Create role | ADMIN |
| PUT | `/{id}` | Update role | ADMIN |
| DELETE | `/{id}` | Delete role | ADMIN |
| POST | `/{id}/permissions` | Assign permissions | ADMIN |
| PUT | `/{id}/permissions` | Replace permissions | ADMIN |
| DELETE | `/{id}/permissions/{permId}` | Remove permission | ADMIN |

### System Roles (Pre-defined)

| Role | Description |
|------|-------------|
| ADMIN | Full system access |
| DOCTOR | Clinical operations |
| NURSE | Patient care support |
| RECEPTIONIST | Front desk operations |
| PHARMACIST | Prescription dispensing |
| LAB_TECHNICIAN | Lab test management |
| BILLING_STAFF | Financial operations |
| COMPLIANCE_OFFICER | Audit and compliance |

### POST /api/roles

Create a custom role.

**Request Body:**
```json
{
  "name": "SENIOR_DOCTOR",
  "description": "Senior doctor with extended permissions",
  "permissionIds": [
    "perm-uuid-1",
    "perm-uuid-2"
  ]
}
```

---

## 6. Billing

**Base Path:** `/api/billing`

Invoicing, payments, and financial tracking.

### Endpoints

| Method | Endpoint | Description | Required Role |
|--------|----------|-------------|---------------|
| GET | `/` | List invoices | BILLING_READ |
| GET | `/{id}` | Get invoice by ID | BILLING_READ |
| GET | `/patient/{patientId}` | Patient's invoices | BILLING_READ |
| GET | `/pending` | Unpaid invoices | BILLING_READ |
| GET | `/overdue` | Overdue invoices | BILLING_READ |
| GET | `/date-range` | Invoices by date range | BILLING_READ |
| POST | `/` | Create invoice | BILLING_WRITE |
| PUT | `/{id}` | Update invoice | BILLING_WRITE |
| POST | `/{id}/payment` | Record payment | BILLING_WRITE |
| POST | `/{id}/refund` | Process refund | ADMIN |

### Invoice Status Flow

```
DRAFT --> SENT --> PARTIALLY_PAID --> PAID
              \-> OVERDUE --> PAID
              \-> CANCELLED
```

### POST /api/billing

Create a new invoice.

**Request Body:**
```json
{
  "patientId": "patient-uuid",
  "appointmentId": "appointment-uuid",
  "items": [
    {
      "description": "Consultation - General Medicine",
      "quantity": 1,
      "unitPrice": 500.00,
      "taxRate": 18.0
    },
    {
      "description": "Blood Test - Complete Blood Count",
      "quantity": 1,
      "unitPrice": 350.00,
      "taxRate": 18.0
    }
  ],
  "discountPercent": 10.0,
  "notes": "Insurance claim submitted"
}
```

**Response (201 Created):**
```json
{
  "id": "invoice-uuid",
  "invoiceNumber": "INV-2025-00123",
  "patientId": "patient-uuid",
  "subtotal": 850.00,
  "discount": 85.00,
  "tax": 137.70,
  "totalAmount": 902.70,
  "amountPaid": 0.00,
  "balanceDue": 902.70,
  "status": "DRAFT",
  "createdAt": "2025-01-15T14:30:00Z"
}
```

### POST /api/billing/{id}/payment

Record a payment against an invoice.

**Request Body:**
```json
{
  "amount": 500.00,
  "paymentMethod": "UPI",
  "transactionId": "UPI-123456789",
  "notes": "Partial payment"
}
```

---

## 7. Prescriptions

**Base Path:** `/api/prescriptions`

Prescription management with drug interaction checking and inventory integration.

### Endpoints

| Method | Endpoint | Description | Required Role |
|--------|----------|-------------|---------------|
| GET | `/{id}` | Get prescription | PRESCRIPTION_READ |
| GET | `/status/{status}` | Filter by status | PRESCRIPTION_READ |
| POST | `/{id}/dispense` | Dispense prescription | PHARMACIST |
| POST | `/{id}/complete` | Mark as completed | PHARMACIST |
| POST | `/{id}/cancel` | Cancel prescription | DOCTOR, ADMIN |
| POST | `/{id}/refill` | Create refill | DOCTOR |

### Prescription Status Flow

```
PENDING --> DISPENSED --> COMPLETED
        \-> CANCELLED

Refill: COMPLETED --> new PENDING prescription
```

### POST /api/prescriptions/{id}/dispense

Dispense prescription with atomic inventory deduction.

**Mathematical Foundation:** Theorem 12 - Atomic Inventory Deduction
- Either prescription is marked DISPENSED AND inventory is reduced
- Or neither happens (atomic transaction with rollback)

**Request Body:**
```json
{
  "dispensedBy": "pharmacist-user-uuid"
}
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| tenantId | UUID | Tenant ID (required) |

**Response (200 OK):**
```json
{
  "prescriptionId": "prescription-uuid",
  "status": "DISPENSED",
  "dispensedAt": "2025-01-15T15:30:00Z",
  "dispensedBy": "pharmacist-user-uuid",
  "totalItemsDispensed": 3,
  "itemsDispensed": [
    {
      "medicationName": "Metformin 500mg",
      "medicationCode": "MET-500",
      "prescribedQuantity": 30,
      "dispensedQuantity": 30,
      "stockBefore": 500,
      "stockAfter": 470
    }
  ],
  "inventoryTransactionsCreated": 3,
  "interactions": [],
  "success": true
}
```

**Error Responses:**
- `409 Conflict` - Insufficient stock or drug interaction detected
- `400 Bad Request` - Invalid state transition

### POST /api/prescriptions/{id}/refill

Create a refill prescription (copy of original with new PENDING status).

**Constraint:** `timesFilled < (allowedRefills + 1)`

**Response:** `201 Created` with new prescription object

---

## 8. Queue Management

**Base Path:** `/api/queue`

Real-time queue status using M/M/1 queuing theory.

### Mathematical Foundation

**Utilization:** `rho = lambda / mu` (must be < 1 for stability)

**Average Wait Time:** `W = 1 / (mu - lambda)`

**Little's Law:** `L = lambda x W`

### Endpoints

| Method | Endpoint | Description | Required Role |
|--------|----------|-------------|---------------|
| GET | `/status/{doctorId}` | Current queue status | ADMIN, DOCTOR, RECEPTIONIST |
| GET | `/wait-time/{appointmentId}` | Estimated wait time | PATIENT, ADMIN, DOCTOR |
| GET | `/position/{appointmentId}` | Queue position | PATIENT, ADMIN, DOCTOR |
| GET | `/metrics/{doctorId}` | Queue analytics | ADMIN, DOCTOR |
| POST | `/metrics/calculate` | Trigger metrics calculation | ADMIN |
| POST | `/token/generate` | Generate token number | RECEPTIONIST |

### GET /api/queue/status/{doctorId}

Get real-time queue status for clinic display boards.

**Response (200 OK):**
```json
{
  "doctorId": "doctor-uuid",
  "doctorName": "Dr. Ramesh Kumar",
  "currentToken": 12,
  "nextToken": 13,
  "patientsWaiting": 5,
  "averageWaitTimeMinutes": 15,
  "utilization": 0.78,
  "isStable": true,
  "arrivalRate": 4.5,
  "serviceRate": 6.0,
  "lastUpdated": "2025-01-15T10:30:00Z"
}
```

### GET /api/queue/wait-time/{appointmentId}

Get estimated wait time using M/M/1 formula.

**Response (200 OK):**
```json
{
  "appointmentId": "appointment-uuid",
  "estimatedWaitMinutes": 18,
  "confidenceLevel": "HIGH",
  "queuePosition": 3,
  "utilization": 0.75,
  "arrivalRate": 4.5,
  "serviceRate": 6.0,
  "calculatedAt": "2025-01-15T10:30:00Z"
}
```

### GET /api/queue/metrics/{doctorId}

Get detailed queue metrics for analysis.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| date | LocalDate | Date for metrics (required, format: yyyy-MM-dd) |

**Response (200 OK):**
```json
{
  "doctorId": "doctor-uuid",
  "metricDate": "2025-01-15",
  "totalAppointments": 24,
  "completedAppointments": 20,
  "noShows": 2,
  "cancellations": 2,
  "arrivalRate": 4.5,
  "serviceRate": 6.0,
  "utilization": 0.75,
  "averageWaitTimeMinutes": 12.5,
  "maxWaitTimeMinutes": 35,
  "averageQueueLength": 3.2,
  "isStable": true
}
```

---

## 9. Inventory Optimization

**Base Path:** `/api/v1/tenants/{tenantId}/inventory/optimization`

Operations Research-based inventory management.

### Mathematical Foundations

**EOQ Formula:** `Q* = sqrt(2DS/H)`
- D = Annual demand
- S = Ordering cost per order
- H = Holding cost per unit per year

**Reorder Point:** `ROP = (d x L) + SS`
- d = Average daily demand
- L = Lead time in days
- SS = Safety stock (z x sigma x sqrt(L))

**ABC Classification:**
- A items: Top 70% of value (~20% of items)
- B items: Next 20% of value (~30% of items)
- C items: Bottom 10% of value (~50% of items)

### Endpoints

| Method | Endpoint | Description | Required Role |
|--------|----------|-------------|---------------|
| GET | `/eoq/{itemId}` | Calculate EOQ | ADMIN, STAFF |
| GET | `/rop/{itemId}` | Calculate Reorder Point | ADMIN, STAFF |
| POST | `/abc-analysis` | Trigger ABC classification | ADMIN |
| GET | `/abc/{classification}` | Get items by ABC class | ADMIN, STAFF |
| GET | `/reorder-needed` | Items below ROP | ADMIN, STAFF |
| GET | `/analytics/{itemId}` | Item demand analytics | ADMIN, STAFF |
| GET | `/analytics/{itemId}/history` | Historical analytics | ADMIN, STAFF |

### GET /eoq/{itemId}

Calculate Economic Order Quantity for an item.

**Response (200 OK):**
```json
{
  "itemId": "item-uuid",
  "itemName": "Paracetamol 500mg",
  "annualDemand": 12000,
  "orderingCost": 150.00,
  "holdingCost": 2.50,
  "economicOrderQuantity": 1200,
  "ordersPerYear": 10,
  "averageInventory": 600,
  "annualOrderingCost": 1500.00,
  "annualHoldingCost": 1500.00,
  "totalInventoryCost": 3000.00,
  "currentStock": 450,
  "recommendedOrderFrequencyDays": 36.5
}
```

### GET /rop/{itemId}

Calculate Reorder Point with safety stock.

**Response (200 OK):**
```json
{
  "itemId": "item-uuid",
  "itemName": "Paracetamol 500mg",
  "annualDemand": 12000,
  "averageDailyDemand": 32.88,
  "leadTimeDays": 7,
  "demandStdDev": 8.5,
  "serviceLevel": 0.95,
  "zScore": 1.645,
  "leadTimeDemand": 230,
  "safetyStock": 37,
  "reorderPoint": 267,
  "currentStock": 250,
  "unitsBelowROP": -17,
  "reorderNeeded": true
}
```

### GET /abc/{classification}

Get items by ABC classification.

**Path Parameters:**
| Parameter | Type | Values |
|-----------|------|--------|
| classification | string | A, B, C |

**Response (200 OK):**
```json
{
  "content": [
    {
      "itemId": "item-uuid",
      "itemName": "Insulin Glargine",
      "annualDemand": 500,
      "unitPrice": 2500.00,
      "annualValue": 1250000.00,
      "classification": "A",
      "recommendedControlStrategy": "Tight control, daily review, exact records",
      "recommendedReviewFrequency": "Daily",
      "recommendedServiceLevel": 0.99
    }
  ],
  "totalElements": 45,
  "totalPages": 3
}
```

---

## 10. Branches

**Base Path:** `/api/v1/tenants/{tenantId}/branches`

Multi-location clinic branch management.

### Endpoints

| Method | Endpoint | Description | Required Role |
|--------|----------|-------------|---------------|
| GET | `/` | List all branches | Any |
| GET | `/{branchId}` | Get branch by ID | Any |
| GET | `/code/{code}` | Get branch by code | Any |
| GET | `/active` | List active branches | Any |
| GET | `/main` | Get main branch | Any |
| GET | `/search` | Search branches | Any |
| GET | `/count` | Count total branches | Any |
| GET | `/count/active` | Count active branches | Any |
| POST | `/` | Create branch | ADMIN |
| PUT | `/{branchId}` | Update branch | ADMIN |
| PATCH | `/{branchId}/activate` | Activate branch | ADMIN |
| PATCH | `/{branchId}/deactivate` | Deactivate branch | ADMIN |
| DELETE | `/{branchId}` | Soft delete branch | ADMIN |

### POST /branches

Create a new branch.

**Request Body:**
```json
{
  "branchCode": "CBE-MAIN",
  "branchName": "Coimbatore Main Clinic",
  "address": "123 Race Course Road",
  "city": "Coimbatore",
  "state": "Tamil Nadu",
  "pincode": "641018",
  "phoneNumber": "+91-422-2345678",
  "email": "coimbatore@clinic.com",
  "isMainBranch": true
}
```

---

## 11. Treatments

**Base Path:** `/api/v1/tenants/{tenantId}/treatments`

Treatment catalog and pricing management.

### Endpoints

| Method | Endpoint | Description | Required Role |
|--------|----------|-------------|---------------|
| GET | `/` | List all treatments | Any |
| GET | `/{treatmentId}` | Get treatment by ID | Any |
| GET | `/search` | Search treatments | Any |
| GET | `/category/{category}` | Get by category | Any |
| GET | `/count` | Count treatments | Any |
| POST | `/` | Create treatment | ADMIN, DOCTOR |
| PUT | `/{treatmentId}` | Update treatment | ADMIN, DOCTOR |
| PATCH | `/{treatmentId}/activate` | Activate | ADMIN |
| PATCH | `/{treatmentId}/deactivate` | Deactivate | ADMIN |
| DELETE | `/{treatmentId}` | Soft delete | ADMIN |

### POST /treatments

Create a new treatment.

**Request Body:**
```json
{
  "treatmentCode": "CONS-GEN",
  "treatmentName": "General Consultation",
  "category": "CONSULTATION",
  "description": "General medical consultation with specialist",
  "durationMinutes": 15,
  "basePrice": 500.00,
  "taxRate": 18.0
}
```

---

## 12. Medical Orders

**Base Path:** `/api/v1/tenants/{tenantId}/orders`

Medical equipment and supply orders (lab orders, imaging, etc.).

### Order Status Flow

```
DRAFT --> SENT --> IN_PRODUCTION --> SHIPPED --> RECEIVED --> READY_FOR_PICKUP --> DELIVERED
                                                                               \-> CANCELLED
```

### Endpoints

| Method | Endpoint | Description | Required Role |
|--------|----------|-------------|---------------|
| GET | `/` | List all orders | Any |
| GET | `/{orderId}` | Get order by ID | Any |
| GET | `/patient/{patientId}` | Patient's orders | Any |
| GET | `/status/{status}` | Filter by status | Any |
| GET | `/search/product` | Search by product | Any |
| GET | `/search/patient` | Search by patient | Any |
| GET | `/ready-for-pickup` | Orders ready | Any |
| GET | `/overdue` | Overdue orders | Any |
| GET | `/count` | Count orders | Any |
| GET | `/count/status/{status}` | Count by status | Any |
| POST | `/` | Create order | ADMIN, DOCTOR |
| PUT | `/{orderId}` | Update order | ADMIN, DOCTOR |
| PATCH | `/{orderId}/send` | Send order | ADMIN, DOCTOR |
| PATCH | `/{orderId}/in-production` | Mark in production | ADMIN, DOCTOR |
| PATCH | `/{orderId}/shipped` | Mark shipped | ADMIN, DOCTOR |
| PATCH | `/{orderId}/received` | Mark received | ADMIN, DOCTOR |
| PATCH | `/{orderId}/ready-for-pickup` | Mark ready | ADMIN, DOCTOR |
| PATCH | `/{orderId}/delivered` | Mark delivered | ADMIN, DOCTOR |
| PATCH | `/{orderId}/cancel` | Cancel order | ADMIN, DOCTOR |
| PATCH | `/{orderId}/notify-patient` | Mark notified | ADMIN, DOCTOR |
| DELETE | `/{orderId}` | Soft delete | ADMIN |

### POST /orders

Create a medical order.

**Request Body:**
```json
{
  "patientId": "patient-uuid",
  "productName": "Custom Orthotics - Left Foot",
  "productCode": "ORTH-LEFT-001",
  "quantity": 1,
  "unitPrice": 3500.00,
  "supplierName": "MedSupply Corp",
  "notes": "Patient requires arch support modification",
  "expectedDeliveryDate": "2025-01-30"
}
```

---

## 13. Compliance Dashboard

**Base Path:** `/api/compliance`

ISO 27001 A.18 compliance monitoring with Statistical Process Control (SPC).

### Mathematical Foundation: 3-Sigma Rule

- **Center Line (CL):** mu (mean)
- **Upper Control Limit (UCL):** mu + 3*sigma
- **Lower Control Limit (LCL):** mu - 3*sigma

Any metric outside these limits is flagged as "out of control" (statistically significant deviation).

### Endpoints

| Method | Endpoint | Description | Required Role |
|--------|----------|-------------|---------------|
| GET | `/dashboard` | Compliance summary | ADMIN, COMPLIANCE_OFFICER |
| GET | `/violations` | Recent violations | ADMIN, COMPLIANCE_OFFICER |
| GET | `/metrics/{type}` | Metric history | ADMIN, COMPLIANCE_OFFICER |

### Compliance Metric Types

| Type | Description |
|------|-------------|
| QUEUE_STABILITY | M/M/1 queue stability (lambda < mu) |
| WAIT_TIME_SLA | Average wait time < threshold |
| CACHE_HIT_RATE | Cache effectiveness |
| ERROR_RATE | API error rate |
| ACCESS_LOG_COVERAGE | % sensitive operations logged |
| DATA_RETENTION_COMPLIANCE | % records within retention policy |
| CONSENT_VALIDITY | % active consents not expired |

### GET /api/compliance/dashboard

Get compliance dashboard summary.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| days | int | Days to analyze (default: 30) |

**Response (200 OK):**
```json
{
  "daysAnalyzed": 30,
  "averageComplianceRate": 97.5,
  "totalViolations": 150,
  "violationsDays": 3,
  "metricsSummary": {
    "QUEUE_STABILITY": {
      "metricType": "QUEUE_STABILITY",
      "currentRate": 98.5,
      "averageRate": 97.2,
      "minRate": 85.0,
      "violations": 1,
      "outOfControl": false
    },
    "WAIT_TIME_SLA": {
      "metricType": "WAIT_TIME_SLA",
      "currentRate": 95.0,
      "averageRate": 96.8,
      "minRate": 88.0,
      "violations": 2,
      "outOfControl": false
    }
  },
  "recentViolations": [
    {
      "violationId": "violation-uuid",
      "metricType": "WAIT_TIME_SLA",
      "violationDate": "2025-01-14",
      "complianceRate": 88.0,
      "upperControlLimit": 100.0,
      "lowerControlLimit": 90.0,
      "severity": "HIGH"
    }
  ]
}
```

### GET /api/compliance/violations

Get recent SLA violations.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| days | int | Days to look back (default: 7) |

**Response (200 OK):**
```json
[
  {
    "violationId": "violation-uuid",
    "metricType": "QUEUE_STABILITY",
    "violationDate": "2025-01-14",
    "complianceRate": 78.5,
    "upperControlLimit": 100.0,
    "lowerControlLimit": 85.0,
    "severity": "CRITICAL",
    "description": "QUEUE_STABILITY metric out of control: 78.50% compliance (limits: 85.00% - 100.00%)"
  }
]
```

---

## 14. Data Retention

**Base Path:** `/api/retention`

ISO 27001 A.18.1.3 - Records management and data lifecycle.

### Retention Periods (Default)

| Entity Type | Retention | Requirement |
|-------------|-----------|-------------|
| AUDIT_LOG | 7 years | HIPAA/DPDP |
| PATIENT_RECORD | 7 years | Clinical Establishments Act |
| MEDICAL_RECORD | 10 years | Legal requirement |
| PRESCRIPTION | 3 years | Pharmacy regulations |
| APPOINTMENT | 2 years | Operational |
| SESSION | 90 days | Security |
| NOTIFICATION | 30 days | Operational |

### Archival Actions

| Action | Description |
|--------|-------------|
| SOFT_DELETE | Set deletedAt timestamp |
| EXPORT_TO_S3 | Export to MinIO/S3 then delete |
| ANONYMIZE | Replace PII with pseudonymous values |
| HARD_DELETE | Permanent deletion |

### Endpoints

| Method | Endpoint | Description | Required Role |
|--------|----------|-------------|---------------|
| GET | `/policies` | List retention policies | ADMIN, COMPLIANCE_OFFICER |
| GET | `/policies/{entityType}` | Get policy for entity | ADMIN, COMPLIANCE_OFFICER |
| PUT | `/policies/{entityType}` | Update policy | ADMIN, COMPLIANCE_OFFICER |
| GET | `/archival-logs` | Get archival logs | ADMIN, COMPLIANCE_OFFICER |
| GET | `/archival-logs/recent` | Recent successful archival | ADMIN, COMPLIANCE_OFFICER |
| GET | `/archival-logs/failed` | Failed archival jobs | ADMIN, COMPLIANCE_OFFICER |
| POST | `/execute/{entityType}` | Trigger policy execution | ADMIN, COMPLIANCE_OFFICER |

### GET /api/retention/policies

Get all retention policies.

**Response (200 OK):**
```json
[
  {
    "id": "policy-uuid",
    "entityType": "AUDIT_LOG",
    "retentionDays": 2555,
    "gracePeriodDays": 30,
    "archivalAction": "EXPORT_TO_S3",
    "enabled": true,
    "lastExecution": "2025-01-15T02:00:00Z",
    "recordsArchived": 15000,
    "createdAt": "2024-01-01T00:00:00Z"
  }
]
```

### PUT /api/retention/policies/{entityType}

Update a retention policy.

**Request Body:**
```json
{
  "retentionDays": 3650,
  "gracePeriodDays": 60,
  "archivalAction": "ANONYMIZE",
  "enabled": true
}
```

---

## 15. Access Audit

**Base Path:** `/api/access-audit`

ISO 27001 A.12.4 - Logging and monitoring for sensitive data access.

### Access Types Logged

| Type | Description |
|------|-------------|
| VIEW_MEDICAL_RECORD | Read patient medical record |
| VIEW_PRESCRIPTION | Read prescription details |
| VIEW_LAB_RESULT | Read lab test results |
| VIEW_PATIENT_DETAILS | Read patient demographics |
| EXPORT_PATIENT_DATA | Bulk data export |
| MODIFY_MEDICAL_RECORD | Update medical record |
| PRINT_PRESCRIPTION | Print prescription |
| VIEW_BILLING_DETAILS | Read billing information |

### Endpoints

| Method | Endpoint | Description | Required Role |
|--------|----------|-------------|---------------|
| GET | `/patient/{patientId}` | Patient access logs | ADMIN, COMPLIANCE_OFFICER |
| GET | `/patient/{patientId}/complete` | Complete patient audit trail | ADMIN, COMPLIANCE_OFFICER |
| GET | `/user/{userId}` | User's access logs | ADMIN, COMPLIANCE_OFFICER |
| GET | `/recent` | Recent access logs | ADMIN, COMPLIANCE_OFFICER |
| GET | `/exports` | Data export operations | ADMIN, COMPLIANCE_OFFICER |
| GET | `/entity/{type}/{id}` | Entity access history | ADMIN, COMPLIANCE_OFFICER |
| GET | `/by-type/{accessType}` | Filter by access type | ADMIN, COMPLIANCE_OFFICER |
| GET | `/suspicious` | Detect anomalies | ADMIN, COMPLIANCE_OFFICER |
| GET | `/report` | Generate audit report | ADMIN, COMPLIANCE_OFFICER |

### GET /api/access-audit/patient/{patientId}

Get all access logs for a specific patient (HIPAA 164.308(a)(7)(ii) compliance).

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| startDate | LocalDate | Range start (default: 30 days ago) |
| endDate | LocalDate | Range end (default: today) |
| page | int | Page number (default: 0) |
| size | int | Page size (default: 20) |

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": "log-uuid",
      "userId": "user-uuid",
      "patientId": "patient-uuid",
      "entityType": "MedicalRecord",
      "entityId": "record-uuid",
      "accessType": "VIEW_MEDICAL_RECORD",
      "accessTimestamp": "2025-01-15T10:30:00Z",
      "ipAddress": "192.168.1.100",
      "userAgent": "Mozilla/5.0...",
      "accessReason": "Routine consultation",
      "dataExported": false
    }
  ],
  "totalElements": 45,
  "totalPages": 3
}
```

### GET /api/access-audit/report

Generate comprehensive access audit report.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| startDate | LocalDate | Report start (default: 30 days ago) |
| endDate | LocalDate | Report end (default: today) |

**Response (200 OK):**
```json
{
  "tenantId": "tenant-uuid",
  "startDate": "2025-01-01",
  "endDate": "2025-01-15",
  "generatedAt": "2025-01-15T12:00:00Z",
  "totalAccess": 1250,
  "dataExportCount": 15,
  "uniqueUsers": 25,
  "uniquePatients": 450,
  "accessByType": {
    "VIEW_MEDICAL_RECORD": 800,
    "VIEW_PRESCRIPTION": 300,
    "VIEW_LAB_RESULT": 100,
    "EXPORT_PATIENT_DATA": 15,
    "MODIFY_MEDICAL_RECORD": 35
  },
  "entityAccess": {
    "MedicalRecord": 835,
    "Prescription": 300,
    "LabResult": 100,
    "Patient": 15
  }
}
```

### GET /api/access-audit/suspicious

Detect suspicious access patterns (anomaly detection).

**Heuristics:**
1. Multiple accesses to same patient in < 5 minutes
2. Access from unusual IP addresses
3. Bulk exports (> 10 records)
4. Off-hours access (midnight to 6 AM)
5. High-frequency access (> 100 records in 1 hour)

**Response (200 OK):**
```json
[
  {
    "patternType": "HIGH_FREQUENCY_ACCESS",
    "userId": "user-uuid",
    "accessCount": 150,
    "timeWindow": "1 hour",
    "detectedAt": "2025-01-15T03:30:00Z",
    "severity": "HIGH",
    "recommendation": "Review user activity and verify legitimate need"
  }
]
```

---

## 16. Admin - Materialized Views

**Base Path:** `/api/admin/materialized-views`

Administrative endpoints for materialized view management.

### Materialized Views (Phase 1)

| View | Purpose | Refresh Frequency |
|------|---------|-------------------|
| mv_patient_clinical_summary | Patient clinical overview | 15 minutes |
| mv_billing_summary_by_period | Financial aggregations | Hourly |
| mv_user_notification_summary | User notifications | 30 minutes |

### Endpoints

| Method | Endpoint | Description | Required Role |
|--------|----------|-------------|---------------|
| GET | `/health` | Health check | ADMIN |
| POST | `/refresh/all` | Refresh all views | ADMIN |
| POST | `/refresh/patient-summary` | Refresh patient view | ADMIN |
| POST | `/refresh/billing-summary` | Refresh billing view | ADMIN |
| POST | `/refresh/notification-summary` | Refresh notification view | ADMIN |

### POST /api/admin/materialized-views/refresh/all

Manually refresh all materialized views.

**Use Cases:**
- After bulk data import
- After data migration
- Testing and development
- Recovery from refresh failures

**Response (200 OK):**
```
"Successfully refreshed all materialized views in 1250ms"
```

**Error Response (500):**
```
"Failed to refresh views: Connection timeout"
```

---

## Error Responses

All endpoints return consistent error responses.

### Standard Error Format

```json
{
  "timestamp": "2025-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/patients",
  "errors": [
    {
      "field": "email",
      "message": "must be a valid email address"
    }
  ]
}
```

### HTTP Status Codes

| Code | Meaning |
|------|---------|
| 200 | OK - Request succeeded |
| 201 | Created - Resource created |
| 204 | No Content - Success with no response body |
| 400 | Bad Request - Invalid input |
| 401 | Unauthorized - Authentication required |
| 403 | Forbidden - Insufficient permissions |
| 404 | Not Found - Resource doesn't exist |
| 409 | Conflict - Business rule violation |
| 422 | Unprocessable Entity - Validation failed |
| 429 | Too Many Requests - Rate limit exceeded |
| 500 | Internal Server Error - Server error |

---

## Pagination

All list endpoints support pagination using Spring Data conventions.

### Query Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | Page number (0-indexed) |
| size | int | 20 | Items per page |
| sort | string | - | Sort field and direction |

### Example

```
GET /api/patients?page=0&size=10&sort=lastName,asc
```

### Response Format

```json
{
  "content": [...],
  "totalElements": 150,
  "totalPages": 15,
  "size": 10,
  "number": 0,
  "first": true,
  "last": false,
  "numberOfElements": 10,
  "empty": false
}
```

---

## Rate Limiting

API endpoints are rate-limited to prevent abuse.

| Tier | Limit | Window |
|------|-------|--------|
| Standard | 100 requests | 1 minute |
| Authenticated | 500 requests | 1 minute |
| Admin | 1000 requests | 1 minute |

When rate limit is exceeded, response:
- Status: `429 Too Many Requests`
- Header: `Retry-After: 30` (seconds)

---

## Multi-Tenancy

All data is isolated by tenant. The tenant ID is:

1. **Extracted from JWT** for authenticated requests
2. **Validated** against user's assigned tenant
3. **Enforced** at database level via Row Level Security (RLS)

Attempting to access another tenant's data returns `403 Forbidden`.

---

## API Summary

### Total Endpoints: 130+

| Category | Endpoints | Description |
|----------|-----------|-------------|
| Authentication | 4 | Login, logout, refresh, profile |
| Patients | 7 | Patient CRUD and search |
| Appointments | 12 | Scheduling and status management |
| Users | 11 | Staff management |
| Roles | 11 | RBAC management |
| Billing | 10 | Invoicing and payments |
| Prescriptions | 6 | Prescription workflow |
| Queue Management | 6 | M/M/1 queue analytics |
| Inventory Optimization | 7 | EOQ, ROP, ABC analysis |
| Branches | 13 | Multi-location management |
| Treatments | 10 | Treatment catalog |
| Medical Orders | 18 | Order lifecycle |
| Compliance Dashboard | 3 | SPC monitoring |
| Data Retention | 7 | Archival management |
| Access Audit | 10 | Sensitive data tracking |
| Admin Views | 5 | Materialized view refresh |

---

*Last Updated: January 2025*
