# Food Ordering Microservices System - CTSE Assignment

A professional 4-microservice architecture for a food ordering application built with Spring Boot 3, Spring Cloud, and deployed to Azure Container Apps.

## 📋 Project Overview

This is a cloud-native microservices implementation of a Food Ordering/Canteen System with the following components:

- **API Gateway**: Entry point with JWT authentication and request routing
- **Auth Service**: User authentication, JWT token generation, and JWKS endpoint
- **Catalog Service**: Menu item management and inventory
- **Order Service**: Order creation and status tracking
- **Payment Service**: Payment processing and notification handling

## 🏗️ Architecture

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       ▼
┌─────────────────────┐
│  API Gateway        │ (8080)
│  - JWT Validation   │
│  - Route Requests   │
└─────────────────────┘
   │  │  │  │
   ▼  ▼  ▼  ▼
┌──────────────────────────────────────────────────┐
│        Individual Microservices                  │
├──────────────────────────────────────────────────┤
│ Auth Service │ Catalog │ Order │ Payment Service │
│   (8081)     │(8082)   │(8083) │    (8084)       │
└──────────────────────────────────────────────────┘
```

## 🚀 Quick Start (Local Development)

### Prerequisites

- Java 17+
- Maven 3.9+
- Docker & Docker Compose (optional, for containerized setup)

### Build All Services

```bash
# Build each service
cd api-gateway && mvn clean install && cd ..
cd auth-service && mvn clean install && cd ..
cd catalog-service && mvn clean install && cd ..
cd order-service && mvn clean install && cd ..
cd payment-service && mvn clean install && cd ..
```

### Run with Docker Compose

```bash
docker-compose up --build
```

Services will be available at:
- API Gateway: http://localhost:8080
- Auth Service: http://localhost:8081
- Catalog Service: http://localhost:8082
- Order Service: http://localhost:8083
- Payment Service: http://localhost:8084

### Run Individual Services

Each service can be run independently:

```bash
cd api-gateway
mvn spring-boot:run

