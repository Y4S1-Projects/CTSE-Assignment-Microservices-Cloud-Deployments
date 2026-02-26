# Service Configuration Templates

Template files and configurations for customization.

## Environment Configuration Template

Create `.env.local` file in the project root:

```bash
# JWT Configuration
JWT_SECRET=your-super-secret-key-change-in-production-env
JWT_EXPIRATION=86400000

# Service URLs (for local development)
SERVICE_AUTH_URL=http://auth-service:8081
SERVICE_CATALOG_URL=http://catalog-service:8082
SERVICE_ORDER_URL=http://order-service:8083
SERVICE_PAYMENT_URL=http://payment-service:8084

# Azure Configuration (for cloud deployment)
AZURE_CLIENT_ID=your-client-id
AZURE_TENANT_ID=your-tenant-id
AZURE_SUBSCRIPTION_ID=your-subscription-id
AZURE_RESOURCE_GROUP=ctse-rg

# GHCR Configuration
GITHUB_USERNAME=your-username
GITHUB_TOKEN=your-pat-token
GITHUB_ORGANIZATION=your-organization
```

## Port Configuration

Default ports for each service:

| Service | Port | Health Endpoint |
|---------|------|-----------------|
| API Gateway | 8080 | http://localhost:8080/actuator/health |
| Auth Service | 8081 | http://localhost:8081/actuator/health |
| Catalog Service | 8082 | http://localhost:8082/actuator/health |
| Order Service | 8083 | http://localhost:8083/actuator/health |
| Payment Service | 8084 | http://localhost:8084/actuator/health |

## Database Configuration

Each service uses H2 in-memory database by default:

### H2 Console Access

Add this to your browser after starting a service:
```
http://localhost:8081/h2-console (Auth Service)
http://localhost:8082/h2-console (Catalog Service)
http://localhost:8083/h2-console (Order Service)
http://localhost:8084/h2-console (Payment Service)
```

**Default H2 Credentials:**
- Driver Class: `org.h2.Driver`
- JDBC URL: `jdbc:h2:mem:<service-name>db`
- User: `sa`
- Password: (empty)

### Switching to a Real Database

For production deployment, update `application.properties`:

```properties
# PostgreSQL Example
spring.datasource.url=jdbc:postgresql://host:5432/orderdb
spring.datasource.username=postgres
spring.datasource.password=your-password
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate
```

## Swagger/OpenAPI URLs

Each service provides interactive API documentation:

- Auth Service: http://localhost:8081/swagger-ui.html
- Catalog Service: http://localhost:8082/swagger-ui.html
- Order Service: http://localhost:8083/swagger-ui.html
- Payment Service: http://localhost:8084/swagger-ui.html

OpenAPI JSON specs:
- Auth Service: http://localhost:8081/v3/api-docs
- Catalog Service: http://localhost:8082/v3/api-docs
- Order Service: http://localhost:8083/v3/api-docs
- Payment Service: http://localhost:8084/v3/api-docs

## Logging Configuration

Customize logging in `application.properties`:

```properties
# Root logging level
logging.level.root=INFO

# Service-specific logging
logging.level.com.example.authservice=DEBUG
logging.level.com.example.catalogservice=DEBUG
logging.level.com.example.orderservice=DEBUG
logging.level.com.example.paymentservice=DEBUG

# Framework logging
logging.level.org.springframework=INFO
logging.level.org.springframework.web=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.org.hibernate=WARN

# Logging pattern
logging.pattern.console=%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
logging.pattern.file=%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
```

## Docker Build Configuration

### Build All Services

```bash
#!/bin/bash
SERVICES=("api-gateway" "auth-service" "catalog-service" "order-service" "payment-service")

for service in "${SERVICES[@]}"; do
  echo "Building $service..."
  docker build -t food-ordering/$service:latest ./$service
done
```

### Push to Registry

