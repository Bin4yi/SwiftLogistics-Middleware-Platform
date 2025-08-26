# services/driver-service/Dockerfile
FROM openjdk:11-jre-slim

# Set working directory
WORKDIR /app

# Copy the jar file
COPY target/driver-service-1.0.0.jar app.jar

# Create uploads directory for profile pictures and proof of delivery photos
RUN mkdir -p /app/uploads/drivers

# Expose port
EXPOSE 8083

# Add health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8083/api/drivers/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

# Optional: Add environment variable for profile
# ENV SPRING_PROFILES_ACTIVE=docker