# In separate terminals:
cd auth-service && mvn spring-boot:run
cd catalog-service && mvn spring-boot:run
cd order-service && mvn spring-boot:run
cd payment-service && mvn spring-boot:run
```

## 📚 Service Details

### 1. Auth Service (Port 8081)

**Endpoints:**
- `POST /auth/register` - User registration
- `POST /auth/login` - User login (returns JWT token)
- `GET /users/me` - Get current user profile (JWT required)
- `GET /auth/jwks` - Get public keys for token verification

**Database**: H2 (in-memory)
**Features**:
- JWT token generation and validation
- User registration and authentication
- Role-based access control (USER, ADMIN)

### 2. Catalog Service (Port 8082)

**Endpoints:**
- `GET /catalog/items` - List all menu items
- `GET /catalog/items/{id}` - Get specific item
- `GET /catalog/items/category/{category}` - Filter by category
- `PATCH /catalog/items/{id}/availability` - Update availability (ADMIN)

**Database**: H2 (in-memory)
**Features**:
- Menu item management
- Availability tracking
- Category filtering

### 3. Order Service (Port 8083)

**Endpoints:**
- `POST /orders` - Create new order (JWT required)
- `GET /orders/{id}` - Get order details
- `GET /orders/my` - Get user's orders (JWT required)
- `PATCH /orders/{id}/status` - Update order status (ADMIN)

**Database**: H2 (in-memory)
**Features**:
- Order creation with item validation
- Integration with Catalog Service (Feign client)
- Order status tracking
- Support for payment integration

**Integration Points**:
- Calls Catalog Service to validate items and get prices
- Called by Payment Service to update status

### 4. Payment Service (Port 8084)

**Endpoints:**
- `POST /payments/charge` - Process payment (JWT required)
- `GET /payments/{orderId}` - Get payment status

**Database**: H2 (in-memory)
**Features**:
- Mock payment processing
- Payment status tracking
- Integration with Order Service (Feign client)

**Integration Points**:
- Calls Order Service to update order status to PAID

### 5. API Gateway (Port 8080)

**Public Endpoints**:
- `/auth/**` - Routes to Auth Service
- `/actuator/health` - Health check

**Protected Endpoints** (require JWT):
- `/catalog/**` - Routes to Catalog Service
- `/orders/**` - Routes to Order Service
- `/payments/**` - Routes to Payment Service

**Features**:
- Gateway-level JWT authentication
- Request routing
- Load balancing
- Circuit breaker for downstream services

## 🔐 Security Configuration

### Environment Variables

Each service requires the following environment variable:

```bash
JWT_SECRET=your-super-secret-key-change-in-production-env
```

For Cloud deployment, set this in Azure Container Apps secrets.

### JWT Token Structure

```json
{
  "userId": "user-id-uuid",
  "username": "username",
  "roles": ["USER"],
  "iat": 1234567890,
  "exp": 1234654290
}
```

### Service-to-Service Communication

Feign clients with built-in resilience:
- Circuit breaker pattern with Resilience4j
- Timeout: 3 seconds
- Retry attempts with exponential backoff
- Health check integration

## 🐳 Docker & Containerization

Each service includes a multi-stage Dockerfile for optimized builds:

```dockerfile
# Stage 1: Build with Maven
# Stage 2: Runtime with minimal JDK image
```

Build single service image:

```bash
cd order-service
docker build -t ghcr.io/yourusername/order-service:latest .
```

## 📊 API Documentation

Each service exposes OpenAPI/Swagger documentation:

- Auth Service: http://localhost:8081/swagger-ui.html
- Catalog Service: http://localhost:8082/swagger-ui.html
- Order Service: http://localhost:8083/swagger-ui.html
- Payment Service: http://localhost:8084/swagger-ui.html

## 🚀 Cloud Deployment (Azure Container Apps)

### Prerequisites

- Azure CLI installed
- Azure account with active subscription
- GitHub Personal Access Token (PAT) for GHCR access

### Setup Steps

```bash
# 1. Log in to Azure
az login

# 2. Create resource group
az group create -n ctse-rg -l southeastasia

# 3. Create Container Apps environment
az containerapp env create \
  -n ctse-env \
  -g ctse-rg \
  -l southeastasia

# 4. For each service, build and push to GHCR
docker build -t ghcr.io/yourusername/order-service:latest ./order-service
docker push ghcr.io/yourusername/order-service:latest

# 5. Deploy to Container Apps
az containerapp create \
  -n order-service \
  -g ctse-rg \
  --environment ctse-env \
  --image ghcr.io/yourusername/order-service:latest \
  --ingress external \
  --target-port 8080 \
  --env-vars JWT_SECRET="your-secret" \
           SERVICE_CATALOG_URL="http://catalog-service:8082"
```

## 🔄 CI/CD Pipeline

GitHub Actions workflow (`.github/workflows/deploy.yml`) includes:

1. **Build & Test**: Maven compilation and JUnit tests
2. **Security Scanning**:
   - SonarCloud (SAST - Static Application Security Testing)
   - Snyk (Dependency vulnerability scanning)
   - Trivy (Container image scanning)
3. **Build Docker Image**: Multi-stage Dockerfile
4. **Push to GHCR**: GitHub Container Registry
5. **Deploy to Azure**: Container Apps update
6. **Smoke Test**: Health endpoint verification

### Required GitHub Secrets

```
SONAR_TOKEN              # SonarCloud token
SONAR_HOST_URL          # SonarCloud instance
SONAR_PROJECT_KEY       # Project key in SonarCloud
SONAR_ORGANIZATION      # Organization in SonarCloud
SNYK_TOKEN              # Snyk token (optional)
AZURE_CLIENT_ID         # Azure OIDC client ID
AZURE_TENANT_ID         # Azure tenant ID
AZURE_SUBSCRIPTION_ID   # Azure subscription ID
AZURE_RESOURCE_GROUP    # Azure resource group name
SERVICE_URL             # Deployed service URL for smoke tests
```

## 📝 Database Schema (H2)

### Users Table
```sql
CREATE TABLE users (
  id UUID PRIMARY KEY,
  username VARCHAR(100) NOT NULL UNIQUE,
  email VARCHAR(150) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(255),
  role VARCHAR(50) DEFAULT 'USER',
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  is_active BOOLEAN DEFAULT TRUE
);
```

### Menu Items Table
```sql
CREATE TABLE items (
  id UUID PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  price DECIMAL(10,2) NOT NULL,
  availability VARCHAR(50) DEFAULT 'AVAILABLE',
  category VARCHAR(100),
  image_url VARCHAR(500),
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);
```

### Orders Table
```sql
CREATE TABLE orders (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  total_amount DECIMAL(12,2),
  status VARCHAR(50) DEFAULT 'CREATED',
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);
```

### Payments Table
```sql
CREATE TABLE payments (
  id UUID PRIMARY KEY,
  order_id UUID NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  status VARCHAR(50) DEFAULT 'PENDING',
  reference VARCHAR(255),
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);
```

## 🔄 Service Integration Flow

### Order Creation Flow

1. Client calls: `POST /orders` (via Gateway) with JWT token
2. API Gateway validates JWT and routes to Order Service
3. Order Service:
   - Extracts userId from JWT
   - Calls Catalog Service to validate items and get prices
   - Calculates total amount
   - Saves order with status "CREATED"
   - Returns OrderResponse

### Payment Processing Flow

1. Client calls: `POST /payments/charge` (via Gateway) with payment details
2. API Gateway validates JWT and routes to Payment Service
3. Payment Service:
   - Validates payment request
   - Simulates payment processing
   - Saves payment record with status "SUCCESS"
   - Calls Order Service to update order status to "PAID"
   - Returns PaymentResponse

## 🧪 Implementation Checklist

- [ ] **Phase 1: Core Services**
  - [ ] Auth Service implementation (login, register, JWT generation)
  - [ ] Catalog Service implementation (CRUD operations)
  - [ ] Order Service implementation (order creation, listing)
  - [ ] Payment Service implementation (payment processing)

- [ ] **Phase 2: Integration**
  - [ ] Feign client setup for inter-service calls
  - [ ] Resilience4j circuit breaker configuration
  - [ ] Error handling and retry logic
  - [ ] Logging and correlation IDs

- [ ] **Phase 3: Security**
  - [ ] JWT token validation across all services
  - [ ] RBAC implementation (USER, ADMIN roles)
  - [ ] API Gateway JWT filter
  - [ ] Secrets management (no hardcoded keys)

- [ ] **Phase 4: DevOps**
  - [ ] Dockerfile optimization
  - [ ] Docker Compose for local development
  - [ ] GitHub Actions workflow setup
  - [ ] SonarCloud/Snyk integration
  - [ ] Azure deployment scripts

- [ ] **Phase 5: Testing & Documentation**
  - [ ] Unit tests (CRUD, business logic)
  - [ ] Integration tests (service-to-service)
  - [ ] API documentation (Swagger/OpenAPI)
  - [ ] Architecture documentation
  - [ ] Deployment runbooks

## 📊 Monitoring & Health Checks

All services expose health endpoints:

```bash
# Health check
curl http://localhost:8080/actuator/health

# Detailed health
curl http://localhost:8080/actuator/health/liveness
curl http://localhost:8080/actuator/health/readiness

# Metrics
curl http://localhost:8080/actuator/metrics
```

## 🤝 Team Collaboration

### Code Structure
- Each service has independent Java package structure
- Controllers → Services → Repositories pattern (DDD)
- DTOs for API contracts
- Entities for database models

### API Contracts
- OpenAPI specs in `springdoc-openapi`
- Consistent request/response formats
- Error handling with HTTP status codes

### Dependency Management
- Maven BOM for Spring Cloud dependencies
- Resilience4j for circuit breakers
- JJWT for JWT operations
- H2 for embedded database

## 🐛 Troubleshooting

### Service fails to start
1. Check if port is already in use: `netstat -ano | findstr :8080`
2. Verify Java version: `java -version` (should be 17+)
3. Check Maven: `mvn -version`
4. Clean Maven cache: `mvn clean install`

### Connection refused between services
1. Ensure all services are running
2. Check service URLs in `application.properties`
3. Verify Docker network if using Docker Compose

### JWT token validation fails
1. Ensure `JWT_SECRET` environment variable is set
2. Check token format: should be `Bearer <token>`
3. Verify token expiration

## 📖 Additional Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)
- [Azure Container Apps](https://learn.microsoft.com/en-us/azure/container-apps/)
- [OpenAPI/Swagger](https://www.openapis.org/)
- [Resilience4j](https://resilience4j.readme.io/)

## 📄 License

This project is part of CTSE Assignment 1 (2026) at SLIIT.

## 📞 Support

For issues and questions, please refer to the HELP.md files in each service directory.

---

**Last Updated**: February 2026
**Status**: Ready for Feature Implementation
