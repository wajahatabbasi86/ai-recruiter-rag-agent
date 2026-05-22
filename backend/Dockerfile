# ── Stage 1: dependency cache ─────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS deps
WORKDIR /app
# Copy ONLY pom.xml first — this layer is cached until pom.xml changes
COPY pom.xml .
RUN mvn dependency:go-offline -B

# ── Stage 2: build ────────────────────────────────────────────
FROM deps AS build
# Now copy source — changes here don't invalidate the deps layer
COPY src ./src
RUN mvn package -DskipTests -B -q

# ── Stage 3: runtime ──────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=build /app/target/*.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]