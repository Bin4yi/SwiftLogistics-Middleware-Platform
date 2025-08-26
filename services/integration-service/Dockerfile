
# services/integration-service/Dockerfile
FROM openjdk:11-jre-slim

# Set working directory
WORKDIR /app

# Copy the jar file
COPY target/integration-service-1.0.0.jar app.jar

# Expose port
EXPOSE 8082

# Add health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8082/api/integration/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]