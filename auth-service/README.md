# 🔐 Auth Service — Comprehensive Implementation Analysis

Authentication, authorization, account management, and user-profile microservice for the Food Ordering System.

---

## 1) Service Identity and Scope

- Service name: `auth-service`
- Default local port: `8081`
- Context path: `/auth`
- Primary responsibility: identity lifecycle + account/profile data + admin user control
- Technology stack:
  - Spring Boot (`spring-boot-starter-parent` 4.0.3 in current `pom.xml`)
  - Spring Web + Spring Security + Method Security
  - Spring Data JPA with PostgreSQL
  - JWT (`io.jsonwebtoken` / JJWT)
  - Bean Validation (`jakarta.validation`)
  - OpenAPI/Swagger (SpringDoc)
  - Actuator for health/metrics

This service is designed to run behind API Gateway in production and can also run standalone for local testing.

---

## 2) Current Implemented Functionalities

### 2.1 Authentication Lifecycle

1. **Register** (`POST /auth/register`)
   - Creates a new customer account if email is unique.
   - Password stored as BCrypt hash.
   - Returns both access token and refresh token.

2. **Login** (`POST /auth/login`)
   - Verifies email + password.
   - Rejects inactive accounts.
   - Returns access token and refresh token.

3. **Refresh** (`POST /auth/refresh`)
   - Accepts a non-revoked refresh token.
   - Validates expiry.
   - Issues a new access token and a newly persisted refresh token.

4. **Logout** (`POST /auth/logout`)
   - Revokes the provided refresh token.
   - Ownership is checked in authenticated variant (`logout(String email, RefreshRequest)`).

5. **Validate session/token** (`POST /auth/validate`)
   - Requires valid bearer token.
   - Returns `valid=true` and authenticated principal details.

### 2.2 Password Management

1. **Change password** (`POST /auth/change-password`)
   - Requires current password match.
   - Enforces minimum length and old/new mismatch.
   - Stores new BCrypt hash.

2. **Forgot password** (`POST /auth/forgot-password`)
   - Generates a reset token for an existing email.
   - Stores only SHA-256 hash of token in DB.
   - Returns raw token to caller (for delivery workflow integration).

3. **Reset password** (`POST /auth/reset-password`)
   - Matches incoming token by hashed value.
   - Rejects used/expired/invalid token.
   - Updates password hash and marks token as used.

### 2.3 User Profile and Address Domain

1. **Get profile** (`GET /auth/users/me`)
   - Returns identity + role + active state.
   - Includes `primaryAddress` and full `addresses` list.

2. **Update profile** (`PUT /auth/users/profile`)
   - Supports email/full-name updates.
   - Supports profile-embedded address synchronization.

3. **Address model behavior** (no separate address CRUD endpoints)
   - Max 3 addresses per user.
   - Max 1 default address.
   - Submit list to upsert/delete via sync behavior.
   - If none is marked default and list is non-empty, first address is auto-defaulted.

### 2.4 Admin User Management (RBAC protected)

All under `POST/GET/PUT/PATCH/DELETE /auth/admin/users...`, guarded by `@PreAuthorize("hasRole('ADMIN')")`:

- Create user
- List all users
- Get user by ID
- Update active status
- Update user fields/role
- Delete user

---

## 3) Architecture and Internal Design

### 3.1 Layered Architecture

The service uses a standard layered pattern:

- **Controller layer**
  - `AuthController`, `UserController`, `AdminController`
  - Receives HTTP requests, applies DTO validation, returns response models.

- **Service layer**
  - `AuthServiceImpl` implements core authentication and token lifecycle business logic.
  - `AuditService` records auth-related actions.

- **Persistence layer**
  - JPA repositories (`UserRepository`, `RefreshTokenRepository`, `PasswordResetTokenRepository`, `AddressRepository`, `AuthLogRepository`).

- **Security/utility layer**
  - `SecurityConfig` + `JwtAuthenticationFilter` for stateless auth.
  - `JwtTokenProvider` for token generation/validation/claim extraction.
  - `GlobalExceptionHandler` for API-wide error contract.

### 3.2 Runtime Request Flow (Protected Endpoint)

1. Request enters with `Authorization: Bearer <token>`.
2. `JwtAuthenticationFilter` validates token and extracts `email` + `role`.
3. Security context is populated with `ROLE_<role>` authority.
4. Controller endpoint executes (and method-level pre-authorization where applicable).
5. Service/repository operations run.
6. Uniform error payload returned via `GlobalExceptionHandler` when exceptions occur.

### 3.3 Token Strategy

- Access token: JWT signed with HMAC key from `app.jwt.secret`.
- Claims: `userId`, `email`, `role`; subject = email.
- Expiration: configured by `app.jwt.access-expiration`.
- Refresh token: opaque UUID, persisted in DB with expiry + revoked flag.

---

## 4) Data Model (Current Entities)

### 4.1 `User`

- `id` (UUID string)
- `email` (unique, not null)
- `username` (unique, not null; legacy compatibility populated from email in lifecycle hook)
- `passwordHash`
- `fullName`
- `role` (`ADMIN`/`CUSTOMER`)
- `active`
- `createdAt`, `updatedAt`

### 4.2 `RefreshToken`

- `id`, `userId`, `token` (unique), `expiryDate`, `revoked`, `createdAt`

### 4.3 `PasswordResetToken`

- `id`, `userId`, `token` (hashed), `expiryDate`, `used`, `createdAt`

### 4.4 `Address`

- `id`, `userId`, core address fields + optional geolocation/place metadata
- `isDefault`
- `createdAt`, `updatedAt`

### 4.5 `AuthLog`

