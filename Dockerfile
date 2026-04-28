# ── Stage 1: Build ────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom first for layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build fat jar
COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: Run ──────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Render / Railway expose $PORT
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=${PORT}"]
