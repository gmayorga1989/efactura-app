package ec.tusaas.efactura.dto.maestro;

import java.math.BigDecimal;
import java.util.UUID;

public record ImpuestoProductoCatalogoResponse(
    UUID id,
    UUID empresaId,
    String paisIso,
    String tipo,
    String codigo,
    String nombre,
    BigDecimal porcentajeDefault,
    int orden,
    boolean activo) {}
