# 🍽️ Food Ordering System - Microservices Architecture

**Course:** SE4010 - Current Trends in Software Engineering  
**Academic Year:** Year 4 Semester 2  
**Institution:** SLIIT (Sri Lanka Institute of Information Technology)

> **📌 NEW: [Azure Deployment Guide for University Accounts](AZURE_DEPLOYMENT_GUIDE.md)** - Comprehensive guide for deploying with university email restrictions

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

### Prerequisites

- **Docker** (version 20.10+)
- **Docker Compose** (version 1.29+)
- **Java 17+** (for local development)
- **Maven 3.9+** (for building)
- **Azure CLI** (for Azure deployment)

### Local Deployment (Docker Compose)

#### 1. Build All Services Locally

```bash
# Build all services
docker-compose build

# Build specific service
docker build -t api-gateway:latest ./api-gateway
docker build -t auth-service:latest ./auth-service
docker build -t catalog-service:latest ./catalog-service
docker build -t order-service:latest ./order-service
docker build -t payment-service:latest ./payment-service
```

#### 2. Run All Services

```bash
# Start all services in background
docker-compose up -d

# Start with logs visible
docker-compose up

# View logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f api-gateway
docker-compose logs -f auth-service
```

#### 3. Verify Services Running

```bash
# Check all containers
docker-compose ps

# Check API Gateway health
curl http://localhost:8080/actuator/health

# Check Auth Service health
curl http://localhost:8081/actuator/health
```

#### 4. Stop Services

```bash
# Stop all services
docker-compose down

# Remove volumes as well
docker-compose down -v

# Remove images
docker-compose down --rmi all
```

### Docker Optimization

✅ **Optimizations Applied:**

- **Multi-stage builds:** Maven builder (3.9.6) → Alpine runtime (17-jdk-alpine)
- **.dockerignore files:** Exclude unnecessary files (target, .git, .idea, node_modules, \*.log, docs, etc.)
- **Layer caching:** Dependency and source separation for better cache hits
- **Optimized base images:** Alpine Linux for minimal image size (~370MB per service)
- **Health checks:** All services configured with Spring Boot Actuator endpoints

### Manual Docker Build & Run

```bash
# Build individual service
cd api-gateway
docker build -t api-gateway:latest .

# Run service
docker run -d \
  --name api-gateway \
  -p 8080:8080 \
  -e JWT_SECRET=your-secret-key \
  api-gateway:latest

# Stop and remove
docker stop api-gateway
docker rm api-gateway
```

### Environment Variables for Docker

Set these in `docker-compose.yml` or at runtime:

| Variable            | Default                     | Description         |
| ------------------- | --------------------------- | ------------------- |
| JWT_SECRET          | secret-key-123              | JWT signing secret  |
| SERVICE_AUTH_URL    | http://auth-service:8081    | Auth Service URL    |
| SERVICE_CATALOG_URL | http://catalog-service:8082 | Catalog Service URL |
| SERVICE_ORDER_URL   | http://order-service:8083   | Order Service URL   |
| SERVICE_PAYMENT_URL | http://payment-service:8084 | Payment Service URL |

### Azure Container Apps Deployment

**Prerequisites:**

- Azure subscription
- Azure CLI installed
- Docker Hub or Azure Container Registry account

**Deployment Steps:**

1. **Login to Azure**

   ```bash
   az login
   ```

2. **Create Resource Group**

   ```bash
   az group create --name ctse-microservices-rg --location eastus
   ```

3. **Create Container Registry**

   ```bash
   az acr create --resource-group ctse-microservices-rg --name ctseregistry --sku Basic
   ```

4. **Build and Push Docker Images**

   ```bash
   # Login to ACR
   az acr login --name ctseregistry

   # Build images
   docker build -t api-gateway:latest ./api-gateway
   docker build -t auth-service:latest ./auth-service

   # Tag for registry
   docker tag api-gateway:latest ctseregistry.azurecr.io/api-gateway:latest
   docker tag auth-service:latest ctseregistry.azurecr.io/auth-service:latest

   # Push to registry
   docker push ctseregistry.azurecr.io/api-gateway:latest
   docker push ctseregistry.azurecr.io/auth-service:latest
   ```

5. **Create Container Apps Environment**

   ```bash
   az containerapp env create \
     --name ctse-env \
     --resource-group ctse-microservices-rg
   ```

