# Catálogo canónico de permisos (API web)

Los permisos viven en la tabla `permiso` (código único). El JWT de acceso incluye los códigos efectivos de la **membresía actual** en el claim `authorities` (coma-separados). Spring Security usa `hasAuthority('CODIGO')`.

## Módulo CORE

| Código | Descripción |
|--------|-------------|
| `EMPRESA_ADMIN` | Administración del tenant: configuración, usuarios de empresa, API keys (según política). |
| `PLATFORM_ADMIN` | Operaciones de plataforma; `empresaId` nulo en JWT. |

## Módulo EMISION

| Código | Descripción |
|--------|-------------|
| `FACTURA_EMITIR` | Emitir comprobantes electrónicos. |

## Módulo REPORTES

| Código | Descripción |
|--------|-------------|
| `REPORTE_VER` | Ver reportes operativos. |

## Evolución recomendada (meta-permisos)

Separar lectura/edición donde aplique, por ejemplo:

- `EMPRESA_CONFIG_LEER` / `EMPRESA_CONFIG_EDITAR`
- `USUARIOS_INVITAR` / `USUARIOS_ROLES_ASIGNAR`
- `API_KEY_GESTION` (si se desea independizar de `EMPRESA_ADMIN`)

Hoy el front puede asumir que `EMPRESA_ADMIN` incluye gestión de API keys y usuarios de empresa, coherentemente con `MeFeatures` en `GET /me`.

## Fuente de verdad del menú

- **Dinámico (BD):** `GET /api/web/v1/menu` (JWT). Devuelve ítems de `menu_item` cuyo permiso requerido está en `authorities` del token (o sin permiso requerido). `PLATFORM_ADMIN` sin `empresaId` ve todas las entradas activas.
- **Catálogo:** migración `V11__menu_item_sync_frontend.sql` alinea `inicio` → `/t/:slug/dashboard` y añade `integraciones-api-keys` → `/t/:slug/integraciones/api-keys` (`requiere_permiso_codigo` = `EMPRESA_ADMIN`, coherente con el panel de API keys en tenant).
- **Convención en cliente:** el campo `menuHints` en `MeResponse` puede quedar vacío si el front solo mapea rutas por permisos.

## Catálogo de permisos (admin)

`GET /api/web/v1/permisos-catalogo` — solo `EMPRESA_ADMIN` o `PLATFORM_ADMIN`. Lista permisos activos ordenados por módulo/código (útil para armar roles personalizados en UI).

## Roles por empresa (CRUD)

Rutas tenant (`EMPRESA_ADMIN`; JWT con `empresaId`):

- `GET/POST /api/web/v1/roles`
- `PUT/DELETE /api/web/v1/roles/{rolId}`

Rutas plataforma (`PLATFORM_ADMIN`):

- `GET/POST /api/web/v1/empresas/{empresaId}/roles`
- `PUT/DELETE /api/web/v1/empresas/{empresaId}/roles/{rolId}`

Los roles de sistema no se editan ni eliminan. Crear/actualizar incluye lista de códigos de permiso.

## Auditoría (`auditoria`)

| Acción | Cuándo |
|--------|--------|
| `AUTH_LOGIN` | Tokens emitidos tras login directo (una empresa o atajo RUC). |
| `AUTH_SELECT_EMPRESA` | Tras `POST /auth/select-empresa`. |
| `AUTH_SWITCH_EMPRESA` | Tras `POST /auth/switch-empresa`. |
| `AUTH_REFRESH` | Tras `POST /auth/refresh`. |
| `AUTH_ACCEPT_INVITE` | Tras aceptar invitación y abrir sesión. |
| `USUARIO_INVITACION_CREADA` | Tras `POST /invitaciones`. |
| `FACTURA_EMITIDA` | Tras emisión exitosa de factura (detalle: clave de acceso, estado SRI, secuencial, total, origen). |

En `cambios` (JSON): `identidadId`, `membresiaId`; invitaciones: `email`, `rol`.

## Invitaciones pendientes y cancelación

- Tenant: `GET /api/web/v1/invitaciones/pendientes`, `POST /api/web/v1/invitaciones/{invitacionId}/cancelacion`.
- Plataforma: `GET /api/web/v1/empresas/{empresaId}/invitaciones/pendientes`, `POST /api/web/v1/empresas/{empresaId}/invitaciones/{invitacionId}/cancelacion`.

Config opcional: `efactura.invitacion.log-token-servidor=true` para registrar el token de invitación en logs del servidor (solo depuración).
