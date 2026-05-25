package ec.tusaas.efactura.dto.auth;

import java.util.UUID;

public record SuiteTenantBootstrapResultDto(
    boolean empresaCreada,
    boolean identidadCreada,
    boolean membresiaCreada,
    UUID empresaId,
    UUID identidadId,
    UUID membresiaId,
    String rucAsignado,
    String empresaSlug) {}
