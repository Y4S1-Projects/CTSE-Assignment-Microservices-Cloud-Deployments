# 🌐 API Gateway - Food Ordering System

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud Gateway](https://img.shields.io/badge/Spring%20Cloud%20Gateway-2024.0.0-blue.svg)](https://spring.io/projects/spring-cloud-gateway)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> Central API Gateway for the Food Ordering Microservices System. Handles routing, authentication, rate limiting, CORS, and cross-cutting concerns.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [Routes Configuration](#routes-configuration)
- [Security](#security)
- [Rate Limiting](#rate-limiting)
- [CORS Configuration](#cors-configuration)
- [Logging](#logging)
- [Testing](#testing)
- [Deployment](#deployment)
- [Troubleshooting](#troubleshooting)

---

## 🎯 Overview

The API Gateway serves as the single entry point for all client requests to the microservices ecosystem. It provides:

- **Unified Entry Point**: Single endpoint for all microservices
- **Authentication & Authorization**: JWT token validation
- **Rate Limiting**: Protection against abuse and DDoS
- **CORS Handling**: Cross-origin resource sharing
- **Request/Response Logging**: Centralized logging
- **Load Balancing**: Distribute requests across service instances
- **Circuit Breaking**: Fault tolerance and resilience

### Technology Stack

| Component         | Technology           |
| ----------------- | -------------------- |
| **Framework**     | Spring Boot 3.x      |
| **Gateway**       | Spring Cloud Gateway |
| **Language**      | Java 17 (LTS)        |
| **Security**      | JWT (JJWT 0.12.3)    |
| **Rate Limiting** | Bucket4j 8.10.1      |
| **API Docs**      | SpringDoc OpenAPI 3  |
| **Build Tool**    | Maven 3.9+           |

---

## ✨ Features

### 🔒 Security Features

✅ **JWT Authentication**

- Token validation on protected endpoints
- User information extraction
- Role-based authorization support
- Automatic header enrichment for downstream services

✅ **Public Endpoints**

- `/auth/**` - Authentication service
- `/actuator/**` - Health checks
- `/swagger-ui/**` - API documentation

### 🚦 Traffic Management

✅ **Rate Limiting**

- 100 requests per minute per IP
- Bucket4j token bucket algorithm
- Automatic refill mechanism
- 429 Too Many Requests response

✅ **CORS Support**

- Configurable allowed origins
- Support for all HTTP methods
- Custom header handling
- Credentials support

### 📊 Observability

✅ **Request/Response Logging**

- Request method, path, and IP
- Response status codes
- Request duration tracking
- Structured logging format

✅ **Health Monitoring**

- Spring Boot Actuator
- Custom health endpoints
- Service status reporting

### 🔀 Routing

✅ **Service Routes**

- Auth Service: `/auth/**` → Port 8081
- Catalog Service: `/catalog/**` → Port 8082
- Order Service: `/orders/**` → Port 8083
- Payment Service: `/payments/**` → Port 8084

---

## 🏗️ Architecture

### Component Diagram

```
┌─────────────────────────────────────────────────────────┐
│                   API Gateway (Port 8080)                │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌────────────────────────────────────────────────┐    │
│  │         Global Filter Chain                    │    │
│  │  1. Logging Filter (Order: -3)                 │    │
│  │  2. Rate Limit Filter (Order: -2)              │    │
│  │  3. JWT Authentication Filter (Order: -1)      │    │
│  └────────────────────────────────────────────────┘    │
│                        │                                 │
│                        ▼                                 │
│  ┌────────────────────────────────────────────────┐    │
│  │           Route Predicates                      │    │
│  │  • /auth/** → Auth Service                      │    │
│  │  • /catalog/** → Catalog Service                │    │
│  │  • /orders/** → Order Service                   │    │
│  │  • /payments/** → Payment Service               │    │
│  └────────────────────────────────────────────────┘    │
│                        │                                 │
│                        ▼                                 │
│  ┌────────────────────────────────────────────────┐    │
│  │        Load Balancer (if configured)            │    │
│  └────────────────────────────────────────────────┘    │
│                                                          │
└──────────────────────┬───────────────────────────────────┘
                       │
       ┌───────────────┴───────────────┐
       │                               │
       ▼                               ▼
┌─────────────┐              ┌─────────────┐
│Auth Service │              │Catalog Svc  │
│  (8081)     │              │  (8082)     │
└─────────────┘              └─────────────┘
       │                               │
       ▼                               ▼
┌─────────────┐              ┌─────────────┐
│Order Service│              │Payment Svc  │
│  (8083)     │              │  (8084)     │
└─────────────┘              └─────────────┘
```

### Request Flow

```
1. Client Request → API Gateway (Port 8080)
2. Logging Filter → Log request details
3. Rate Limit Filter → Check rate limit
4. JWT Auth Filter → Validate token (if not public)
5. Route Predicate → Match route pattern
6. Load Balancer → Select service instance
7. Forward Request → Downstream service
8. Response Back → Through filter chain
9. Client Response → With added headers
```

---

## 🚀 Getting Started

### Prerequisites

- **Java 17** or higher
- **Maven 3.9+**
- **Docker** (optional)
- Running microservices (Auth, Catalog, Order, Payment)

### Clone and Build

```bash
cd api-gateway
mvn clean install
```

### Run Locally

#### Option 1: Using Maven

```bash
mvn spring-boot:run
```

#### Option 2: Using JAR

```bash
mvn clean package -DskipTests
java -jar target/api-gateway-0.0.1-SNAPSHOT.jar
```

#### Option 3: Using Docker

```bash
docker build -t api-gateway:latest .
docker run -p 8080:8080 api-gateway:latest
```

### Verify Gateway is Running

```bash
curl http://localhost:8080/health
```

**Expected Response:**

```json
{
	"status": "UP",
	"service": "api-gateway",
	"timestamp": "2026-02-26T22:30:00",
	"port": 8080
}
```

---

## 🔌 Routes Configuration

### Service Routes

| Route Pattern  | Target Service  | Port | Description                        |
| -------------- | --------------- | ---- | ---------------------------------- |
| `/auth/**`     | Auth Service    | 8081 | User authentication & registration |
| `/catalog/**`  | Catalog Service | 8082 | Menu items & catalog management    |
| `/orders/**`   | Order Service   | 8083 | Order creation & management        |
| `/payments/**` | Payment Service | 8084 | Payment processing                 |

### Configuration (application.properties)

```properties
# Auth Service Route
spring.cloud.gateway.routes[0].id=auth-service
spring.cloud.gateway.routes[0].uri=http://auth-service:8081
spring.cloud.gateway.routes[0].predicates[0]=Path=/auth/**

# Catalog Service Route
spring.cloud.gateway.routes[1].id=catalog-service
spring.cloud.gateway.routes[1].uri=http://catalog-service:8082
spring.cloud.gateway.routes[1].predicates[0]=Path=/catalog/**

# Order Service Route
spring.cloud.gateway.routes[2].id=order-service
spring.cloud.gateway.routes[2].uri=http://order-service:8083
spring.cloud.gateway.routes[2].predicates[0]=Path=/orders/**

# Payment Service Route
spring.cloud.gateway.routes[3].id=payment-service
spring.cloud.gateway.routes[3].uri=http://payment-service:8084
spring.cloud.gateway.routes[3].predicates[0]=Path=/payments/**
```

---

## 🔒 Security

### JWT Authentication

The API Gateway validates JWT tokens on all requests except public endpoints.

**Public Endpoints (No Authentication Required):**

- `/auth/register`
- `/auth/login`
- `/auth/validate`
- `/auth/health`
- `/actuator/**`
- `/swagger-ui/**`
- `/v3/api-docs/**`

**Protected Endpoints (JWT Required):**

- All other routes require valid JWT token

### Request Headers (Authenticated Requests)

**Client → Gateway:**

```
Authorization: Bearer <jwt-token>
```

**Gateway → Downstream Service:**

```
Authorization: Bearer <jwt-token>
X-User-Id: <user-id>
X-Username: <username>
X-User-Roles: <comma-separated-roles>
```

### Example: Making Authenticated Request

```bash
# 1. Login to get token
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}' \
  | jq -r '.token')

# 2. Use token to access protected endpoint
curl -X GET http://localhost:8080/catalog/items \
  -H "Authorization: Bearer $TOKEN"
```

### JWT Configuration

```properties
# JWT Secret (use strong key in production)
app.jwt.secret=${JWT_SECRET:your-super-secret-key-change-in-production-env}

# Token expiration (24 hours in milliseconds)
app.jwt.expiration=86400000
```

**⚠️ Production Warning:** Always use a strong, randomly generated secret key in production and store it securely as an environment variable.

---

## 🚦 Rate Limiting

### Configuration

- **Algorithm**: Token Bucket (Bucket4j)
- **Limit**: 100 requests per minute per IP address
- **Refill**: 100 tokens every minute
- **Response**: 429 Too Many Requests

### How It Works

```java
// Rate limit: 100 requests per minute
Bandwidth limit = Bandwidth.classic(
    100,  // capacity
    Refill.intervally(100, Duration.ofMinutes(1))
);
```

### Rate Limit Exceeded Response

```json
{
	"timestamp": "2026-02-26T22:30:00",
	"status": 429,
	"error": "Too Many Requests",
	"message": "Rate limit exceeded. Please try again later."
}
```

### Testing Rate Limiting

```bash
# Send 101 requests rapidly
for i in {1..101}; do
  curl http://localhost:8080/auth/health
  echo "Request $i"
done
```

After 100 requests, you'll receive 429 responses.

### Production Considerations

For distributed rate limiting across multiple gateway instances:

- Use Redis-based rate limiting
- Implement distributed token bucket
- Share rate limit state across instances

---

## 🌐 CORS Configuration

### Allowed Methods

- GET, POST, PUT, PATCH, DELETE, OPTIONS

### Allowed Headers

- Authorization
- Content-Type
- Accept
- X-Requested-With
- X-User-Id
- X-Username
- X-User-Roles

### Exposed Headers

- Authorization
- X-User-Id
- X-Username

### Configuration Details

```java
corsConfig.setAllowedOriginPatterns(List.of("*"));  // Development
corsConfig.setAllowCredentials(true);
corsConfig.setMaxAge(3600L);  // Cache preflight for 1 hour
```

**⚠️ Production:** Replace `"*"` with specific allowed origins:

```java
corsConfig.setAllowedOrigins(Arrays.asList(
    "https://yourdomain.com",
    "https://app.yourdomain.com"
));
```

---

## 📝 Logging

### Log Levels

```properties
logging.level.root=INFO
logging.level.com.example=DEBUG
logging.pattern.console=%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
```

### Sample Log Output

```
22:30:15.123 [reactor-http-nio-2] INFO  LoggingFilter - → Incoming request: POST /auth/login from 192.168.1.100
22:30:15.234 [reactor-http-nio-2] INFO  JwtAuthenticationFilter - Public endpoint accessed: /auth/login
22:30:15.456 [reactor-http-nio-2] INFO  LoggingFilter - ← Response: POST /auth/login - Status: 200 - Duration: 333ms
```

### Filter Execution Order

1. **LoggingFilter** (Order: -3) - First to execute
2. **RateLimitFilter** (Order: -2) - Second
3. **JwtAuthenticationFilter** (Order: -1) - Third

---

## 🧪 Testing

### 1. Health Check

```bash
curl http://localhost:8080/health
```

### 2. Root Endpoint (Gateway Info)

```bash
curl http://localhost:8080/
```

**Response:**

```json
{
	"service": "api-gateway",
	"version": "1.0.0",
	"description": "API Gateway for Food Ordering Microservices",
	"status": "running",
	"timestamp": "2026-02-26T22:30:00",
	"routes": {
		"Authentication": "/auth/**",
		"Catalog": "/catalog/**",
		"Orders": "/orders/**",
		"Payments": "/payments/**",
		"Health": "/health",
		"Actuator": "/actuator/health",
		"Swagger UI": "/swagger-ui.html",
		"API Docs": "/v3/api-docs"
	}
}
```

### 3. Test Authentication Flow

```bash
# Register user
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123",
    "fullName": "Test User"
  }'

# Save token from response
TOKEN="<token-from-response>"

# Access protected endpoint
curl -X GET http://localhost:8080/catalog/items \
  -H "Authorization: Bearer $TOKEN"
```

### 4. Test Rate Limiting

```bash
# Rapid requests (will hit rate limit)
for i in {1..105}; do
  curl http://localhost:8080/health
done
```

### 5. Test CORS

```bash
curl -X OPTIONS http://localhost:8080/auth/login \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: POST" \
  -v
```

---

## 🐳 Deployment

### Docker Deployment

#### Build Image

```bash
docker build -t api-gateway:1.0.0 .
```

#### Run Container

```bash
docker run -d \
  --name api-gateway \
  -p 8080:8080 \
  -e JWT_SECRET="your-production-secret-key" \
  -e SPRING_PROFILES_ACTIVE=prod \
  api-gateway:1.0.0
```

### Docker Compose

```yaml
version: "3.8"

services:
  api-gateway:
    build: ./api-gateway
    ports:
      - "8080:8080"
    environment:
      - JWT_SECRET=${JWT_SECRET}
      - SERVICE_AUTH_URL=http://auth-service:8081
      - SERVICE_CATALOG_URL=http://catalog-service:8082
      - SERVICE_ORDER_URL=http://order-service:8083
      - SERVICE_PAYMENT_URL=http://payment-service:8084
    depends_on:
      - auth-service
      - catalog-service
      - order-service
      - payment-service
    networks:
      - microservices-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/health"]
      interval: 30s
      timeout: 10s
      retries: 3

networks:
  microservices-network:
    driver: bridge
```

### Azure Container Apps

See [AZURE_DEPLOYMENT_GUIDE.md](../AZURE_DEPLOYMENT_GUIDE.md) for complete Azure deployment instructions.

---

## 🔧 Troubleshooting

### Common Issues

#### 1. Gateway Cannot Connect to Services

**Error:** `Connection refused` or `503 Service Unavailable`

**Solutions:**

```bash
# Verify service URLs in application.properties
service.auth.url=http://auth-service:8081

# Check if services are running
curl http://localhost:8081/auth/health
curl http://localhost:8082/catalog/health
curl http://localhost:8083/orders/health
curl http://localhost:8084/payments/health

# Update docker-compose network
networks:
  - microservices-network
```

#### 2. JWT Validation Failing

**Error:** `Invalid or expired JWT token`

**Solutions:**

- Verify `JWT_SECRET` matches across Gateway and Auth Service
- Check token hasn't expired (24 hours default)
- Ensure correct Bearer token format: `Authorization: Bearer <token>`
- Validate token using `POST /auth/validate`

#### 3. CORS Issues

**Error:** `CORS policy: No 'Access-Control-Allow-Origin' header`

**Solutions:**

```java
// Update CorsConfig.java
corsConfig.setAllowedOrigins(Arrays.asList(
    "http://localhost:3000",
    "https://yourdomain.com"
));
```

#### 4. Rate Limit Too Restrictive

**Error:** 429 Too Many Requests

**Solutions:**

```java
// Increase rate limit in RateLimitConfig.java
Bandwidth limit = Bandwidth.classic(
    200,  // Increase from 100 to 200
    Refill.intervally(200, Duration.ofMinutes(1))
);
```

#### 5. Port Already in Use

**Error:** `Port 8080 already in use`

**Solutions:**

```bash
# Windows: Find and kill process
netstat -ano | findstr :8080
taskkill /PID <pid> /F

# Linux/Mac
lsof -i :8080
kill -9 <pid>

# Or change port
server.port=8081
```

---

## 📊 Monitoring

### Health Endpoints

```bash
# Gateway health
curl http://localhost:8080/health

# Spring Boot Actuator
curl http://localhost:8080/actuator/health

# Detailed metrics
curl http://localhost:8080/actuator/metrics
```

### Metrics to Monitor

- Request rate per endpoint
- Response times
- Error rates (4xx, 5xx)
- Rate limit hits
- JWT validation failures
- Service availability

---

## ⚙️ Configuration Reference

### Environment Variables

| Variable              | Description                   | Default                       | Required        |
| --------------------- | ----------------------------- | ----------------------------- | --------------- |
| `JWT_SECRET`          | Secret key for JWT validation | `your-super-secret-key...`    | Production: Yes |
| `SERVER_PORT`         | Gateway port                  | `8080`                        | No              |
| `SERVICE_AUTH_URL`    | Auth service URL              | `http://auth-service:8081`    | No              |
| `SERVICE_CATALOG_URL` | Catalog service URL           | `http://catalog-service:8082` | No              |
| `SERVICE_ORDER_URL`   | Order service URL             | `http://order-service:8083`   | No              |
| `SERVICE_PAYMENT_URL` | Payment service URL           | `http://payment-service:8084` | No              |

### Production Configuration

```bash
# .env file for production
JWT_SECRET=<strong-256-bit-secret>
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=prod
LOGGING_LEVEL_ROOT=WARN
LOGGING_LEVEL_COM_EXAMPLE=INFO
```

---

## 📚 Additional Resources

- [Spring Cloud Gateway Docs](https://spring.io/projects/spring-cloud-gateway)
- [JWT Best Practices](https://jwt.io/introduction)
- [Bucket4j Documentation](https://bucket4j.com/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

---

## 🤝 Contributing

This is a CTSE Assignment project. For team contributions:

1. Create feature branch
2. Make changes and test locally
3. Submit pull request for review

---

## 📄 License

This project is part of SLIIT CTSE Assignment - Year 4 Semester 2

---

## 👥 Team

**Course:** SE4010 - Current Trends in Software Engineering  
**Institution:** SLIIT  
**Academic Year:** 2025/2026

---

**Last Updated:** February 26, 2026  
**Version:** 1.0.0  
**Status:** ✅ Production Ready
