# 🔐 Auth Service - Food Ordering System

> **Main Documentation:** See [Main README](../README.md)

---

## 🎯 Overview

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Status](https://img.shields.io/badge/Status-Production%20Ready-success.svg)]()

**Port:** 8081  
**Status:** ✅ Production Ready

Authentication and Authorization microservice for the Food Ordering System. Handles user registration, login, JWT token generation, and token validation.

### Core Functionality

✅ **User Registration**

- Email and username uniqueness validation
- BCrypt password hashing (strength: 10)
- Automatic role assignment (USER by default)
- Immediate JWT token generation

✅ **User Login**

- Email-based authentication
- Secure password verification
- Active account validation
- JWT token issuance with 24-hour expiry

✅ **Token Validation**

- JWT signature verification
- Expiry checking
- User information extraction
- Public endpoint for other microservices

✅ **Security**

- Stateless authentication (no sessions)
- BCrypt password encoding
- CSRF protection disabled (JWT-based)
- CORS ready for API Gateway integration

---

## 🏗️ Architecture

### Component Diagram

```
┌─────────────────────────────────────────────────┐
│            Auth Service (Port 8081)              │
├─────────────────────────────────────────────────┤
│                                                  │
│  ┌──────────────┐      ┌──────────────┐        │
│  │  Controller  │─────▶│   Service    │        │
│  │  (REST API)  │      │  (Business)  │        │
│  └──────────────┘      └──────────────┘        │
│         │                      │                 │
│         │                      ▼                 │
│         │              ┌──────────────┐         │
│         │              │  Repository  │         │
│         │              │    (JPA)     │         │
│         │              └──────────────┘         │
│         │                      │                 │
│         ▼                      ▼                 │
│  ┌──────────────┐      ┌──────────────┐        │
│  │  JWT Token   │      │   H2 / DB    │        │
│  │   Provider   │      │              │        │
│  └──────────────┘      └──────────────┘        │
│                                                  │
└─────────────────────────────────────────────────┘
```

### Layer Architecture

```
┌─────────────────────────────────────┐
│   Controller Layer (REST)           │
│   - AuthController                  │
│   - Exception Handling              │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│   Service Layer (Business Logic)    │
│   - AuthServiceImpl                 │
│   - Input Validation                │
│   - Password Hashing                │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│   Repository Layer (Data Access)    │
│   - UserRepository (JPA)            │
│   - H2 Database                     │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│   Utility Layer                     │
│   - JwtTokenProvider                │
│   - Security Configuration          │
└─────────────────────────────────────┘
```

---

## 🚀 Getting Started

### Prerequisites

- **Java 17** or higher
- **Maven 3.9+**
- **Docker** (optional, for containerized deployment)
- **Git**

### Clone the Repository

```bash
cd auth-service
```

### Build the Application

```bash
mvn clean install
```

### Run Locally

#### Option 1: Using Maven

```bash
mvn spring-boot:run
```

#### Option 2: Using JAR

```bash
mvn clean package
java -jar target/auth-service-0.0.1-SNAPSHOT.jar
```

#### Option 3: Using Docker

```bash
docker build -t auth-service:latest .
docker run -p 8081:8081 auth-service:latest
```

### Verify Service is Running

```bash
curl http://localhost:8081/auth/health
```

**Expected Response:**

```json
{
	"status": "UP",
	"service": "auth-service"
}
```

---

## 📡 API Endpoints

### Base URL

```
http://localhost:8081
```

### Swagger UI

```
http://localhost:8081/swagger-ui.html
```

### OpenAPI Docs

```
http://localhost:8081/v3/api-docs
```

---

## 🔌 REST API Reference

### 1. User Registration

**Endpoint:** `POST /auth/register`

**Description:** Register a new user account

**Request Body:**

```json
{
	"username": "johndoe",
	"email": "john@example.com",
	"password": "password123",
	"fullName": "John Doe"
}
```

**Success Response (201 Created):**

```json
{
	"token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
	"username": "johndoe",
	"userId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Error Responses:**

- **409 Conflict** - User already exists

  ```json
  {
  	"timestamp": "2026-02-26T10:30:00",
  	"status": 409,
  	"error": "Conflict",
  	"message": "User with this email already exists"
  }
  ```

- **400 Bad Request** - Invalid input
  ```json
  {
  	"timestamp": "2026-02-26T10:30:00",
  	"status": 400,
  	"error": "Bad Request",
  	"message": "Password must be at least 6 characters"
  }
  ```

**cURL Example:**

```bash
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john@example.com",
    "password": "password123",
    "fullName": "John Doe"
  }'
