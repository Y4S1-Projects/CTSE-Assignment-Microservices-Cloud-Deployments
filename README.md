# 🍽️ Food Ordering System - Microservices Architecture

**Course:** SE4010 - Current Trends in Software Engineering  
**Academic Year:** Year 4 Semester 2  
**Institution:** SLIIT (Sri Lanka Institute of Information Technology)

> **🚨 IMPORTANT FOR UNIVERSITY ACCOUNTS:**
>
> - Use [AZURE_DEPLOYMENT_GUIDE.md](AZURE_DEPLOYMENT_GUIDE.md) as the single deployment guide
> - Recommended path: `git push origin main` → `az login` → `./deploy-from-github.ps1`

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Architecture](#-architecture)
- [Services Status](#-services-status)
- [Quick Start](#-quick-start)
- [API Documentation](#-api-documentation)
- [Technology Stack](#-technology-stack)
- [Service Documentation](#-service-documentation)
- [Development](#-development)
- [Deployment](#-deployment)
- [Testing](#-testing)

---

## 🎯 Overview

A production-ready microservices architecture for a food ordering system built with Spring Boot 3 and Spring Cloud Gateway. The system demonstrates modern cloud-native patterns including:

- API Gateway with JWT authentication
- Service-to-service communication
- Rate limiting and CORS
- Distributed tracing and logging
- Docker containerization
- CI/CD with GitHub Actions
- Azure Container Apps deployment

---

## 🏗️ Architecture

```
┌─────────────┐
│   Clients   │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────────┐
│       API Gateway (Port 8080)           │
│  • JWT Authentication                   │
│  • Rate Limiting (100 req/min)          │
│  • Request Logging                      │
│  • CORS Configuration                   │
└────────┬────────────────────────────────┘
         │
         ├──────────┬──────────┬──────────┐
         ▼          ▼          ▼          ▼
    ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐
    │  Auth  │ │Catalog │ │ Order  │ │Payment │
    │ (8081) │ │ (8082) │ │ (8083) │ │ (8084) │
    └────────┘ └────────┘ └────────┘ └────────┘
         │          │          │          │
         └──────────┴──────────┴──────────┘
                     │
                ┌────▼────┐
                │   H2    │
                │Database │
                └─────────┘
```

---

## ✅ Services Status

| Service             | Port | Status              | Features                                       |
| ------------------- | ---- | ------------------- | ---------------------------------------------- |
| **API Gateway**     | 8080 | ✅ Production Ready | JWT Auth, Rate Limiting, Logging, CORS         |
| **Auth Service**    | 8081 | ✅ Production Ready | User Registration, Login, JWT Token Generation |
| **Catalog Service** | 8082 | 🔄 In Development   | Menu Items Management                          |
| **Order Service**   | 8083 | 🔄 In Development   | Order Processing, Tracking                     |
| **Payment Service** | 8084 | 🔄 In Development   | Payment Processing                             |

---

## 🚀 Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.9+
- Docker & Docker Compose (optional)
- Git

### Option 1: Run with Maven (Development)

```bash
# Clone the repository
git clone <repository-url>
cd CTSE-Assignment-Microservices-Cloud-Deployments

# Build all services
mvn clean install

# Terminal 1 - API Gateway
cd api-gateway
mvn spring-boot:run

# Terminal 2 - Auth Service
cd auth-service
mvn spring-boot:run
```

### Option 2: Run with Docker Compose

```bash
# Build and start all services
docker-compose up --build

# Stop all services
docker-compose down
```

### Verify Services

```bash
# Check API Gateway health
curl http://localhost:8080/actuator/health

# Check Auth Service health
curl http://localhost:8081/actuator/health
```

---

## 📚 API Documentation

### Swagger UI Access

All services are documented with **Swagger (OpenAPI 3.0)**:

| Service          | Swagger UI                                                                     | OpenAPI JSON                                                           |
| ---------------- | ------------------------------------------------------------------------------ | ---------------------------------------------------------------------- |
| **API Gateway**  | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) |
| **Auth Service** | [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html) | [http://localhost:8081/v3/api-docs](http://localhost:8081/v3/api-docs) |

### How to Use Swagger

1. **Start services:** `docker-compose up --build`
2. **Open browser:** Click links above
3. **Test endpoints:** Click "Try it out" button on any endpoint
4. **Authorize:** Click green "Authorize" button and paste JWT token for protected endpoints

### Auth Service Endpoints

```bash
# Register a new user
POST http://localhost:8081/auth/register
Content-Type: application/json

{
  "username": "john.doe",
  "email": "john.doe@example.com",
  "password": "password123",
  "fullName": "John Doe"
}

# Login
POST http://localhost:8081/auth/login
Content-Type: application/json

{
  "email": "john.doe@example.com",
  "password": "password123"
}

# Validate Token
POST http://localhost:8081/auth/validate
Authorization: Bearer <your-jwt-token>
```

---

## 🛠️ Technology Stack

### Backend Framework

- **Spring Boot 3.x** - Application framework
- **Spring Cloud Gateway 2024.0.0** - API Gateway
- **Spring Security** - Authentication & Authorization
- **Spring Data JPA** - Data persistence

### Libraries

- **JJWT 0.12.3** - JWT token generation & validation
- **Bucket4j 8.10.1** - Rate limiting
- **SpringDoc OpenAPI 3.0** - API documentation
- **Lombok** - Boilerplate code reduction
- **BCrypt** - Password hashing

### Database

- **H2 Database** - In-memory database (development)
- **PostgreSQL** - Production database (planned)

### DevOps & Cloud

- **Docker** - Containerization
- **Docker Compose** - Local orchestration
- **GitHub Actions** - CI/CD pipeline
- **Azure Container Apps** - Cloud deployment
- **Maven** - Build automation

---

## 📖 Service Documentation

### API Gateway (Port 8080) ✅

**[See api-gateway/README.md](api-gateway/README.md)**

Full technical documentation including:

- Complete implementation details
- All filters and configurations
- Setup and running instructions
- Testing guide with examples

### Auth Service (Port 8081) ✅

**[See auth-service/README.md](auth-service/README.md)**

Full technical documentation including:

- Complete implementation details
- JWT token handling
- User registration and login flows
- Database schema and entities
- Setup and running instructions

---

## 💻 Development

### Project Structure

```
CTSE-Assignment-Microservices-Cloud-Deployments/
├── api-gateway/              # API Gateway service
│   ├── src/main/java/com/example/apigateway/
│   │   ├── filter/           # JWT, Rate Limit, Logging filters
│   │   ├── config/           # CORS, OpenAPI configurations
│   │   ├── controller/       # Health endpoints
│   │   └── util/             # JWT validator
│   └── pom.xml
├── auth-service/             # Authentication service
│   ├── src/main/java/com/example/authservice/
│   │   ├── entity/           # User entity
│   │   ├── dto/              # Request/Response DTOs
│   │   ├── repository/       # JPA repositories
│   │   ├── service/          # Business logic
│   │   ├── controller/       # REST controllers
│   │   ├── config/           # Security, OpenAPI config
│   │   └── util/             # JWT token provider
│   └── pom.xml
├── catalog-service/          # Menu catalog service
├── order-service/            # Order management service
├── payment-service/          # Payment processing service
├── docker-compose.yml        # Local container orchestration
└── README.md                 # This file
```

### Environment Variables

#### Auth Service & API Gateway

```bash
# Required for JWT authentication
JWT_SECRET=your-super-secret-jwt-key-change-this-in-production-min-256-bits
```

#### Database Configuration (Production)

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/foodordering
SPRING_DATASOURCE_USERNAME=dbuser
SPRING_DATASOURCE_PASSWORD=dbpassword
```

### Building the Project

```bash
# Build all services
mvn clean install

# Build specific service
cd auth-service
mvn clean package

# Run tests
mvn test

# Skip tests
mvn clean install -DskipTests
```

---

## ☁️ Deployment

### Main Deployment Flow (University-Account Friendly)

Use this 3-step flow. It does not require creating a service principal:

```powershell
# 1) Push code (GitHub Actions builds and pushes images to ghcr.io)
git push origin main

# 2) Login to Azure with your account
az login

# 3) Deploy using built images
./deploy-from-github.ps1
```

### Deployment Guide and Scripts

- Single source of truth: [AZURE_DEPLOYMENT_GUIDE.md](AZURE_DEPLOYMENT_GUIDE.md)
- Main script: `deploy-from-github.ps1`

### Verify Deployment

```powershell
# Get gateway URL
az containerapp show --name api-gateway --resource-group ctse-microservices-rg --query properties.configuration.ingress.fqdn -o tsv

# Health check
curl https://<gateway-url>/health

# Auth health via gateway
curl https://<gateway-url>/auth/health
```

### Local Docker Compose (Development)

```bash
docker-compose up --build
docker-compose down
```

---

## 🧪 Testing

### Run Unit Tests

```bash
# All services
mvn test

# Specific service
cd auth-service
mvn test
```

### Manual API Testing

```bash
# 1. Register a user
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123",
    "fullName": "Test User"
  }'

# 2. Login and get JWT token
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'

# 3. Validate token
curl -X POST http://localhost:8081/auth/validate \
  -H "Authorization: Bearer <your-jwt-token>"
```

### Using Swagger UI

1. Start the services
2. Open Swagger UI: http://localhost:8081/swagger-ui.html
3. Click "Try it out" on any endpoint
4. Fill in request body
5. Click "Execute"

---

## 🔒 Security

### JWT Authentication

- **Algorithm:** HMAC-SHA256
- **Token Expiry:** 24 hours
- **Password Hashing:** BCrypt (strength 10)

### Rate Limiting

- **Limit:** 100 requests per minute per IP
- **Implementation:** Bucket4j with in-memory cache
- **Response:** 429 Too Many Requests

### CORS Configuration

- **Allowed Origins:** All origins in development
- **Allowed Methods:** GET, POST, PUT, DELETE, PATCH, OPTIONS
- **Allowed Headers:** All headers
- **Max Age:** 3600 seconds

### Security Best Practices

✅ Passwords hashed with BCrypt  
✅ JWT tokens signed and verified  
✅ Environment variables for secrets  
✅ HTTPS recommended in production  
✅ Rate limiting enabled  
✅ Input validation on all endpoints

---

## 📝 License

This project is developed for academic purposes as part of the CTSE (Current Trends in Software Engineering) course at SLIIT.

---

## 👥 Contributors

SLIIT Year 4 Semester 2 Students (SE4010)

---

## 📞 Support

For issues and questions:

- Review documentation in [docs/](docs/)
- Check Swagger UI for API details
- Contact course instructors

---

## 🗺️ Roadmap

### ✅ Completed

- [x] API Gateway with filters
- [x] Auth Service with JWT
- [x] Swagger documentation
- [x] Docker containerization
- [x] CI/CD pipeline setup

### 🔄 In Progress

- [ ] Catalog Service implementation
- [ ] Order Service implementation
- [ ] Payment Service implementation

### 📅 Planned

- [ ] PostgreSQL integration
- [ ] Service-to-service communication
- [ ] Distributed tracing
- [ ] Load testing
- [ ] Production deployment to Azure

---

**Last Updated:** February 2026  
**Project Status:** Active Development
