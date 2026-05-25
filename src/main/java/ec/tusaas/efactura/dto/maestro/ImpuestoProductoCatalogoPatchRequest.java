package ec.tusaas.efactura.dto.maestro;

import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ImpuestoProductoCatalogoPatchRequest(
    @Size(max = 200) String nombre,
    BigDecimal porcentajeDefault,
    Integer orden,
    Boolean activo) {}
