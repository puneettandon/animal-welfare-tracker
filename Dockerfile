# Stage 1: Build the application using Gradle
FROM gradle:8.5-jdk17 AS build
COPY --chown=gradle:gradle . /app
WORKDIR /app
RUN gradle build -x test

# Stage 2: Run the application
FROM eclipse-temurin:17-jdk
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
