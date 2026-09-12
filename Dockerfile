# Multi-stage Dockerfile for EDOTS Spring Boot Backend
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY edots-domain edots-domain
COPY edots-event-engine edots-event-engine
COPY edots-notification edots-notification
COPY edots-webhook edots-webhook
COPY edots-scheduler edots-scheduler
COPY edots-api edots-api

RUN chmod +x gradlew && ./gradlew :edots-api:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/edots-api/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
