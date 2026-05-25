# Despliegue en DigitalOcean — efactura-app

Repositorio independiente. Cada push a **`main`** ejecuta [`.github/workflows/deploy.yml`](../.github/workflows/deploy.yml).

Documentación técnica: [DEPLOY-CI-CD-ARQUITECTURA.md](./DEPLOY-CI-CD-ARQUITECTURA.md)

## Arquitectura

```
GitHub (main) → GitHub Actions → GHCR
                      ↓
DigitalOcean Droplet → docker compose → :8080
                      ↓
Managed PostgreSQL → BD efactura
```

## 1. PostgreSQL

Base **`efactura`** en el cluster Managed (misma instancia que `suite` y `sri_download`).

## 2. Droplet

1. Ubuntu 22.04, ≥ 2 vCPU / 4 GB RAM recomendado.
2. `bash deploy/setup-droplet.sh`
3. Preparar `/opt/efactura-app`:

```bash
mkdir -p /opt/efactura-app
scp docker-compose.prod.yml root@DROPLET:/opt/efactura-app/
scp .env.example root@DROPLET:/opt/efactura-app/.env
# Editar .env: GITHUB_REPOSITORY_OWNER, DB_*, JWT_SECRET, integraciones
```

## 3. Secrets GitHub

Ver tabla en [README.md](../README.md).

## 4. Nginx + HTTPS

[`deploy/nginx-efactura-app.conf.example`](../deploy/nginx-efactura-app.conf.example)

## 5. Verificación

```bash
curl -sf http://127.0.0.1:8080/actuator/health
docker logs -f efactura-app
```
