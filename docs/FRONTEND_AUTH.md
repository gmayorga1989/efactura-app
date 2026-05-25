# Autenticación multiempresa — guía rápida para el front

## Variables de estado

Tras autenticación completa, mantener:

- `accessToken`, `refreshToken`
- `identidadId` = claim `sub` del access (UUID)
- `empresaId` = claim `empresaId` del access (UUID o `null` si sesión de plataforma)
- `slug` / RUC para rutas tipo `/t/{slug}/...` (el backend expone `slug` en `EmpresaResponse` y `GET /me`; si no hay slug, normalizar RUC en la URL según convenga)

## Flujo de pantallas

1. **Login** — `POST /api/web/v1/auth/login` con `{ email, password }` (opcional `ruc` como atajo para elegir empresa en el mismo paso).
2. Si `loginStep === "COMPLETE"` → guardar tokens y `identidadId` / `empresaId` desde el JWT decodificado o llamar `GET /me`.
3. Si `loginStep === "SELECT_EMPRESA"` → pantalla selector con `empresas` (cada ítem indica `seleccionable` y `motivoNoSeleccion` si aplica). Enviar `POST /api/web/v1/auth/select-empresa` con `{ sessionTicket, empresaId }` (`empresaId` null solo para fila `esPlataforma`).
4. **Dashboard** — con tokens completos; rutas acotadas al `slug`/`empresaId` actual.

## Invitaciones

- Tenant: `POST /api/web/v1/invitaciones` (`EMPRESA_ADMIN`), body `{ email, rolCodigo }`.
- Plataforma: `POST /api/web/v1/empresas/{empresaId}/invitaciones` (`PLATFORM_ADMIN`).
- Listar pendientes: `GET /api/web/v1/invitaciones/pendientes` o `GET .../empresas/{empresaId}/invitaciones/pendientes`.
- Cancelar (tenant): `POST /api/web/v1/invitaciones/{invitacionId}/cancelacion`.
- Cancelar (plataforma): `POST /api/web/v1/empresas/{empresaId}/invitaciones/{invitacionId}/cancelacion`.
- Respuesta crear: `id`, `token` (enlace mágico), `expiraEn` — en producción puede omitirse en API y enviarse solo por correo.
- Aceptar (público): `POST /api/web/v1/auth/accept-invite` con `{ token, password, nombre? }`. Cuenta nueva: `nombre` obligatorio. Cuenta existente: `password` debe ser la actual.

## Menú y roles

- `GET /api/web/v1/menu` — árbol de menú filtrado por permisos del JWT.
- `GET /api/web/v1/permisos-catalogo` — catálogo de permisos (`EMPRESA_ADMIN` o `PLATFORM_ADMIN`).
- CRUD de roles personalizados: `/api/web/v1/roles` (tenant) o `/api/web/v1/empresas/{empresaId}/roles` (plataforma); ver `docs/PERMISOS.md`.

## Cambio de empresa sin password

`POST /api/web/v1/auth/switch-empresa` con header `Authorization: Bearer <access>` y body `{ empresaId }` (null para volver a plataforma). Respuesta: nuevo par de tokens si se implementa rotación de refresh (hoy se devuelven access + refresh nuevos).

Política servidor (opcional): `efactura.jwt.switch-max-token-age-minutes`, `efactura.jwt.switch-require-mfa`.

## Sesión y permisos

- `POST /api/web/v1/auth/refresh` — mismo refresh rota y **recalcula permisos** desde BD.
- `GET /api/web/v1/me` — debe coincidir con el JWT (misma identidad, misma empresa, mismos permisos efectivos salvo carrera con cambio de roles).
- `GET /api/web/v1/mis-empresas` — listado de membresías para selector contextual.

## Guardas en el front

- No mezclar permisos de otra empresa: solo usar `authorities` del access actual.
- Cualquier `empresaId` en rutas del API debe alinearse con el del token (salvo `PLATFORM_ADMIN` con endpoints explícitos de plataforma).
- Tras `switch-empresa`, actualizar estado global (tokens, `empresaId`, branding desde `/me`) y redirigir al tenant correcto en la URL.

## Pruebas de integración

`MultiTenantAuthIntegrationTest` usa Testcontainers (PostgreSQL). Requiere Docker local; si no hay Docker, el test se omite (`disabledWithoutDocker = true`). En CI con Docker se ejecuta el flujo login → selector → `GET /empresas` (403 a otra empresa) → refresh.
