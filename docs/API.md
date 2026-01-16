# API Documentation

Comprehensive API documentation for the Clinic Administration REST API.

## Table of Contents

1. [API Overview](#api-overview)
2. [Authentication](#authentication)
3. [API Conventions](#api-conventions)
4. [Error Handling](#error-handling)
5. [API Endpoints](#api-endpoints)
6. [Request/Response Examples](#requestresponse-examples)
7. [Pagination](#pagination)
8. [Filtering and Sorting](#filtering-and-sorting)
9. [Rate Limiting](#rate-limiting)
10. [API Versioning](#api-versioning)

## API Overview

### Base URL

```
Development: http://localhost:8080/api
Production:  https://api.clinic.example.com/api
```

### API Design Principles

- **RESTful**: Resource-oriented URLs, HTTP methods for operations
- **JSON**: All requests and responses use JSON format
- **Stateless**: JWT-based authentication, no server-side sessions
- **Versioned**: API versioning via URL path (`/api/v1/...`)
- **Paginated**: Large collections support pagination
- **Filtered**: Query parameters for filtering, sorting, searching
- **Documented**: OpenAPI 3.0 (Swagger) specification

### Technology Stack

- **Framework**: Spring Boot 3.3.7
- **API Documentation**: SpringDoc OpenAPI 2.x
- **Validation**: Hibernate Validator (Bean Validation)
- **Serialization**: Jackson (JSON)
- **Security**: Spring Security 6 + JWT

## Authentication

### JWT Token Authentication

All protected endpoints require a valid JWT access token in the Authorization header.

#### Request Header

```http
Authorization: Bearer <access_token>
```

#### Authentication Flow

```
1. Login
POST /api/auth/login
{
  "email": "doctor@clinic.com",
  "password": "SecurePassword123!"
}

Response:
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900000
}

2. Use Access Token
GET /api/patients
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...

3. Refresh Token (when access token expires)
POST /api/auth/refresh
{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}

Response:
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",  // New access token
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...", // Same refresh token
  "tokenType": "Bearer",
  "expiresIn": 900000
}
```

### Token Expiration

| Token Type | Expiration | Use Case |
|------------|-----------|----------|
| **Access Token** | 15 minutes | API requests |
| **Refresh Token** | 7 days | Token renewal |

### Public Endpoints

No authentication required:
- `POST /api/auth/login`
- `POST /api/auth/register` (future)
- `GET /api/public/**`
- `GET /actuator/health`
- `GET /actuator/prometheus`
- `GET /api/docs/**`
- `GET /swagger-ui/**`

## API Conventions

### HTTP Methods

| Method | Usage | Idempotent |
|--------|-------|-----------|
| **GET** | Retrieve resources | Yes |
| **POST** | Create new resource | No |
| **PUT** | Update/replace entire resource | Yes |
| **PATCH** | Partial update | No |
| **DELETE** | Delete resource (soft delete) | Yes |

### Resource Naming

- Use **plural nouns** for collections: `/patients`, `/appointments`
- Use **singular nouns** for single resources: `/patients/{id}`
- Use **kebab-case** for multi-word resources: `/lab-tests`
- Use **nested resources** for relationships: `/patients/{id}/appointments`

### HTTP Status Codes

| Code | Meaning | Use Case |
|------|---------|----------|
| **200 OK** | Success | GET, PUT, PATCH requests |
| **201 Created** | Resource created | POST requests |
| **204 No Content** | Success, no response body | DELETE requests |
| **400 Bad Request** | Invalid request data | Validation failures |
| **401 Unauthorized** | Missing/invalid auth token | Authentication failure |
| **403 Forbidden** | Insufficient permissions | Authorization failure |
| **404 Not Found** | Resource not found | Invalid resource ID |
| **409 Conflict** | Resource conflict | Duplicate email, etc. |
| **422 Unprocessable Entity** | Business logic error | State transition error |
| **429 Too Many Requests** | Rate limit exceeded | Throttling |
| **500 Internal Server Error** | Server error | Unexpected errors |

### Request Headers

| Header | Required | Description |
|--------|----------|-------------|
| `Authorization` | Yes* | JWT access token (`Bearer <token>`) |
| `Content-Type` | Yes** | `application/json` |
| `Accept` | No | `application/json` (default) |
| `X-Tenant-ID` | No*** | Tenant ID (extracted from JWT) |
| `X-Request-ID` | No | Request tracing ID |

*Required for protected endpoints
**Required for POST, PUT, PATCH requests
***Automatically extracted from JWT, manual override not allowed

### Response Headers

| Header | Description |
|--------|-------------|
| `Content-Type` | `application/json` |
| `X-Total-Count` | Total number of items (pagination) |
| `X-Page-Number` | Current page number |
| `X-Page-Size` | Items per page |

## Error Handling

### Error Response Format

All errors follow a consistent JSON structure:

```json
{
  "timestamp": "2026-01-16T10:30:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/patients",
  "errors": [
    {
      "field": "email",
      "rejectedValue": "invalid-email",
      "message": "Invalid email format"
    },
    {
      "field": "dateOfBirth",
      "rejectedValue": null,
      "message": "Date of birth is required"
    }
  ]
}
```

### Error Types

#### Validation Error (400)

```json
{
  "timestamp": "2026-01-16T10:30:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/patients",
  "errors": [
    {
      "field": "firstName",
      "message": "First name must be 2-50 characters"
    }
  ]
}
```

#### Authentication Error (401)

```json
{
  "timestamp": "2026-01-16T10:30:00.000Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired token",
  "path": "/api/patients"
}
```

#### Authorization Error (403)

```json
{
  "timestamp": "2026-01-16T10:30:00.000Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Insufficient permissions to access this resource",
  "path": "/api/users"
}
```

#### Not Found Error (404)

```json
{
  "timestamp": "2026-01-16T10:30:00.000Z",
  "status": 404,
  "error": "Not Found",
  "message": "Patient not found with ID: 123e4567-e89b-12d3-a456-426614174000",
  "path": "/api/patients/123e4567-e89b-12d3-a456-426614174000"
}
```

#### Business Logic Error (422)

```json
{
  "timestamp": "2026-01-16T10:30:00.000Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Cannot cancel appointment: already checked in",
  "path": "/api/appointments/123e4567-e89b-12d3-a456-426614174000/cancel"
}
```

## API Endpoints

### Authentication Endpoints

#### Login

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "doctor@clinic.com",
  "password": "SecurePassword123!"
}
```

**Response** (200 OK):
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900000,
  "user": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "email": "doctor@clinic.com",
    "firstName": "Dr. John",
    "lastName": "Doe",
    "roles": ["DOCTOR", "USER"]
  }
}
```

#### Refresh Token

```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**Response** (200 OK):
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900000
}
```

#### Logout

```http
POST /api/auth/logout
Authorization: Bearer <access_token>
```

**Response** (204 No Content)

### Patient Endpoints

#### List Patients

```http
GET /api/patients?page=0&size=20&sort=lastName,asc&search=john
Authorization: Bearer <access_token>
```

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": "456e7890-e12b-34c5-d678-901234567890",
      "fullName": "John Doe",
      "firstName": "John",
      "lastName": "Doe",
      "age": 35,
      "gender": "MALE",
      "phone": "9876543210",
      "email": "john.doe@example.com",
      "bloodGroup": "O+",
      "status": "ACTIVE",
      "createdAt": "2026-01-15T10:00:00Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "sorted": true,
      "orders": [{"property": "lastName", "direction": "ASC"}]
    }
  },
  "totalElements": 150,
  "totalPages": 8,
  "last": false,
  "first": true,
  "numberOfElements": 20
}
```

#### Get Patient Detail

```http
GET /api/patients/{id}
Authorization: Bearer <access_token>
```

**Response** (200 OK):
```json
{
  "id": "456e7890-e12b-34c5-d678-901234567890",
  "fullName": "John Doe",
  "firstName": "John",
  "middleName": "Smith",
  "lastName": "Doe",
  "dateOfBirth": "1990-05-15",
  "age": 35,
  "gender": "MALE",
  "phone": "9876543210",
  "email": "john.doe@example.com",
  "bloodGroup": "O+",
  "abhaId": "12345678901234",
  "maritalStatus": "MARRIED",
  "occupation": "Software Engineer",
  "address": {
    "line1": "123 Main Street",
    "line2": "Apartment 4B",
    "city": "Bangalore",
    "state": "Karnataka",
    "pincode": "560001"
  },
  "emergencyContact": {
    "name": "Jane Doe",
    "phone": "9876543211",
    "relation": "SPOUSE"
  },
  "allergies": ["Penicillin", "Peanuts"],
  "chronicConditions": ["Diabetes Type 2", "Hypertension"],
  "latestVitals": {
    "recordedAt": "2026-01-15T09:30:00Z",
    "temperature": 36.8,
    "pulse": 72,
    "bloodPressure": "120/80",
    "weight": 75.5,
    "height": 175,
    "bmi": 24.7
  },
  "statistics": {
    "totalAppointments": 12,
    "totalPrescriptions": 8,
    "totalLabTests": 5,
    "outstandingBalance": 1500.00
  },
  "createdAt": "2025-06-10T08:00:00Z",
  "updatedAt": "2026-01-15T09:30:00Z"
}
```

#### Create Patient

```http
POST /api/patients
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "firstName": "John",
  "middleName": "Smith",
  "lastName": "Doe",
  "dateOfBirth": "1990-05-15",
  "gender": "MALE",
  "phone": "9876543210",
  "email": "john.doe@example.com",
  "bloodGroup": "O+",
  "abhaId": "12345678901234",
  "maritalStatus": "MARRIED",
  "occupation": "Software Engineer",
  "addressLine1": "123 Main Street",
  "addressLine2": "Apartment 4B",
  "city": "Bangalore",
  "state": "Karnataka",
  "pincode": "560001",
  "emergencyContactName": "Jane Doe",
  "emergencyContactPhone": "9876543211",
  "emergencyContactRelation": "SPOUSE",
  "allergies": ["Penicillin", "Peanuts"],
  "chronicConditions": ["Diabetes Type 2"]
}
```

**Response** (201 Created):
```json
{
  "id": "456e7890-e12b-34c5-d678-901234567890",
  "fullName": "John Smith Doe",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "createdAt": "2026-01-16T10:30:00Z"
}
```

#### Update Patient

```http
PUT /api/patients/{id}
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "phone": "9876543299",
  "email": "john.new@example.com",
  "addressLine1": "456 New Street"
}
```

**Response** (200 OK):
```json
{
  "id": "456e7890-e12b-34c5-d678-901234567890",
  "fullName": "John Smith Doe",
  "phone": "9876543299",
  "email": "john.new@example.com",
  "updatedAt": "2026-01-16T10:35:00Z"
}
```

#### Delete Patient (Soft Delete)

```http
DELETE /api/patients/{id}
Authorization: Bearer <access_token>
```

**Response** (204 No Content)

### Appointment Endpoints

#### List Appointments

```http
GET /api/appointments?date=2026-01-16&status=SCHEDULED
Authorization: Bearer <access_token>
```

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": "789a0123-b45c-67d8-e901-234567890abc",
      "patientName": "John Doe",
      "doctorName": "Dr. Sarah Smith",
      "appointmentDate": "2026-01-16",
      "startTime": "10:00:00",
      "endTime": "10:30:00",
      "status": "SCHEDULED",
      "consultationType": "IN_PERSON",
      "chiefComplaint": "Regular checkup",
      "createdAt": "2026-01-10T15:00:00Z"
    }
  ],
  "totalElements": 25,
  "totalPages": 2
}
```

#### Create Appointment

```http
POST /api/appointments
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "patientId": "456e7890-e12b-34c5-d678-901234567890",
  "providerId": "123e4567-e89b-12d3-a456-426614174000",
  "appointmentDate": "2026-01-20",
  "startTime": "14:00:00",
  "durationMinutes": 30,
  "consultationType": "IN_PERSON",
  "chiefComplaint": "Knee pain",
  "notes": "Patient reports pain for 3 days"
}
```

**Response** (201 Created):
```json
{
  "id": "789a0123-b45c-67d8-e901-234567890abc",
  "patientName": "John Doe",
  "doctorName": "Dr. Sarah Smith",
  "appointmentDate": "2026-01-20",
  "startTime": "14:00:00",
  "endTime": "14:30:00",
  "status": "SCHEDULED",
  "createdAt": "2026-01-16T10:40:00Z"
}
```

#### Update Appointment Status

```http
PATCH /api/appointments/{id}/status
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "status": "CONFIRMED"
}
```

**Response** (200 OK):
```json
{
  "id": "789a0123-b45c-67d8-e901-234567890abc",
  "status": "CONFIRMED",
  "confirmedAt": "2026-01-16T10:45:00Z"
}
```

### User Endpoints

#### List Users

```http
GET /api/users?role=DOCTOR
Authorization: Bearer <access_token>
```

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "fullName": "Dr. Sarah Smith",
      "email": "sarah.smith@clinic.com",
      "roles": ["DOCTOR", "USER"],
      "status": "ACTIVE",
      "createdAt": "2025-01-15T08:00:00Z"
    }
  ],
  "totalElements": 8
}
```

#### Get User Detail

```http
GET /api/users/{id}
Authorization: Bearer <access_token>
```

**Response** (200 OK):
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "firstName": "Sarah",
  "lastName": "Smith",
  "email": "sarah.smith@clinic.com",
  "phone": "9876543220",
  "roles": [
    {
      "id": "role-uuid-1",
      "name": "DOCTOR",
      "description": "Medical practitioner"
    }
  ],
  "permissions": [
    "PATIENT:READ",
    "PATIENT:CREATE",
    "APPOINTMENT:READ",
    "PRESCRIPTION:CREATE"
  ],
  "status": "ACTIVE",
  "createdAt": "2025-01-15T08:00:00Z",
  "lastLoginAt": "2026-01-16T08:30:00Z"
}
```

## Request/Response Examples

### Common Scenarios

#### Scenario 1: Doctor Views Today's Appointments

```bash
curl -X GET "http://localhost:8080/api/appointments/today" \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

**Response**:
```json
{
  "appointments": [
    {
      "id": "appt-1",
      "patientName": "John Doe",
      "time": "10:00",
      "status": "CONFIRMED",
      "chiefComplaint": "Regular checkup"
    },
    {
      "id": "appt-2",
      "patientName": "Jane Smith",
      "time": "11:00",
      "status": "CHECKED_IN",
      "chiefComplaint": "Knee pain"
    }
  ],
  "total": 8
}
```

#### Scenario 2: Receptionist Creates Billing Record

```bash
curl -X POST "http://localhost:8080/api/billings" \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": "patient-uuid",
    "appointmentId": "appointment-uuid",
    "items": [
      {
        "description": "Consultation Fee",
        "quantity": 1,
        "unitPrice": 500.00
      },
      {
        "description": "X-Ray",
        "quantity": 1,
        "unitPrice": 800.00
      }
    ],
    "discount": 50.00,
    "notes": "Insurance applicable"
  }'