6. **Deploy Services**

   Deploy all 5 services to Azure Container Apps:

   ```bash
   # deploy API Gateway
   az containerapp create \
     --name api-gateway \
     --resource-group ctse-microservices-rg \
     --environment ctse-env \
     --image ctseregistry.azurecr.io/api-gateway:latest \
     --target-port 8080 \
     --ingress external \
     --registry-server ctseregistry.azurecr.io \
     --environment-variables JWT_SECRET=your-secret-key SERVICE_AUTH_URL=http://auth-service:8081 SERVICE_CATALOG_URL=http://catalog-service:8082 SERVICE_ORDER_URL=http://order-service:8083 SERVICE_PAYMENT_URL=http://payment-service:8084

   # Deploy Auth Service
   az containerapp create \
     --name auth-service \
     --resource-group ctse-microservices-rg \
     --environment ctse-env \
     --image ctseregistry.azurecr.io/auth-service:latest \
     --target-port 8081 \
     --ingress internal \
     --registry-server ctseregistry.azurecr.io \
     --environment-variables JWT_SECRET=your-secret-key

   # Deploy Catalog Service
   az containerapp create \
     --name catalog-service \
     --resource-group ctse-microservices-rg \
     --environment ctse-env \
     --image ctseregistry.azurecr.io/catalog-service:latest \
     --target-port 8082 \
     --ingress internal \
     --registry-server ctseregistry.azurecr.io

   # Deploy Order Service
   az containerapp create \
     --name order-service \
     --resource-group ctse-microservices-rg \
     --environment ctse-env \
     --image ctseregistry.azurecr.io/order-service:latest \
     --target-port 8083 \
     --ingress internal \
     --registry-server ctseregistry.azurecr.io

   # Deploy Payment Service
   az containerapp create \
     --name payment-service \
     --resource-group ctse-microservices-rg \
     --environment ctse-env \
     --image ctseregistry.azurecr.io/payment-service:latest \
     --target-port 8084 \
     --ingress internal \
     --registry-server ctseregistry.azurecr.io
   ```

7. **Configure Ingress & Load Balancing**

   ```bash
   # Update API Gateway for external access
   az containerapp ingress update \
     --name api-gateway \
     --resource-group ctse-microservices-rg \
     --type external \
     --target-port 8080

   # Enable auto-scaling
   az containerapp update \
     --name api-gateway \
     --resource-group ctse-microservices-rg \
     --min-replicas 1 \
     --max-replicas 3
   ```

8. **Verify Deployment**

   ```bash
   # List all container apps
   az containerapp list \
     --resource-group ctse-microservices-rg \
     --output table

   # Check pod logs
   az containerapp logs show \
     --name api-gateway \
     --resource-group ctse-microservices-rg

   # Get container app URL
   az containerapp show \
     --name api-gateway \
     --resource-group ctse-microservices-rg \
     --query "properties.configuration.ingress.fqdn"
   ```

### Docker Compose File Reference

The complete `docker-compose.yml` includes:

- **5 services:** api-gateway, auth-service, catalog-service, order-service, payment-service
- **Multi-stage builds:** Maven builder + Alpine runtime for optimized images
- **Service networking:** microservices-network bridge for inter-service communication
- **Environment variables:** JWT_SECRET and service URLs
- **Health checks:** All services have health check endpoints configured
- **Port mappings:** 8080-8084 properly mapped
- **Dependencies:** Services started in correct order
- **Restart policies:** Automatic restart on failure

### CI/CD Pipeline

**GitHub Actions Implementation:**

The project includes automated CI/CD pipeline configured in `.github/workflows/deploy.yml`:

```yaml
# Automated workflow triggers on:
# - Push to main branch
# - Pull requests to main branch

Steps:
1. Checkout code
2. Setup Java 17
3. Run unit tests
4. Verify build (Maven)
5. Build Docker images for all 5 services
6. Push images to Azure Container Registry
7. Deploy to Azure Container Apps
8. Run smoke tests on deployed services
9. Send deployment notifications
```

**Pipeline Features:**

- ✅ **Automated builds** on every push to main
- ✅ **Test execution** for all services
- ✅ **Code quality checks** with Maven plugins
- ✅ **Security scanning** of dependencies
- ✅ **Docker image building** with multi-stage optimization
- ✅ **Registry push** to Azure Container Registry
- ✅ **Automatic deployment** to Azure Container Apps
- ✅ **Health checks** before marking deployment complete
- ✅ **Slack notifications** of build status

**Accessing Build Logs:**

```bash
# View Actions in GitHub
# https://github.com/your-org/CTSE-Assignment-Microservices/actions

# View deployment logs in Azure
az containerapp logs show \
  --name api-gateway \
  --resource-group ctse-microservices-rg \
  --follow
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
