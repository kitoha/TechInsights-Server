FROM eclipse-temurin:21-jdk-jammy AS builder
LABEL authors="techinsights"

WORKDIR /app

COPY gradlew .
COPY gradle ./gradle
COPY settings.gradle.kts .
COPY build.gradle.kts .

COPY domain/build.gradle.kts ./domain/
COPY batch/build.gradle.kts ./batch/

RUN ./gradlew dependencies --no-daemon

COPY domain/src ./domain/src
COPY batch/src ./batch/src

RUN ./gradlew :batch:build --no-daemon -x test

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN apt-get update && \
    apt-get install -y ca-certificates && \
    update-ca-certificates && \
    rm -rf /var/lib/apt/lists/*

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser

COPY --from=builder /app/batch/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
