package ec.tusaas.efactura.dto.tenant;

import java.time.Instant;
import java.util.UUID;

public record TenantDatasourceConfigResponse(
    UUID id,
    UUID empresaId,
    String empresaRuc,
    String empresaSlug,
    String empresaRazonSocial,
    String modoTenant,
    String datasourceKey,
    String estado,
    Instant fechaCreacion,
    Instant fechaModificacion) {}
