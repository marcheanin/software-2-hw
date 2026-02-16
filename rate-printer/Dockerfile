FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY api/ api/
COPY rate-printer/ rate-printer/
RUN gradle :rate-printer:bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/rate-printer/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
