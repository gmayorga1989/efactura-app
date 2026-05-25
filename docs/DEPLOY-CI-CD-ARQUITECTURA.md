# CI/CD — efactura-app

Mismo patrón que **sri-download-service**: GitHub Actions → GHCR → SSH → Docker Compose → Spring Boot con `entrypoint.sh` y `exec java` como PID 1.

## Flujo

1. **build-and-push:** `mvn package` + `docker build-push` → `ghcr.io/<owner>/efactura-app:latest` y `:<sha>`
2. **deploy:** SSH al Droplet, `docker pull`, `docker compose up -d`, espera `/actuator/health`

## Dockerfile

- Multi-stage Maven 17 + JRE
- Sin Xvfb (no requiere scraping)
- `ENTRYPOINT ["/entrypoint.sh"]` → `exec java -jar /app/app.jar`
- Healthcheck: `http://127.0.0.1:8080/actuator/health`, `start-period: 180s`

## entrypoint.sh

```sh
#!/bin/sh
set -e
echo "Starting efactura-app..."
exec java -jar /app/app.jar
```

## docker-compose.prod.yml

- Perfil `SPRING_PROFILES_ACTIVE=cloud-test` → activa **`prod`** (producción) + pool Hikari reducido para Postgres managed DO
- Alternativa equivalente: `SPRING_PROFILES_ACTIVE=prod` (sin ajuste Hikari de `cloud-test`)
- `SPRING_FLYWAY_ENABLED=false` en contenedor (migraciones fuera del deploy o previas)
- Volúmenes: certificados, comprobantes, logos bajo `/data`

## Perfiles Spring (`application.yml`)

| Perfil | Uso |
|--------|-----|
| `local` | `mvn spring-boot:run`, logging DEBUG, bootstrap demo |
| `prod` | Docker/DO: bootstrap off, SRI pruebas off, CORS/URLs vía env, Swagger UI off, paths `/data`, Flyway off por defecto |
| `cloud-test` | Alias en `.env`: incluye `prod` + Hikari máx. 6 (plan DO pequeño) |

Variables críticas en `.env` del servidor (perfil `cloud-test` → `prod`): `JWT_SECRET`, `EFACTURA_CRYPTO_MASTER`, `SUITE_JWT_SECRET`, `SUITE_IDENTITY_PUBLIC_BASE_URL`, `CORS_ORIGINS`, `APP_BASE_URL`, `EFACTURA_SRI_DOWNLOAD_*`.

## Secrets (prefijo `EFACTURA_APP_`)

Comparten environment **`EFACTURA_CI_CD`** con el resto de la suite.
