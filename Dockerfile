# Imagen de producción: API facturación electrónica (Spring Boot).
# Build: docker build -t efactura-app .
# Run:   docker compose -f docker-compose.prod.yml up -d

FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy

ENV DEBIAN_FRONTEND=noninteractive \
    SPRING_PROFILES_ACTIVE=cloud-test \
    SERVER_PORT=8080 \
    EFACTURA_REDIS_ENABLED=false \
    EFACTURA_STORAGE_LOCAL=/data/certificados \
    EFACTURA_COMPROBANTES_LOCAL=/data/comprobantes \
    EFACTURA_LOGOS_LOCAL=/data/logos \
    EFACTURA_STORAGE_OBJECT_ROOT=/data

RUN apt-get update \
    && apt-get install -y --no-install-recommends wget ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /build/target/efactura-app-*.jar /app/app.jar
COPY entrypoint.sh /entrypoint.sh

RUN mkdir -p /data/certificados /data/comprobantes /data/logos \
    && useradd -r -s /usr/sbin/nologin efactura \
    && chown -R efactura:efactura /app /data \
    && chmod +x /entrypoint.sh \
    && chown efactura:efactura /entrypoint.sh

USER efactura
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=180s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["/entrypoint.sh"]
