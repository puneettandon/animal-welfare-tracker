# Stage 1: Build the React frontend
# We use a Node.js base image for this stage.
FROM node:20-alpine AS frontend-build

# Set the working directory inside the container for the frontend project.
# This WORKDIR is relative to the root of the container's build context.
# Since your 'ui' folder is at the root of your repo (where Dockerfile is),
# we'll work directly within the 'ui' folder inside the container.
WORKDIR /app/ui

# Copy package.json and package-lock.json from the host's 'ui' directory
# into the current working directory (/app/ui) inside the container.
COPY ui/package.json ui/package-lock.json ./

# Install Node.js dependencies.
RUN npm install

# Copy the rest of your React source code from the host's 'ui' directory
# into the current working directory (/app/ui) inside the container.
COPY ui/ ./

# Build the React application for production.
RUN npm run build

# Stage 2: Build the Spring Boot application
# We use a Gradle base image for this stage.
FROM gradle:8.5-jdk21 AS backend-build

# Set the working directory inside the container for the backend project.
# This will be the root of your Spring Boot project.
# Based on your structure, the Spring Boot project is inside 'animal-welfare-tracker/'
# relative to the Dockerfile.
WORKDIR /app/animal-welfare-tracker

# Copy the entire Spring Boot project from your host machine into this stage.
# This assumes your Spring Boot project is in 'animal-welfare-tracker/'
# relative to the Dockerfile.
COPY --chown=gradle:gradle animal-welfare-tracker/ ./

# --- CRUCIAL STEP: Copy React build output into Spring Boot's static resources ---
# This takes the 'build' directory created in the 'frontend-build' stage
# (which is located at /app/ui/build inside the frontend-build container)
# and places it into the 'src/main/resources/static' directory
# within your Spring Boot project's context (/app/animal-welfare-tracker/src/main/resources/static).
COPY --from=frontend-build /app/ui/build /app/src/main/resources/static

# Build the Spring Boot application using Gradle.
# '-x test' skips tests, which can speed up builds in CI/CD.
RUN gradle build -x test

# Stage 3: Create the final runtime image
# We use a lightweight Java Runtime Environment (JRE) for the final image.
FROM eclipse-temurin:21-jdk

# Set the working directory for the running application.
WORKDIR /app

# Copy the built Spring Boot JAR from the 'backend-build' stage into the final image.
# The JAR will be in '/app/animal-welfare-tracker/build/libs/*.jar' from the backend-build stage.
COPY --from=backend-build /app/build/libs/*.jar app.jar

# Expose the port that your Spring Boot application will listen on.
# This is the internal port. Render will map an external port to this.
EXPOSE 8080

# Set the SERVER_PORT environment variable for Spring Boot.
# Render provides a 'PORT' environment variable (e.g., 10000).
# This tells Spring Boot to listen on Render's assigned port, defaulting to 8080 if not set.
ENV SERVER_PORT=${PORT:-8080}

# Define the command to run your Spring Boot application when the container starts.
ENTRYPOINT ["java", "-jar", "app.jar"]
