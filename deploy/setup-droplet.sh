#!/usr/bin/env bash
# Configuración inicial de un Droplet Ubuntu 22.04 para efactura-app.
# Ejecutar como root: bash setup-droplet.sh

set -euo pipefail

apt-get update
apt-get install -y docker.io docker-compose-v2 nginx certbot python3-certbot-nginx ufw

systemctl enable docker
systemctl start docker

ufw allow OpenSSH
ufw allow 'Nginx Full'
ufw --force enable

mkdir -p /opt/efactura-app
echo "Listo. Copie .env y docker-compose.prod.yml a /opt/efactura-app y configure nginx."
