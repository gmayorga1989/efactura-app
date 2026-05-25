package ec.tusaas.efactura.dto.sridescarga;

import java.time.Instant;
import java.util.UUID;

public record SriPortalCredentialStatusResponse(
    boolean serviceEnabled,
    boolean provisioned,
    UUID subscriberId,
    boolean configured,
    String portalUsuarioMasked,
    Instant vigenteDesde) {}
