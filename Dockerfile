# Stage 1: Build the React frontend
FROM node:20-alpine AS frontend-build
WORKDIR /app/ui
COPY ui/package.json ui/package-lock.json ./
RUN npm install
COPY ui/ ./
RUN npm run build

# Stage 2: Build the Spring Boot application
FROM gradle:8.5-jdk21 AS backend-build
WORKDIR /app # Set the working directory to the root of your entire repository within the container

# IMPORTANT CHANGE HERE: Use an explicit source for COPY, even if it's the current directory.
# Sometimes, explicitly stating './' instead of just '.' can resolve obscure context issues.
COPY --chown=gradle:gradle ./ /app # Copy the ENTIRE repository content to /app

# --- CRUCIAL STEP: Copy React build output into Spring Boot's static resources ---
# Source is /app/ui/build inside frontend-build container
# Destination is /app/animal-welfare-tracker/src/main/resources/static inside this container
COPY --from=frontend-build /app/ui/build /app/animal-welfare-tracker/src/main/resources/static

# IMPORTANT CHANGE HERE:
# We are running the Gradle command from the root WORKDIR /app.
# Therefore, we need to specify which sub-project to build.
# Assuming your Spring Boot project is named 'animal-welfare-tracker' in your settings.gradle at /app.
RUN gradle :animal-welfare-tracker:build -x test

# Stage 3: Create the final runtime image
FROM eclipse-temurin:21-jdk
WORKDIR /app
# Copy the built Spring Boot JAR from the 'backend-build' stage.
# The JAR will be in '/app/animal-welfare-tracker/build/libs/*.jar' from the backend-build stage.
COPY --from=backend-build /app/animal-welfare-tracker/build/libs/*.jar app.jar
EXPOSE 8080
ENV SERVER_PORT=${PORT:-8080}
ENTRYPOINT ["java", "-jar", "app.jar"]