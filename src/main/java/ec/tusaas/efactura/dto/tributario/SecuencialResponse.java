package ec.tusaas.efactura.dto.tributario;

import java.util.UUID;

public record SecuencialResponse(
    UUID id, UUID puntoEmisionId, String tipoComprobante, long valorActual, String estado) {}
