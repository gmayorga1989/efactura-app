package ec.tusaas.efactura.dto.maestro;

import java.util.UUID;

public record ClienteDireccionResponse(
    UUID id,
    String tipo,
    String direccion,
    String provincia,
    String canton,
    String parroquia,
    String referencia,
    boolean principal,
    String estado) {}
