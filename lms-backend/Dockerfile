# Stage 1: Build với Maven & Java 21
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy file cấu hình trước để tận dụng caching
COPY pom.xml .
# Tải dependencies trước (giúp build nhanh hơn ở các lần sau)
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime với JRE (nhẹ hơn JDK)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install wget cho health checks
RUN apk add --no-cache wget

# Copy JAR từ build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Support JAVA_OPTS từ environment variable
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]