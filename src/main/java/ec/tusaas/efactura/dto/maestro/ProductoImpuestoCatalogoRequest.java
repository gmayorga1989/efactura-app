package ec.tusaas.efactura.dto.maestro;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/** Línea de impuesto/cargo adicional tomada del catálogo por país. */
public record ProductoImpuestoCatalogoRequest(@NotNull UUID catalogoItemId, BigDecimal porcentaje) {}
