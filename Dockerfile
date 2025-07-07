# Stage 1: Build the React frontend
# We use a Node.js base image for this stage.
FROM node:20-alpine AS frontend-build

# Set the working directory inside the container for the frontend project.
# This assumes your 'ui' folder is directly at the root level of your Git repository
# where the Dockerfile is located.
WORKDIR /app/ui

# Copy package.json and package-lock.json from your host's 'ui' directory
# into the current working directory (/app/ui) inside the container.
COPY ui/package.json ui/package-lock.json ./

# Install Node.js dependencies.
RUN npm install

# Copy the rest of your React source code from your host's 'ui' directory
# into the current working directory (/app/ui) inside the container.
COPY ui/ ./

# Build the React application for production.
RUN npm run build

# Stage 2: Build the Spring Boot application
# We use a Gradle base image for this stage.
FROM gradle:8.5-jdk21 AS backend-build

# Set the working directory to '/app' for this stage.
WORKDIR /app

# Copy the ENTIRE contents of your repository (where the Dockerfile is)
# into the '/app' directory inside the container.
# This ensures that both 'animal-welfare-tracker/' (Spring Boot)
# and 'ui/' (React) are available under '/app/' in the container.
COPY --chown=gradle:gradle . /app

# --- CRUCIAL STEP: Copy React build output into Spring Boot's static resources ---
# This takes the 'build' directory created in the 'frontend-build' stage
# (which is located at /app/ui/build inside the frontend-build container)
# and places it into the 'src/main/resources/static' directory
# within your Spring Boot project's context, which is located at
# /app/animal-welfare-tracker/src/main/resources/static inside this container.
COPY --from=frontend-build /app/ui/build /app/animal-welfare-tracker/src/main/resources/static

# Now, change the working directory specifically to your Spring Boot project's root.
# This is '/app/animal-welfare-tracker/' because you copied the whole repo into /app,
# and your Spring Boot project is nested within 'animal-welfare-tracker/'.
WORKDIR /app/animal-welfare-tracker

# Build the Spring Boot application using Gradle.
# '-x test' skips tests, which can speed up builds in CI/CD.
RUN gradle build -x test

# Stage 3: Create the final runtime image
# We use a lightweight Java Runtime Environment (JRE) for the final image.
FROM eclipse-temurin:21-jdk

# Set the working directory for the running application.
WORKDIR /app

# Copy the built Spring Boot JAR from the 'backend-build' stage into the final image.
# The JAR will be located at '/app/animal-welfare-tracker/build/libs/*.jar'
# from the 'backend-build' stage's working directory.
COPY --from=backend-build /app/animal-welfare-tracker/build/libs/*.jar app.jar

# Expose the port that your Spring Boot application will listen on.
# This is the internal port. Render will map an external port to this.
EXPOSE 8080

# Set the SERVER_PORT environment variable for Spring Boot.
# Render provides a 'PORT' environment variable (e.g., 10000).
# This tells Spring Boot to listen on Render's assigned port, defaulting to 8080 if not set.
ENV SERVER_PORT=${PORT:-8080}

# Define the command to run your Spring Boot application when the container starts.
ENTRYPOINT ["java", "-jar", "app.jar"]