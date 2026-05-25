package ec.tusaas.efactura.dto.maestro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ImpuestoProductoCatalogoCreateRequest(
    @NotBlank @Size(max = 40) String tipo,
    @NotBlank @Size(max = 50) String codigo,
    @NotBlank @Size(max = 200) String nombre,
    BigDecimal porcentajeDefault,
    Integer orden) {}
