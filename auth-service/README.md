# 🔐 Auth Service

Authentication and user-profile microservice for the Food Ordering System.

- Service name: `auth-service`
- Default port: `8081`
- Context path: `/auth`
- Stack: Spring Boot, Spring Security, JPA, PostgreSQL, JWT, OpenAPI

---

## ✅ Current Capabilities

### Authentication

- Register with email/password (`POST /auth/register`)
- Login with email/password (`POST /auth/login`)
- Refresh access token using refresh token (`POST /auth/refresh`)
- Logout with ownership-checked refresh token revocation (`POST /auth/logout`)
- Session/token validation for authenticated requests (`POST /auth/validate`)

### Password Operations

- Change password for authenticated users (`POST /auth/change-password`)
- Forgot password token generation (`POST /auth/forgot-password`)
- Reset password with reset token (`POST /auth/reset-password`)
- Password-reset tokens are stored **hashed (SHA-256)** in DB (raw token is only returned to caller)

### User/Profile

- Get authenticated user profile (`GET /auth/users/me`)
- Update profile and addresses (`PUT /auth/users/profile`)
- Address management is profile-driven (no separate address CRUD endpoints)
- Address constraints: maximum 3 addresses per user, at most one default

### Admin User Management

- Create user (`POST /auth/admin/users`)
- List users (`GET /auth/admin/users`)
- Get user by id (`GET /auth/admin/users/{id}`)
- Update active status (`PATCH /auth/admin/users/{id}/status`)
- Update user details/role (`PUT /auth/admin/users/{id}`)
- Delete user (`DELETE /auth/admin/users/{id}`)

### Security / Hardening

- Stateless security (`SessionCreationPolicy.STATELESS`)
- JWT auth filter for protected endpoints
- Method-level authorization for admin APIs (`@PreAuthorize("hasRole('ADMIN')")`)
- Request validation using Bean Validation (`@Valid`, `@NotBlank`, `@Email`, `@Size`, etc.)
- Centralized error handling in `GlobalExceptionHandler`
- Audit logging for critical auth events

---

## 🔗 API Docs

When running locally:

- Swagger UI: `http://localhost:8081/auth/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8081/auth/v3/api-docs`
- Health: `http://localhost:8081/auth/health`

Via API Gateway:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Auth OpenAPI: `http://localhost:8080/auth/v3/api-docs`

---

## ⚙️ Configuration

Main config file:

- `src/main/resources/application.properties`

Important properties:

- `server.servlet.context-path=/auth`
- `app.jwt.secret`
- `app.jwt.access-expiration`
- `app.auth.refresh-expiry-days`
- `spring.datasource.*`

### Local Secret Handling

The service loads local secrets from `.env` if present:

- `spring.config.import=optional:file:.env[.properties]`
- `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`, `JWT_SECRET`, `ADMIN_PASSWORD`, and `CUSTOMER_PASSWORD` are required in runtime environments.

Recommended local `.env` values:

```env
JWT_SECRET=change-this-secret
DATABASE_URL=jdbc:postgresql://<host>:5432/<db>?sslmode=require&channel_binding=require
DATABASE_USER=<db-user>
DATABASE_PASSWORD=<db-password>
ADMIN_PASSWORD=<admin-password>
CUSTOMER_PASSWORD=<customer-password>
```

---

## 🚀 Run

### With Maven

```bash
cd auth-service
mvn clean install
mvn spring-boot:run
```

### With Docker

```bash
cd auth-service
docker build -t auth-service:latest .
docker run -p 8081:8081 auth-service:latest
```

---

## 🧪 Tests

Run all tests:

```bash
cd auth-service
mvn test
```

Run key suites:

```bash
mvn "-Dtest=AuthServiceImplTest" test
mvn "-Dtest=AuthControllerIntegrationTest" test
mvn "-Dtest=AdminControllerIntegrationTest" test
mvn "-Dtest=UserControllerIntegrationTest" test
```

Run API Gateway end-to-end verification (from repo root):

```powershell
powershell -ExecutionPolicy Bypass -File ./verify-auth-gateway.ps1
```

Optional direct-service smoke test (when not testing through gateway):

```powershell
powershell -ExecutionPolicy Bypass -File ./test-api.ps1 -BaseUrl http://localhost:8081
```

---

## 📝 Notes

- Auth endpoints are under `/auth` because of service context path.
- API gateway is expected to be the single public entry point in deployed environments.
- For production, keep secrets only in secure env/secret stores and avoid plaintext credentials in source-controlled files.
