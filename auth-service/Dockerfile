# Multi-stage build for Auth Service
# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 as builder
WORKDIR /build
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=builder /build/target/*.jar auth-service.jar
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 CMD wget -q -O- http://localhost:8081/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "auth-service.jar"]
