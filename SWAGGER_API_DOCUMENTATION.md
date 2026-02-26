# 📘 Swagger API Documentation Guide

Complete guide for accessing and using Swagger UI across all microservices.

---

## 🎯 Overview

All services in the Food Ordering System use **SpringDoc OpenAPI 3.0** (Swagger) for comprehensive API documentation. Each service has its own Swagger UI accessible at the service's port.

### Technology Stack

- **SpringDoc OpenAPI:** Version 2.6.0
- **OpenAPI Specification:** 3.0
- **UI Framework:** Swagger UI
- **Auth Service:** Uses `springdoc-openapi-starter-webmvc-ui` (Spring MVC)
- **API Gateway:** Uses `springdoc-openapi-starter-webflux-ui` (Spring WebFlux/Reactive)

---

## 🚀 Quick Access

### Auth Service (Port 8081)

| Resource         | URL                                    |
| ---------------- | -------------------------------------- |
| **Swagger UI**   | http://localhost:8081/swagger-ui.html  |
| **OpenAPI JSON** | http://localhost:8081/v3/api-docs      |
| **OpenAPI YAML** | http://localhost:8081/v3/api-docs.yaml |

### API Gateway (Port 8080)

| Resource         | URL                                    |
| ---------------- | -------------------------------------- |
| **Swagger UI**   | http://localhost:8080/swagger-ui.html  |
| **OpenAPI JSON** | http://localhost:8080/v3/api-docs      |
| **OpenAPI YAML** | http://localhost:8080/v3/api-docs.yaml |

---

## 📋 Auth Service API Documentation

### Starting Auth Service

```bash
cd auth-service
mvn spring-boot:run
```

### Access Swagger UI

Open in browser: **http://localhost:8081/swagger-ui.html**

### Available Endpoints

#### 1. **POST /auth/register**

Register a new user account

**Request Body:**

```json
{
	"username": "johndoe",
	"email": "john@example.com",
	"password": "password123",
	"fullName": "John Doe"
}
```

**Response (201 Created):**

```json
{
	"token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
	"username": "johndoe",
	"userId": "550e8400-e29b-41d4-a716-446655440000"
}
```

#### 2. **POST /auth/login**

Login to get JWT token

**Request Body:**

```json
{
	"email": "john@example.com",
	"password": "password123"
}
```

**Response (200 OK):**

```json
{
	"token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
	"username": "johndoe",
	"userId": "550e8400-e29b-41d4-a716-446655440000"
}
```

#### 3. **POST /auth/validate**

Validate a JWT token

**Request Body:**

```json
{
	"token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response (200 OK):**

```json
{
	"valid": true,
	"userId": "550e8400-e29b-41d4-a716-446655440000",
	"username": "johndoe",
	"roles": ["USER"]
}
```

#### 4. **GET /auth/health**

Health check endpoint

**Response (200 OK):**

```json
{
	"status": "UP",
	"service": "auth-service",
	"timestamp": "2026-02-26T23:30:00"
}
```

---

## 🌐 API Gateway Documentation

### Starting API Gateway

```bash
cd api-gateway
mvn spring-boot:run
```

### Access Swagger UI

Open in browser: **http://localhost:8080/swagger-ui.html**

### Gateway Features Documented

- **JWT Authentication Filter** - Token validation on protected routes
- **Rate Limiting** - 100 requests per minute per IP
- **CORS Configuration** - Cross-origin request handling
- **Request Logging** - Request/response tracking
- **Health Endpoints** - Service status monitoring

### Testing Through API Gateway

All auth service endpoints can be accessed through the gateway:

#### Via Gateway (Recommended)

```bash
# Register through gateway
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@example.com","password":"password123","fullName":"Test User"}'

# Login through gateway
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

#### Direct to Service

```bash
# Register directly
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@example.com","password":"password123","fullName":"Test User"}'
```

---

## 🔒 Using JWT Authentication in Swagger UI

### Step 1: Get a Token

