package ec.tusaas.efactura.dto.maestro;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductoImpuestoAdicionalResponse(
    UUID catalogoItemId,
    String nombre,
    String tipo,
    String codigo,
    BigDecimal porcentaje) {}
