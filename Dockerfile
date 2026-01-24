# Multi-stage build for Spring Boot application
# Stage 1: Build the application
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build the application
COPY backend/src ./src
RUN mvn clean package -DskipTests -T 1C

# Stage 2: Run the application
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser
# Create cache directory for Spring AI models
RUN mkdir -p /tmp/spring-ai-onnx-generative && chown -R appuser:appuser /tmp/spring-ai-onnx-generative
RUN chown -R appuser:appuser /app
USER appuser

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application with optimized JVM options for faster startup
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+TieredCompilation", \
  "-XX:TieredStopAtLevel=1", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