1. Open Swagger UI for Auth Service
2. Navigate to **POST /auth/login**
3. Click **"Try it out"**
4. Enter credentials:

```json
{
	"email": "test@example.com",
	"password": "password123"
}
```

5. Click **"Execute"**
6. Copy the `token` from the response

### Step 2: Authorize in Swagger

1. Click the **"Authorize"** button (lock icon) at top right
2. In the dialog, enter: `Bearer <your-token>`
   - Example: `Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`
3. Click **"Authorize"**
4. Click **"Close"**

### Step 3: Test Protected Endpoints

Now all requests will include the Authorization header automatically!

---

## 🎨 Swagger UI Features

### Interactive API Testing

✅ **Try It Out** - Execute API calls directly from the browser  
✅ **Request Bodies** - Pre-filled examples for all endpoints  
✅ **Response Codes** - See all possible response codes (200, 401, 409, etc.)  
✅ **Schemas** - View data models and structures  
✅ **Authorization** - Built-in JWT token management  
✅ **Download** - Export OpenAPI spec as JSON/YAML

### Swagger UI Sections

1. **Endpoints List** - All available API endpoints grouped by tags
2. **Models/Schemas** - Data structures (DTOs, Entities)
3. **Authorization** - Configure Bearer token authentication
4. **Servers** - Available server URLs

---

## 📝 OpenAPI Specification Details

### Auth Service OpenAPI Config

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI authServiceOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Auth Service API")
                .version("1.0.0")
                .description("Authentication and User Management Service")
                .contact(new Contact()
                    .name("SLIIT CTSE Team")
                    .email("team@example.com")))
            .addSecurityItem(new SecurityRequirement()
                .addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Enter JWT token")));
    }
}
```

### API Gateway OpenAPI Config

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiGatewayOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("API Gateway")
                .version("1.0.0")
                .description("Central API Gateway for Food Ordering System")
                .contact(new Contact()
                    .name("SLIIT CTSE Team")
                    .email("team@example.com")))
            .addSecurityItem(new SecurityRequirement()
                .addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
```

---

## 🔧 Configuration

### Auth Service (application.properties)

```properties
# OpenAPI / Swagger Configuration
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.enabled=true
springdoc.swagger-ui.operations-sorter=method
```

### API Gateway (application.properties)

```properties
# OpenAPI / Swagger Configuration
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.enabled=true
springdoc.swagger-ui.operations-sorter=method
```

---

## 🐳 Docker Access

When running services in Docker containers:

### Auth Service (Docker)

```bash
# Access from host machine
http://localhost:8081/swagger-ui.html

# Access from another container
http://auth-service:8081/swagger-ui.html
```

### API Gateway (Docker)

```bash
# Access from host machine
http://localhost:8080/swagger-ui.html

# Access from another container
http://api-gateway:8080/swagger-ui.html
```

---

## 🧪 Testing Workflow

### Complete Test Flow Using Swagger UI

#### 1. Start Services

```bash
# Terminal 1: Auth Service
cd auth-service
mvn spring-boot:run

# Terminal 2: API Gateway
cd api-gateway
mvn spring-boot:run
```

#### 2. Register New User

- Open http://localhost:8081/swagger-ui.html
- Navigate to **POST /auth/register**
- Click **"Try it out"**
- Execute with sample data

#### 3. Login and Get Token

- Navigate to **POST /auth/login**
- Login with registered credentials
- **Copy the token from response**

#### 4. Authorize Swagger

- Click **"Authorize"** button (top right)
- Enter: `Bearer <your-token>`
- Click **"Authorize"** then **"Close"**

#### 5. Test Protected Endpoints

- All subsequent requests will include JWT automatically
- Try **POST /auth/validate** to verify token

#### 6. Test Through Gateway

- Open http://localhost:8080/swagger-ui.html
- Repeat steps 2-5 through gateway
- Verify requests are routed correctly

---

## 🔍 Troubleshooting

### Issue: Swagger UI Not Loading

**Check:**

