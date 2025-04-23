# Stage 1: Build with Maven
FROM --platform=$BUILDPLATFORM maven:3.8.6-eclipse-temurin-11 as builder

WORKDIR /app
COPY pom.xml .
COPY src ./src

RUN mvn package -DskipTests

# Stage 2: Create final image
FROM --platform=$TARGETPLATFORM eclipse-temurin:11-jre-jammy

WORKDIR /app

# Copy the built jar from the builder stage
COPY --from=builder /app/target/jwt-grpc-server-*.jar /app/app.jar

# Copy any additional resources
COPY --from=builder /app/target/libs /app/libs

# Add health check
HEALTHCHECK --interval=30s --timeout=30s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Expose ports
EXPOSE 50051 8080

# Set environment variables
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]