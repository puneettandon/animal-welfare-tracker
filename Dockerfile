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

# Copy full project (your current folder is the Gradle project root)
COPY . .

# Copy built React app into Spring Boot static resources
COPY --from=frontend-build /app/ui/build ./src/main/resources/static/

# Build the Spring Boot app
RUN gradle build -x test

# Stage 3: Runtime
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=backend-build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
