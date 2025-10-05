# Multi-stage Docker build for optimal image size
FROM maven:3.9-eclipse-temurin-23 AS builder

WORKDIR /app

# Copy POM file first for better layer caching
COPY pom.xml ./pom.xml

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Production image
FROM eclipse-temurin:23-jre-alpine

# Create non-root user for security
RUN addgroup -g 1001 weatherapi && \
    adduser -D -s /bin/sh -u 1001 -G weatherapi weatherapi

WORKDIR /app

# Copy the built JAR
COPY --from=builder /app/target/weather-api-*.jar app.jar

# Create logs directory
RUN mkdir -p logs && chown -R weatherapi:weatherapi /app

# Switch to non-root user
USER weatherapi

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# Environment variables
ENV SPRING_PROFILES_ACTIVE=docker
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
