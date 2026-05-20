# ── Stage 1: Build ────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

# Copy POM first to leverage Docker layer caching for dependency downloads
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build, skipping tests (tests run in CI, not in Docker build)
COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

# Create a dedicated non-root user for security
RUN groupadd --system booker && useradd --system --gid booker booker

WORKDIR /app

# Copy the fat JAR from the build stage
COPY --from=build /workspace/target/*.jar app.jar

# Set ownership
RUN chown booker:booker app.jar

USER booker

# Expose the application port (Spring Boot default under context path /api)
EXPOSE 8080

# JVM tuning: use container-aware memory settings
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
