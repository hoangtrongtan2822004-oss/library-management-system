# Multi-stage Dockerfile for Railway deployment
# Stage 1: Build với Maven & Java 21
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy backend files
COPY lms-backend/pom.xml .
RUN mvn dependency:go-offline -B

COPY lms-backend/src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime với JRE
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install wget for health checks
RUN apk add --no-cache wget

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Railway provides PORT env var
ENV PORT=8080
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
