# 🎯 Auth Service - Quick Start Guide

## ⚡ Quick Commands

### 1. Build the Service

```bash
cd auth-service
mvn clean package -DskipTests
```

### 2. Run the Service

#### Option A: Using Maven

```bash
mvn spring-boot:run
```

#### Option B: Using JAR

```bash
java -jar target/auth-service-0.0.1-SNAPSHOT.jar
```

#### Option C: Using Docker

```bash
docker build -t auth-service:latest .
docker run -p 8081:8081 auth-service:latest
```

### 3. Verify Service is Running

```bash
curl http://localhost:8081/auth/health
```

---

## 🧪 Test the API

### Automated Testing (Recommended)

**PowerShell (Windows):**

```powershell
cd auth-service
.\test-api.ps1
```

**Bash (Linux/Mac):**

```bash
cd auth-service
chmod +x test-api.sh
./test-api.sh
```

### Manual Testing with cURL

#### Register a New User

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

**Response:**

```json
{
	"token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
	"username": "johndoe",
	"userId": "550e8400-e29b-41d4-a716-446655440000"
}
```

#### Login

```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "password123"
  }'
```

#### Validate Token

```bash
# Replace <TOKEN> with the actual token from login/register response
curl -X POST http://localhost:8081/auth/validate \
  -H "Authorization: Bearer <TOKEN>"
```

---

## 📚 Access Documentation

### Swagger UI (Interactive API Testing)

```
http://localhost:8081/swagger-ui.html
```

### OpenAPI JSON

```
http://localhost:8081/v3/api-docs
```

### H2 Database Console (Development)

```
http://localhost:8081/h2-console
```

**Connection:**

- JDBC URL: `jdbc:h2:mem:authdb`
- Username: `sa`
- Password: _(leave empty)_

---

## 🔧 Configuration

### Environment Variables

For production deployment, set these environment variables:

```bash
# Required for production
export JWT_SECRET="your-256-bit-secret-key-change-this-in-production"

# Optional (defaults shown)
export SERVER_PORT=8081
export SPRING_DATASOURCE_URL=jdbc:h2:mem:authdb
```

### Docker Environment

```bash
docker run -p 8081:8081 \
  -e JWT_SECRET="your-production-secret" \
  auth-service:latest
```

---

## 📊 Key Endpoints Summary

| Method | Endpoint           | Description            | Auth Required |
| ------ | ------------------ | ---------------------- | ------------- |
| POST   | `/auth/register`   | Register new user      | ❌            |
| POST   | `/auth/login`      | User login             | ❌            |
| POST   | `/auth/validate`   | Validate JWT token     | ✅ (Bearer)   |
| GET    | `/auth/health`     | Service health check   | ❌            |
| GET    | `/actuator/health` | Spring actuator health | ❌            |
| GET    | `/swagger-ui.html` | API documentation      | ❌            |

---

## 🐛 Troubleshooting

### Port 8081 Already in Use

```bash
# Windows: Find and kill process
netstat -ano | findstr :8081
taskkill /PID <process_id> /F

# Linux/Mac: Find and kill process
lsof -i :8081
kill -9 <process_id>

# Or change port in application.properties
server.port=8082
```

### Build Failures

```bash
# Clean Maven cache
mvn clean install -U

# Skip tests if they fail
mvn clean package -DskipTests
```

### JWT Token Issues

- Ensure `JWT_SECRET` is the same across all services
- Token expires after 24 hours by default
- Use format: `Authorization: Bearer <token>`

---

## 📦 What's Included

✅ **Fully Implemented:**

- User registration with password hashing (BCrypt)
- User login with JWT token generation
- Token validation and user extraction
- Exception handling (401, 409, 400, 500)
- Spring Security configuration
- Swagger/OpenAPI documentation
- H2 database (development)
- Health check endpoints
- Docker support
- Automated test scripts

✅ **Production Ready:**

- Proper error handling
- Input validation
- Security best practices
- Transaction management
- Logging
- Actuator health checks

---

## 🚀 Next Steps

1. **Test Locally**: Run the service and test with curl or Swagger UI
2. **Integrate**: Connect other microservices to use this auth service
3. **Deploy**: Use Docker image or deploy to Azure Container Apps
4. **Monitor**: Check health endpoints and logs

---

## 📖 Full Documentation

See [README.md](README.md) for complete documentation including:

- Detailed API reference
- Database schema
- Security configuration
- Deployment guides
- Architecture diagrams

---

**Service Port:** 8081  
**Status:** ✅ Production Ready  
**Version:** 1.0.0
