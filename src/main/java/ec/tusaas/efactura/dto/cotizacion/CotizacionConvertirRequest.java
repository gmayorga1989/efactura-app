package ec.tusaas.efactura.dto.cotizacion;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CotizacionConvertirRequest(@NotNull UUID puntoEmisionId) {}
