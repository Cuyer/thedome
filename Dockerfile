# syntax=docker/dockerfile:1

# --- Build Stage ---
FROM eclipse-temurin:21-jdk-alpine AS build

# Install build dependencies
RUN apk add --no-cache bash

# Set workdir
WORKDIR /app

# Copy Gradle wrapper and build scripts first for better caching
COPY --link gradlew gradlew
COPY --link build.gradle.kts settings.gradle.kts ./
COPY --link gradle gradle/

# Make gradlew executable
RUN chmod +x gradlew

# Pre-download dependencies (leverages Docker cache)
RUN ./gradlew dependencies --no-daemon || true

# Copy the rest of the source code
COPY --link src src
COPY --link src/main/resources src/main/resources

# Build the application (skip tests for faster build)
RUN ./gradlew installDist --no-daemon -x test

# --- Runtime Stage ---
FROM eclipse-temurin:21-jre-alpine AS runtime

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy built distribution from build stage
COPY --from=build /app/build/install/thedome /app

# Set permissions
RUN chown -R appuser:appgroup /app
USER appuser

# Expose the default port (can be overridden by PORT env var)
EXPOSE 8080

# Healthcheck (optional, can be adjusted)
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s CMD wget -qO- http://localhost:8080/servers || exit 1

# Entrypoint
ENTRYPOINT ["/app/bin/thedome"]
