package ec.tusaas.efactura.dto.tributario;

import java.util.UUID;

public record PuntoEmisionResponse(
    UUID id,
    UUID empresaId,
    UUID establecimientoId,
    String codigo,
    String nombre,
    String establecimientoCodigo,
    String estado) {}