```

---

### 2. User Login

**Endpoint:** `POST /auth/login`

**Description:** Authenticate user and receive JWT token

**Request Body:**

```json
{
	"email": "john@example.com",
	"password": "password123"
}
```

**Success Response (200 OK):**

```json
{
	"token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
	"username": "johndoe",
	"userId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Error Response (401 Unauthorized):**

```json
{
	"timestamp": "2026-02-26T10:30:00",
	"status": 401,
	"error": "Unauthorized",
	"message": "Invalid email or password"
}
```

**cURL Example:**

```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "password123"
  }'
```

---

### 3. Validate Token

**Endpoint:** `POST /auth/validate`

**Description:** Validate JWT token and extract user information

**Headers:**

```
Authorization: Bearer <jwt-token>
```

**Success Response (200 OK):**

```json
{
	"valid": true,
	"userId": "550e8400-e29b-41d4-a716-446655440000",
	"username": "johndoe"
}
```

**Error Response (401 Unauthorized):**

```json
{
	"valid": false,
	"message": "Invalid or expired token"
}
```

**cURL Example:**

```bash
curl -X POST http://localhost:8081/auth/validate \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### 4. Health Check

**Endpoint:** `GET /auth/health`

**Description:** Check service health status

**Response (200 OK):**

```json
{
	"status": "UP",
	"service": "auth-service"
}
```

**cURL Example:**

```bash
curl http://localhost:8081/auth/health
```

---

## ⚙️ Configuration

### Application Properties

Located at: `src/main/resources/application.properties`

#### Server Configuration

```properties
server.port=8081
spring.application.name=auth-service
```

#### Database Configuration (H2)

```properties
spring.datasource.url=jdbc:h2:mem:authdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

#### JWT Configuration

```properties
app.jwt.secret=${JWT_SECRET:your-super-secret-key-change-in-production-env}
app.jwt.expiration=86400000
app.jwt.issuer=food-ordering-system
```

#### OpenAPI/Swagger Configuration

```properties
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.enabled=true
```

### Environment Variables

| Variable                | Description                | Default                    | Required        |
| ----------------------- | -------------------------- | -------------------------- | --------------- |
| `JWT_SECRET`            | Secret key for JWT signing | `your-super-secret-key...` | Production: Yes |
| `SPRING_DATASOURCE_URL` | Database connection URL    | `jdbc:h2:mem:authdb`       | No              |
| `SERVER_PORT`           | Service port               | `8081`                     | No              |

### Production Configuration

For production deployment, create `.env` file:

```bash
JWT_SECRET=<your-256-bit-secret-key>
SPRING_DATASOURCE_URL=jdbc:postgresql://db-host:5432/authdb
SPRING_DATASOURCE_USERNAME=dbuser
SPRING_DATASOURCE_PASSWORD=dbpassword
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
```

---

## 🗄️ Database Schema

### User Table

```sql
CREATE TABLE users (
    id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    role VARCHAR(50) DEFAULT 'USER' NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

### Entity Relationships

```
┌─────────────────────────────────────┐
│            User Entity               │
├─────────────────────────────────────┤
│ + id: String (UUID)                  │
│ + username: String (unique)          │
│ + email: String (unique)             │
│ + passwordHash: String               │
│ + fullName: String                   │
│ + role: String (USER|ADMIN)          │
│ + isActive: boolean                  │
│ + createdAt: LocalDateTime           │
│ + updatedAt: LocalDateTime           │
└─────────────────────────────────────┘
```

### Sample Data (For Testing)

Access H2 Console at: `http://localhost:8081/h2-console`

**Connection Details:**

- **JDBC URL:** `jdbc:h2:mem:authdb`
- **Username:** `sa`
- **Password:** _(leave empty)_

---

## 🔒 Security

### JWT Token Structure

**Header:**

```json
{
	"alg": "HS256",
	"typ": "JWT"
}
```

**Payload:**

```json
{
	"userId": "550e8400-e29b-41d4-a716-446655440000",
	"username": "johndoe",
	"roles": ["USER"],
	"sub": "johndoe",
	"iat": 1708956000,
	"exp": 1709042400,
	"iss": "food-ordering-system"
}
```

### Password Security

- **Algorithm:** BCrypt
- **Strength:** 10 rounds
- **Minimum Length:** 6 characters
- **Storage:** Hashed passwords only (never plain text)

### Token Expiration

- **Default:** 24 hours (86400000 milliseconds)
- **Configurable:** Via `app.jwt.expiration` property
- **Renewal:** Implement refresh token mechanism (future)

### Public Endpoints

The following endpoints are accessible without authentication:

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/validate`
- `GET /auth/health`
- `/actuator/**` (health checks)
- `/h2-console/**` (development only)
- `/swagger-ui/**` (API documentation)

---

## 🧪 Testing

### Manual Testing with cURL

#### 1. Register a New User

```bash
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "test123",
    "fullName": "Test User"
  }'
```

#### 2. Login with Credentials

```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "test123"
  }'
```

#### 3. Validate JWT Token

```bash
# Save token from previous response
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

curl -X POST http://localhost:8081/auth/validate \
  -H "Authorization: Bearer $TOKEN"
```

### Testing with Postman

1. Import OpenAPI specification from `/v3/api-docs`
2. Create environment variables:
   - `BASE_URL`: `http://localhost:8081`
   - `TOKEN`: _(save from login response)_
3. Use collection runner for automated testing

### Unit Testing (Maven)

```bash
mvn test
```

### Integration Testing

```bash
mvn verify
```

---

## 🐳 Deployment

### Docker Deployment

#### Build Image

```bash
docker build -t auth-service:1.0.0 .
```

#### Run Container

```bash
docker run -d \
  --name auth-service \
  -p 8081:8081 \
  -e JWT_SECRET="your-production-secret-key" \
  auth-service:1.0.0
```

#### Using Docker Compose

Create `docker-compose.yml`:

```yaml
version: "3.8"

services:
  auth-service:
    build: .
    ports:
      - "8081:8081"
    environment:
      - JWT_SECRET=${JWT_SECRET}
      - SPRING_PROFILES_ACTIVE=prod
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/auth/health"]
      interval: 30s
      timeout: 10s
      retries: 3
```

Run:

```bash
docker-compose up -d
```

### Azure Container Apps Deployment

See [AZURE_DEPLOYMENT_GUIDE.md](../docs/AZURE_DEPLOYMENT_GUIDE.md) for detailed Azure deployment instructions.

### Kubernetes Deployment

Create `k8s-deployment.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: auth-service
  template:
    metadata:
      labels:
        app: auth-service
    spec:
      containers:
        - name: auth-service
          image: ghcr.io/yourorg/auth-service:latest
          ports:
            - containerPort: 8081
          env:
            - name: JWT_SECRET
              valueFrom:
                secretKeyRef:
                  name: auth-secrets
                  key: jwt-secret
---
apiVersion: v1
kind: Service
metadata:
  name: auth-service
spec:
  selector:
    app: auth-service
  ports:
    - port: 8081
      targetPort: 8081
  type: ClusterIP
```

Deploy:

```bash
kubectl apply -f k8s-deployment.yaml
```

---

## 🔧 Troubleshooting

### Common Issues

#### 1. Port Already in Use

**Error:** `Web server failed to start. Port 8081 was already in use.`

**Solution:**

```bash
# Find process using port 8081
netstat -ano | findstr :8081   # Windows
lsof -i :8081                  # Mac/Linux

# Kill the process or change port
server.port=8082
```

#### 2. JWT Token Invalid

**Error:** `Invalid JWT token` or `Expired JWT token`

**Solutions:**

- Verify token hasn't expired (24 hours default)
- Check `JWT_SECRET` matches between services
- Ensure Bearer prefix: `Authorization: Bearer <token>`
- Validate token structure using [jwt.io](https://jwt.io)

#### 3. User Already Exists

**Error:** `User with this email already exists`

**Solution:**

- Use different email/username
- Delete existing user from H2 console
- Reset database: `spring.jpa.hibernate.ddl-auto=create-drop`

#### 4. Password Too Short

**Error:** `Password must be at least 6 characters`

**Solution:**

- Ensure password is 6+ characters
- Update validation in `AuthServiceImpl` if needed

#### 5. H2 Console Not Accessible

**Error:** Cannot access `/h2-console`

**Solution:**

```properties
# Verify these properties are set:
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

Also check Security Configuration allows H2 console access.

---

## 📊 Monitoring & Observability

### Health Endpoints

```bash
# Service health
curl http://localhost:8081/actuator/health

# Detailed health
curl http://localhost:8081/actuator/health/liveness
curl http://localhost:8081/actuator/health/readiness
```

### Metrics

```bash
# Application metrics
curl http://localhost:8081/actuator/metrics

# JVM memory
curl http://localhost:8081/actuator/metrics/jvm.memory.used
```

### Logging

Logs are output to console by default. Configure log levels:

```properties
logging.level.root=INFO
logging.level.com.example.authservice=DEBUG
logging.level.org.springframework.security=DEBUG
```

---

## 📚 Additional Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Security](https://spring.io/projects/spring-security)
- [JJWT Library](https://github.com/jwtk/jjwt)
- [H2 Database](https://www.h2database.com/)
- [OpenAPI Specification](https://swagger.io/specification/)

---

## 🤝 Contributing

This is a submission project for CTSE Assignment. For team contributions:

1. Create feature branch: `git checkout -b feature/your-feature`
2. Commit changes: `git commit -m 'Add some feature'`
3. Push to branch: `git push origin feature/your-feature`
4. Submit pull request for review

---

## 📄 License

This project is part of SLIIT CTSE Assignment - Year 4 Semester 2

---

## 👥 Team

**Course:** SE4010 - Current Trends in Software Engineering  
**Institution:** SLIIT (Sri Lanka Institute of Information Technology)  
**Academic Year:** 2025/2026

---

## 📞 Support

For issues or questions:

- Create an issue in the repository
- Contact team members via Slack/Teams
- Email: ctse@sliit.lk

---

**Last Updated:** February 26, 2026  
**Version:** 1.0.0  
**Status:** ✅ Production Ready
