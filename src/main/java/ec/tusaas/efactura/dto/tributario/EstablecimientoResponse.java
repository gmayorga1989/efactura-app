package ec.tusaas.efactura.dto.tributario;

import java.util.UUID;

public record EstablecimientoResponse(
    UUID id, UUID empresaId, String codigo, String nombre, String direccion, String estado) {}
