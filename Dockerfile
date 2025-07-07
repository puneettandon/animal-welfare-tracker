# Stage 1: React build
FROM node:20-alpine AS frontend-build
WORKDIR /app
COPY ui/package*.json ./ui/
RUN cd ui && npm install
COPY ui ./ui
RUN cd ui && npm run build

# Stage 2: Spring Boot build
FROM gradle:8.5-jdk21 AS backend-build
WORKDIR /app
COPY animal-welfare-tracker/ ./animal-welfare-tracker/
COPY --from=frontend-build /app/ui/build ./animal-welfare-tracker/src/main/resources/static/
RUN gradle animal-welfare-tracker:build -x test

# Stage 3: Runtime
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=backend-build /app/animal-welfare-tracker/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
