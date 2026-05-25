# efactura-app

API Spring Boot de **facturación electrónica** (eFactura Ecuador). Repositorio independiente con despliegue Docker + GitHub Actions + GHCR + DigitalOcean.

| Recurso | URL local |
|---------|-----------|
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Actuator health | http://localhost:8080/actuator/health |

Puerto por defecto: **8080**.

---

## Arranque local

1. PostgreSQL con base `efactura` (puede usar el compose del monorepo `efactura-ec`).
2. Redis opcional (`EFACTURA_REDIS_ENABLED=false` por defecto en muchos entornos).
3. Ejecutar:

   ```powershell
   mvn spring-boot:run
   ```

---

## Publicación en DigitalOcean

| Documento | Contenido |
|-----------|-----------|
| [docs/DEPLOY-DIGITALOCEAN.md](docs/DEPLOY-DIGITALOCEAN.md) | Checklist operativo |
| [docs/DEPLOY-CI-CD-ARQUITECTURA.md](docs/DEPLOY-CI-CD-ARQUITECTURA.md) | CI/CD, Dockerfile, entrypoint |

Cada push a **`main`** compila, publica en **GHCR** y despliega por SSH (environment `EFACTURA_CI_CD`).

### Secrets de GitHub Actions

| Secret | Descripción |
|--------|-------------|
| `EFACTURA_APP_DEPLOY_HOST` | IP del Droplet |
| `EFACTURA_APP_DEPLOY_USER` | Usuario SSH |
| `EFACTURA_APP_DEPLOY_SSH_KEY` | Clave privada PEM |
| `EFACTURA_APP_DEPLOY_PORT` | Puerto SSH (opcional, default 22) |
| `EFACTURA_APP_DEPLOY_PATH` | Ruta en servidor (opcional, `/opt/efactura-app`) |
| `EFACTURA_APP_GHCR_PAT` | PAT `read:packages` para `docker pull` en el Droplet |

Variables en servidor: copiar [`.env.example`](.env.example) a `/opt/efactura-app/.env`.

### Integración con otros servicios

| Servicio | Variable en `.env` |
|----------|-------------------|
| identity-gateway | `SUITE_IDENTITY_PUBLIC_BASE_URL`, `SUITE_JWT_*` |
| sri-download-service | `EFACTURA_SRI_DOWNLOAD_BASE_URL`, `EFACTURA_SRI_DOWNLOAD_API_KEY` |

---

## Repositorio remoto

```powershell
git remote add origin https://github.com/TU_ORG/efactura-app.git
git push -u origin main
```
