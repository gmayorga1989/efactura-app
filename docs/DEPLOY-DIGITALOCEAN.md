# Despliegue en DigitalOcean — efactura-app

Repositorio independiente. Cada push a **`main`** ejecuta [`.github/workflows/deploy.yml`](../.github/workflows/deploy.yml).

| Documento | Contenido |
|-----------|-----------|
| [DEPLOY-CI-CD-ARQUITECTURA.md](./DEPLOY-CI-CD-ARQUITECTURA.md) | CI/CD, Dockerfile, entrypoint |
| [../../../docs/DEPLOY-SERVIDOR-COMANDOS.md](../../../docs/DEPLOY-SERVIDOR-COMANDOS.md) | Comandos unificados (carpetas + `scp` los 3 servicios) |

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

```sql
CREATE DATABASE efactura;   -- si aún no existe
```

## 2. Droplet — carpetas y copia de archivos

Ruta en servidor: **`/opt/efactura-app`**.

### 2.1 Instalar Docker (primera vez en el Droplet)

```powershell
$DROPLET_IP = "157.230.xxx.xxx"
$SSH_USER = "root"

scp deploy\setup-droplet.sh "${SSH_USER}@${DROPLET_IP}:/tmp/setup-droplet.sh"
ssh "${SSH_USER}@${DROPLET_IP}" "bash /tmp/setup-droplet.sh"
```

### 2.2 Crear carpeta (SSH en el Droplet)

```bash
ssh root@DROPLET_IP
sudo mkdir -p /opt/efactura-app
sudo chown $USER:$USER /opt/efactura-app
exit
```

### 2.3 Copiar archivos desde tu PC

**PowerShell** (desde monorepo `efactura-ec` o desde este repo):

```powershell
$DROPLET_IP = "157.230.xxx.xxx"
$SSH_USER = "root"

ssh "${SSH_USER}@${DROPLET_IP}" "mkdir -p /opt/efactura-app"

scp backend\efactura-app\docker-compose.prod.yml "${SSH_USER}@${DROPLET_IP}:/opt/efactura-app/docker-compose.prod.yml"
scp backend\efactura-app\.env.example "${SSH_USER}@${DROPLET_IP}:/opt/efactura-app/.env"
scp backend\efactura-app\deploy\nginx-efactura-app.conf.example "${SSH_USER}@${DROPLET_IP}:/opt/efactura-app/nginx.conf.example"
```

Desde **solo** este repositorio clonado:

```powershell
scp docker-compose.prod.yml "${SSH_USER}@${DROPLET_IP}:/opt/efactura-app/"
scp .env.example "${SSH_USER}@${DROPLET_IP}:/opt/efactura-app/.env"
scp deploy\nginx-efactura-app.conf.example "${SSH_USER}@${DROPLET_IP}:/opt/efactura-app/nginx.conf.example"
```

**Bash:**

```bash
export DROPLET_IP=157.230.xxx.xxx SSH_USER=root
ssh ${SSH_USER}@${DROPLET_IP} "mkdir -p /opt/efactura-app"
scp docker-compose.prod.yml ${SSH_USER}@${DROPLET_IP}:/opt/efactura-app/
scp .env.example ${SSH_USER}@${DROPLET_IP}:/opt/efactura-app/.env
scp deploy/nginx-efactura-app.conf.example ${SSH_USER}@${DROPLET_IP}:/opt/efactura-app/nginx.conf.example
```

### 2.4 Editar `.env` en el servidor

```bash
ssh root@DROPLET_IP
nano /opt/efactura-app/.env
chmod 600 /opt/efactura-app/.env
```

Mínimo:

```bash
GITHUB_REPOSITORY_OWNER=TU_ORG
DB_HOST=...
DB_PORT=25060
DB_USER=doadmin
DB_PASSWORD=...
# El compose usa DB_USER y DB_PASSWORD (no dejar SPRING_DATASOURCE_* vacíos en compose).
DB_URL_SUFFIX=?sslmode=require
DB_NAME=efactura
JWT_SECRET=...
EFACTURA_CRYPTO_MASTER=...
EFACTURA_SRI_DOWNLOAD_BASE_URL=https://sri.tudominio.com
EFACTURA_SRI_DOWNLOAD_API_KEY=igual-que-SRI_DL_SERVICE_API_KEY
SUITE_IDENTITY_PUBLIC_BASE_URL=https://identity.tudominio.com
SUITE_JWT_SECRET=igual-que-identity-gateway
CORS_ORIGINS=https://app.tudominio.com
EFACTURA_BOOTSTRAP=false
```

### 2.5 Login GHCR y arranque manual (opcional)

```bash
echo "TU_PAT" | docker login ghcr.io -u TU_USUARIO --password-stdin
cd /opt/efactura-app
export GITHUB_REPOSITORY_OWNER=TU_ORG
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

## 3. Secrets GitHub

| Secret | Descripción |
|--------|-------------|
| `EFACTURA_APP_DEPLOY_HOST` | IP del Droplet |
| `EFACTURA_APP_DEPLOY_USER` | Usuario SSH |
| `EFACTURA_APP_DEPLOY_SSH_KEY` | Clave PEM |
| `EFACTURA_APP_DEPLOY_PORT` | `22` (opcional) |
| `EFACTURA_APP_DEPLOY_PATH` | `/opt/efactura-app` (opcional) |
| `EFACTURA_APP_GHCR_PAT` | PAT `read:packages` |

Environment: **`EFACTURA_CI_CD`**. Tabla completa: [README.md](../README.md).

## 4. Nginx + HTTPS

```bash
sudo cp /opt/efactura-app/nginx.conf.example /etc/nginx/sites-available/efactura-app
sudo ln -sf /etc/nginx/sites-available/efactura-app /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
sudo certbot --nginx -d app.tudominio.com
```

## 5. Verificación

```bash
curl -sf http://127.0.0.1:8080/actuator/health
curl -sf http://127.0.0.1:8080/swagger-ui.html -o /dev/null -w "%{http_code}\n"
docker logs -f efactura-app
```
