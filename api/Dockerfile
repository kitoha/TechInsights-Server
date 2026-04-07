FROM eclipse-temurin:21-jdk-jammy AS builder
LABEL authors="techinsights"

WORKDIR /app

COPY gradlew .
COPY gradle ./gradle
COPY settings.gradle.kts .
COPY build.gradle.kts .

COPY domain/build.gradle.kts ./domain/
COPY api/build.gradle.kts ./api/

RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon

COPY domain/src ./domain/src
COPY api/src ./api/src

# 테스트는 매번 병합 전 실행되므로, 빌드 시 테스트를 제외합니다.
RUN ./gradlew :api:build --no-daemon -x test

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser

COPY --from=builder /app/api/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]