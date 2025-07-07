FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

COPY gradlew .
COPY gradle ./gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

COPY domain/build.gradle.kts ./domain/
COPY api/build.gradle.kts ./api/
COPY batch/build.gradle.kts ./batch/

RUN ./gradlew dependencies --no-daemon

COPY . .

RUN ./gradlew :batch:build --no-daemon -x test

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser

COPY --from=builder /app/batch/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]