- `id`, `userId`, `action`, `ipAddress`, `timestamp`

---

## 5) API Methods and Contracts (Implemented)

### 5.1 Public Endpoints (No Auth Required)

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/forgot-password`
- `POST /auth/reset-password`
- `GET /auth/health`
- Swagger/OpenAPI routes and actuator health routes

### 5.2 Authenticated Endpoints

- `POST /auth/logout`
- `POST /auth/change-password`
- `POST /auth/validate`
- `GET /auth/users/me`
- `PUT /auth/users/profile`
- all `/auth/admin/users/**` (ADMIN role required)

### 5.3 Response Style

- Token responses use `LoginResponse` containing:
  - `token` (compatibility)
  - `accessToken`
  - `refreshToken`
  - `email`, `userId`, `role`

- Validation and business errors are normalized by `GlobalExceptionHandler` with fields such as:
  - `timestamp`, `status`, `error`, `message`
  - `details` list for Bean Validation failures

---

## 6) Security Measurements and Hardening Techniques

### 6.1 Authentication and Authorization

- Stateless auth model (`SessionCreationPolicy.STATELESS`)
- Custom JWT authentication filter (`OncePerRequestFilter`)
- Role propagation from token claim to Spring `GrantedAuthority`
- Method-level RBAC on admin controller via `@PreAuthorize`

### 6.2 Credential and Secret Protection

- Password storage: BCrypt hash (never plain-text)
- Reset token at-rest protection: SHA-256 hashed before persistence
- Critical secrets externalized to environment variables:
  - `JWT_SECRET`
  - `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`
  - `ADMIN_PASSWORD`, `CUSTOMER_PASSWORD`

### 6.3 Session and Token Risk Controls

- Refresh tokens are persisted and explicitly revocable.
- Logout flow revokes refresh token and blocks reuse.
- Expired refresh tokens are revoked on access.
- Ownership check prevents one user from revoking another user’s session token.

### 6.4 Input and Error Security

- Bean validation on authentication and admin DTOs (`@Valid`, `@NotBlank`, `@Email`, `@Size`, etc.)
- Global exception handling prevents leaking internal stack traces through API responses
- Access denied and unauthorized responses explicitly handled

### 6.5 Auditing and Traceability

- Auth events logged to DB (`AuthLog`) for:
  - login success/failure
  - account locked attempts
  - password changes

---

## 7) Techniques and Patterns Used

- **Layered architecture** for separation of concerns.
- **Repository pattern** via Spring Data JPA.
- **DTO-based API contracts** to isolate external payloads from entities.
- **Declarative validation** using Jakarta Bean Validation.
- **Filter-based stateless authentication** with JWT.
- **Global exception translation** via `@ControllerAdvice`.
- **Environment-driven configuration** for secure deployments.
- **Profile-embedded aggregate update** pattern for addresses (`syncAddresses`).

---

## 8) Configuration and Operational Details

Main file: `src/main/resources/application.properties`

Key parameters:

- `server.servlet.context-path=/auth`
- `app.jwt.secret=${JWT_SECRET}`
- `app.jwt.access-expiration=${JWT_ACCESS_EXPIRATION:900000}`
- `app.auth.refresh-expiry-days=${JWT_REFRESH_EXPIRY_DAYS:7}`
- `spring.datasource.url=${DATABASE_URL}`
- `spring.datasource.username=${DATABASE_USER}`
- `spring.datasource.password=${DATABASE_PASSWORD}`

Bootstrap users are created on startup (if enabled and not already existing) through `AdminBootstrapConfig`.

---

## 9) API Documentation and Health

Local service:

- Swagger UI: `http://localhost:8081/auth/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8081/auth/v3/api-docs`
- Health endpoint: `http://localhost:8081/auth/health`

Via API Gateway:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Auth OpenAPI: `http://localhost:8080/auth/v3/api-docs`

---

## 10) Build, Run, and Verification

### 10.1 Run (Maven)

```bash
cd auth-service
mvn clean install
mvn spring-boot:run
```

### 10.2 Run (Docker)

```bash
cd auth-service
docker build -t auth-service:latest .
docker run -p 8081:8081 auth-service:latest
```

### 10.3 Test Suites

```bash
cd auth-service
mvn test
```

Key suites:

```bash
mvn "-Dtest=AuthServiceImplTest" test
mvn "-Dtest=AuthControllerIntegrationTest" test
mvn "-Dtest=AdminControllerIntegrationTest" test
mvn "-Dtest=UserControllerIntegrationTest" test
```

### 10.4 Gateway-Level E2E Verification

From repository root:

```powershell
powershell -ExecutionPolicy Bypass -File ./scripts/verify-auth-surface.ps1
```

Optional direct-service smoke script:

```powershell
powershell -ExecutionPolicy Bypass -File ./auth-service/test-api.ps1 -BaseUrl http://localhost:8081
```

---

## 11) Current Strengths and Practical Notes

### Strengths

- Complete auth lifecycle with refresh + revocation logic
- Profile and admin management integrated with role-based controls
- Hashed reset token storage and centralized error formatting
- Strong alignment for gateway-based integration testing

### Practical Notes

- Public clients should use API Gateway as the entry point in deployment.
- Keep secrets out of source control; use managed secret stores in cloud deployments.
- Ensure JWT secret length/entropy is production-grade for HMAC signing.

---

## 12) Summary

The current `auth-service` implementation is a production-oriented, stateless authentication subsystem with JWT, refresh-token lifecycle control, profile/address management, admin RBAC operations, persistent auditing, and standardized error/validation handling. It is actively integrated and verified through the API Gateway flow used by the rest of the microservices platform.