1. Service is running: `curl http://localhost:8081/actuator/health`
2. Correct port: Auth Service (8081), API Gateway (8080)
3. SpringDoc dependency in pom.xml
4. Configuration in application.properties

**Solution:**

```bash
# Rebuild the service
mvn clean package -DskipTests
mvn spring-boot:run
```

### Issue: 401 Unauthorized on Protected Endpoints

**Check:**

1. Token is valid and not expired
2. Token is in correct format: `Bearer <token>`
3. Authorization header is set in Swagger

**Solution:**

- Re-login to get fresh token
- Click "Authorize" and re-enter token

### Issue: CORS Errors in Browser

**Check:**

- CORS configuration in Auth Service SecurityConfig
- Allowed origins include your frontend URL

**Solution:**

```java
// In SecurityConfig.java
http.cors(cors -> cors.configurationSource(request -> {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("http://localhost:3000", "*"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
    config.setAllowedHeaders(List.of("*"));
    return config;
}));
```

---

## 📊 Comparison: Direct vs Gateway Access

| Feature            | Direct Access (Port 8081)     | Via Gateway (Port 8080)       |
| ------------------ | ----------------------------- | ----------------------------- |
| **URL**            | http://localhost:8081/auth/\* | http://localhost:8080/auth/\* |
| **JWT Validation** | Service validates             | Gateway validates first       |
| **Rate Limiting**  | None                          | 100 req/min per IP            |
| **Logging**        | Service logs                  | Gateway + Service logs        |
| **CORS**           | Service handles               | Gateway handles               |
| **Use Case**       | Development/Testing           | Production                    |

**Recommendation:** Use Gateway (port 8080) for production-like testing.

---

## 📚 Additional Resources

### SpringDoc OpenAPI Documentation

- Official Docs: https://springdoc.org/
- OpenAPI Specification: https://spec.openapis.org/oas/v3.0.0

### Swagger UI

- Swagger UI Guide: https://swagger.io/tools/swagger-ui/
- Customization: https://springdoc.org/#swagger-ui-properties

### JWT Authentication

- JWT.io: https://jwt.io/
- Best Practices: https://datatracker.ietf.org/doc/html/rfc8725

---

## ✅ Quick Verification Checklist

- [ ] Auth Service Swagger UI accessible at http://localhost:8081/swagger-ui.html
- [ ] API Gateway Swagger UI accessible at http://localhost:8080/swagger-ui.html
- [ ] Can register new user via Swagger
- [ ] Can login and receive JWT token
- [ ] Can authorize Swagger UI with JWT token
- [ ] Can validate token via Swagger
- [ ] All endpoints documented with request/response examples
- [ ] Both JSON and YAML OpenAPI specs accessible

---

## 🎓 Best Practices

### For Development

1. **Use Swagger UI for all manual testing** - Faster than cURL/Postman
2. **Keep Swagger annotations up-to-date** - Document as you code
3. **Test both direct and gateway access** - Ensure routing works
4. **Export OpenAPI spec** - Share with frontend team

### For Production

1. **Disable Swagger UI** - `springdoc.swagger-ui.enabled=false`
2. **Keep OpenAPI JSON available** - For API clients
3. **Secure Swagger endpoints** - Require authentication
4. **Version your API** - Use proper versioning strategy

### Security Notes

⚠️ **Production Warning:** Swagger UI exposes all API endpoints. Consider:

- Disabling in production
- Requiring authentication to access Swagger
- Using API keys or basic auth for Swagger endpoint
- Documenting only public APIs

---

**Last Updated:** February 26, 2026  
**Version:** 1.0.0  
**Services Covered:** Auth Service, API Gateway  
**Next:** Catalog Service, Order Service, Payment Service

---

**Quick Start:**

```bash
# Start Auth Service
cd auth-service && mvn spring-boot:run

# Open Swagger UI
# Visit: http://localhost:8081/swagger-ui.html
```

🎉 **Happy API Testing with Swagger!**
