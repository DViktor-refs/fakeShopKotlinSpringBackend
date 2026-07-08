# ---- Build szakasz ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Gradle wrapper és build fájlok előbb (jobb réteg-cache)
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x ./gradlew && ./gradlew --version

# Forrás bemásolása és csomagolás
COPY src ./src
RUN ./gradlew clean bootJar -x test --no-daemon

# ---- Futtató szakasz ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
