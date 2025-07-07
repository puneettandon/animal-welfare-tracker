# Stage 1: Build the React frontend
# Uses a Node.js base image to build your React application.
FROM node:20-alpine AS frontend-build

# Set the working directory inside the container for the frontend project.
# This WORKDIR is relative to the root of the container's build context.
# Since your 'ui' folder is at the root of your repo (where Dockerfile is),
# we'll work directly within the 'ui' folder inside the container.
WORKDIR /app/ui

# Copy package.json and package-lock.json from the host's 'ui' directory
# into the current working directory (/app/ui) inside the container.
# This leverages Docker's build cache: if only source code changes, npm install won't re-run.
COPY ui/package.json ui/package-lock.json ./

# Install Node.js dependencies.
RUN npm install

# Copy the rest of your React source code from the host's 'ui' directory
# into the current working directory (/app/ui) inside the container.
COPY ui/ ./

# Build the React application for production.
# The "homepage": "./" in ui/package.json ensures relative paths are generated.
RUN npm run build

# Stage 2: Build the Spring Boot application
# Uses a Gradle base image for building the Java backend.
FROM gradle:8.5-jdk21 AS backend-build

# Set the working directory to '/app' for this stage.
# This will be the root of your entire repository within the container.
WORKDIR /app

# Copy the ENTIRE contents of your repository (where the Dockerfile is located)
# into the '/app' directory inside the container.
# This ensures that both 'animal-welfare-tracker/' (Spring Boot project)
# and 'ui/' (React project) are available under '/app/' in the container.
COPY --chown=gradle:gradle . /app

# --- CRUCIAL STEP: Copy React build output into Spring Boot's static resources ---
# This takes the 'build' directory created in the 'frontend-build' stage
# (which is located at /app/ui/build inside the frontend-build container)
# and places it into the 'src/main/resources/static' directory
# within your Spring Boot project's context.
# The target path is /app/animal-welfare-tracker/src/main/resources/static
# because your Spring Boot project is nested under 'animal-welfare-tracker/'.
COPY --from=frontend-build /app/ui/build /app/animal-welfare-tracker/src/main/resources/static

# Now, change the working directory specifically to your Spring Boot project's root.
# This is '/app/animal-welfare-tracker/' because you copied the whole repo into /app,
# and your Spring Boot project is nested within 'animal-welfare-tracker/'.
WORKDIR /app/animal-welfare-tracker

# Build the Spring Boot application using Gradle.
# We explicitly tell Gradle to build the 'animal-welfare-tracker' sub-project.
# This command is run from the sub-project's directory, so no leading ':' is needed.
# If your settings.gradle at the root defines 'animal-welfare-tracker' as a sub-project,
# and you are running this from its directory, 'build' is sufficient.
# If you were running from /app, it would be 'gradle :animal-welfare-tracker:build'.
RUN gradle build -x test

# Stage 3: Create the final runtime image
# Uses a lightweight Java Runtime Environment (JRE) for the final image.
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