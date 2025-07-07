# Stage 1: React build
FROM node:20-alpine AS frontend-build
WORKDIR /app/ui
COPY ui/package*.json ./
RUN npm install
COPY ui/ ./
RUN npm run build

# Stage 2: Spring Boot build
FROM gradle:8.5-jdk21 AS backend-build
WORKDIR /app

# Copy backend code
COPY animal-welfare-tracker/ ./animal-welfare-tracker/

# Copy React build output into Spring Boot's static resources (IMPORTANT)
COPY --from=frontend-build /app/ui/build/ ./animal-welfare-tracker/src/main/resources/static/

# Now build Spring Boot app
WORKDIR /app/animal-welfare-tracker
RUN gradle build -x test

# Stage 3: Runtime
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=backend-build /app/animal-welfare-tracker/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
