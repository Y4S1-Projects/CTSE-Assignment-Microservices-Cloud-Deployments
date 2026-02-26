# Multi-stage build for Catalog Service
# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 as builder
WORKDIR /build
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=builder /build/target/*.jar catalog-service.jar
EXPOSE 8082
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 CMD wget -q -O- http://localhost:8082/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "catalog-service.jar"]
