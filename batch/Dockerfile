FROM eclipse-temurin:21-jdk-jammy AS builder
LABEL authors="techinsights"

WORKDIR /app

COPY gradlew .
COPY gradle ./gradle
COPY settings.gradle.kts .
COPY build.gradle.kts .

COPY domain/build.gradle.kts ./domain/
COPY batch/build.gradle.kts ./batch/

RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon

COPY domain/src ./domain/src
COPY batch/src ./batch/src

RUN ./gradlew :batch:build --no-daemon -x test

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN apt-get update && \
    apt-get install -y ca-certificates p11-kit && \
    update-ca-certificates --fresh && \
    CACERTS_PATH=$(find $JAVA_HOME -name cacerts | grep security | head -1) && \
    echo "Using cacerts at: $CACERTS_PATH" && \
    trust extract --overwrite --format=java-cacerts --filter=ca-anchors --purpose=server-auth "$CACERTS_PATH" && \
    rm -rf /var/lib/apt/lists/*

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

COPY --from=builder /app/batch/build/libs/*.jar app.jar

# JMX Exporter 추가 (로컬 테스트용)
COPY batch/monitoring/jmx_prometheus_javaagent.jar /app/jmx_prometheus_javaagent.jar
COPY batch/monitoring/jmx-config.yml /app/jmx-config.yml

# Environment variable for JVM options (can be overridden)
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:MaxMetaspaceSize=128m -XX:+UseG1GC"

# Change ownership of /app directory (logs will be mounted as volume)
RUN chown -R appuser:appgroup /app

USER appuser

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
