# services/order-service/Dockerfile
FROM openjdk:11-jre-slim

# Set working directory
WORKDIR /app

# Copy the jar file
COPY target/order-service-1.0.0.jar app.jar

# Expose port
EXPOSE 8081

# Add health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/api/orders/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]