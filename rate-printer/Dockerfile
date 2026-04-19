FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY gradlew gradlew.bat gradle/ ./
COPY build.gradle settings.gradle ./
COPY api/ api/
COPY rate-printer/ rate-printer/
RUN chmod +x gradlew \
    && ./gradlew :rate-printer:bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/rate-printer/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