```bash
#!/bin/bash
GITHUB_USERNAME="your-username"
SERVICES=("api-gateway" "auth-service" "catalog-service" "order-service" "payment-service")

for service in "${SERVICES[@]}"; do
  echo "Pushing $service..."
  docker tag food-ordering/$service:latest ghcr.io/$GITHUB_USERNAME/food-ordering/$service:latest
  docker push ghcr.io/$GITHUB_USERNAME/food-ordering/$service:latest
done
```

## JWT Token Generation (for Testing)

Use https://jwt.io to generate test tokens:

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
  "username": "testuser",
  "roles": ["USER"],
  "iat": 1700000000,
  "exp": 1800000000
}
```

**Secret:** Use your JWT_SECRET value

## Testing Endpoints with cURL

### Register a User

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john@example.com",
    "password": "secure123",
    "fullName": "John Doe"
  }'
```

### Login

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "secure123"
  }'
```

### Get Menu Items

```bash
TOKEN="your-jwt-token"

curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/catalog/items
```

### Create Order

```bash
curl -X POST http://localhost:8080/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {"itemId": "item-id-1", "quantity": 2},
      {"itemId": "item-id-2", "quantity": 1}
    ]
  }'
```

### Process Payment

```bash
curl -X POST http://localhost:8080/payments/charge \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "order-id",
    "amount": 250.50
  }'
```

## Maven Build Commands

### Build Single Service

```bash
cd auth-service
mvn clean package
mvn spring-boot:run
```

### Build All Services

```bash
mvn clean install
```

### Run Tests

```bash
mvn test
mvn verify
```

### Generate Project Docs

```bash
mvn site
mvn site:deploy
```

## GitHub Actions Secrets Configuration

Add these secrets to your GitHub repository (Settings → Secrets and variables → Actions):

```yaml
# Azure Configuration
AZURE_CLIENT_ID: "Your Azure Client ID"
AZURE_TENANT_ID: "Your Azure Tenant ID"
AZURE_SUBSCRIPTION_ID: "Your Azure Subscription ID"

# GitHub Registry
GHCR_USERNAME: "your-github-username"
GHCR_TOKEN: "your-github-pat"

# SonarCloud (Optional)
SONAR_TOKEN: "Your SonarCloud Token"
SONAR_HOST_URL: "https://sonarcloud.io"
SONAR_PROJECT_KEY: "your_organization_your_project"
SONAR_ORGANIZATION: "your_sonarcloud_org"

# Snyk (Optional)
SNYK_TOKEN: "Your Snyk Token"

# Azure Deployment
AZURE_RESOURCE_GROUP: "ctse-rg"
SERVICE_URL: "https://your-api-gateway-url"
```

## Application Properties by Environment

### Development (default)

```properties
spring.jpa.hibernate.ddl-auto=create-drop
spring.h2.console.enabled=true
logging.level.com.example=DEBUG
```

### Testing

```properties
spring.jpa.hibernate.ddl-auto=create
spring.datasource.url=jdbc:h2:mem:test
logging.level.com.example=INFO
```

### Production

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.h2.console.enabled=false
spring.datasource.hikari.maximum-pool-size=10
logging.level.com.example=WARN
spring.devtools.restart.enabled=false
```

## Load Testing with Apache JMeter

Create `load-test.jmx` for testing:

```xml
<!-- Example: Test order creation endpoint under load -->
<HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="Create Order">
  <protocol>http</protocol>
  <domain>localhost</domain>
  <port>8080</port>
  <path>/orders</path>
  <method>POST</method>
  <Header name="Authorization">Bearer ${JWT_TOKEN}</Header>
  <Header name="Content-Type">application/json</Header>
</HTTPSamplerProxy>
```

## Useful Commands Reference

```bash
# Maven
mvn clean install
mvn test
mvn spring-boot:run
mvn package -DskipTests

# Docker
docker-compose up --build
docker-compose down
docker logs container-name
docker exec -it container-name bash

# Azure CLI
az login
az group list
az containerapp list -g resource-group
az containerapp logs show -n service-name -g resource-group

# Git
git clone <repo-url>
git checkout -b feature/feature-name
git add .
git commit -m "Descriptive message"
git push origin feature/feature-name
```

---

**Last Updated**: February 2026
**Status**: Configuration Ready