```

**Response**:
```json
{
  "id": "billing-uuid",
  "invoiceNumber": "INV-2026-001234",
  "patientName": "John Doe",
  "totalAmount": 1300.00,
  "discountAmount": 50.00,
  "taxAmount": 234.00,
  "netAmount": 1484.00,
  "paidAmount": 0.00,
  "balanceAmount": 1484.00,
  "status": "UNPAID",
  "createdAt": "2026-01-16T11:00:00Z"
}
```

## Pagination

### Pagination Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | integer | 0 | Page number (0-indexed) |
| `size` | integer | 20 | Items per page (max: 100) |
| `sort` | string | - | Sort field and direction |

### Pagination Example

```http
GET /api/patients?page=2&size=50&sort=lastName,asc
```

### Pagination Response

```json
{
  "content": [...],
  "pageable": {
    "pageNumber": 2,
    "pageSize": 50,
    "sort": {"sorted": true}
  },
  "totalElements": 500,
  "totalPages": 10,
  "last": false,
  "first": false,
  "numberOfElements": 50
}
```

## Filtering and Sorting

### Filter Parameters

```http
GET /api/appointments?status=SCHEDULED&date=2026-01-16&doctorId=doctor-uuid
GET /api/patients?bloodGroup=O+&gender=MALE&minAge=18&maxAge=65
GET /api/billings?status=UNPAID&fromDate=2026-01-01&toDate=2026-01-31
```

### Search Parameter

```http
GET /api/patients?search=john
GET /api/users?search=doctor@clinic.com
```

### Sort Parameter

```http
GET /api/patients?sort=lastName,asc&sort=firstName,asc
GET /api/appointments?sort=appointmentDate,desc
```

## Rate Limiting

### Rate Limit Headers

| Header | Description |
|--------|-------------|
| `X-RateLimit-Limit` | Requests allowed per window |
| `X-RateLimit-Remaining` | Requests remaining |
| `X-RateLimit-Reset` | Reset time (Unix timestamp) |

### Rate Limits

| Endpoint Category | Limit | Window |
|-------------------|-------|--------|
| Authentication | 5 requests | 15 minutes |
| Read Operations | 100 requests | 1 minute |
| Write Operations | 20 requests | 1 minute |

### Rate Limit Exceeded Response

```json
{
  "timestamp": "2026-01-16T11:00:00.000Z",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Try again in 45 seconds.",
  "retryAfter": 45
}
```

## API Versioning

### Current Version

```
/api/v1/...  (Current)
```

### Future Versions

```
/api/v2/...  (Future)
```

### Version Negotiation

- **URL-based versioning** (current approach)
- Version specified in URL path
- Backward compatibility maintained for at least 1 year

## Interactive API Documentation

### Swagger UI

Access interactive API documentation at:

```
Development: http://localhost:8080/swagger-ui.html
Production:  https://api.clinic.example.com/swagger-ui.html
```

### OpenAPI Specification

Download OpenAPI specification:

```
JSON: http://localhost:8080/v3/api-docs
YAML: http://localhost:8080/v3/api-docs.yaml
```

## References

- [REST API Best Practices](https://restfulapi.net/)
- [OpenAPI Specification](https://swagger.io/specification/)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/3.3.7/reference/htmlsingle/)
- [ARCHITECTURE.md](ARCHITECTURE.md)
- [SECURITY.md](SECURITY.md)

---

**Document Version**: 1.0
**Last Updated**: 2026-01-16
**Maintained By**: API Team
