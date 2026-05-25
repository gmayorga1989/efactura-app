package ec.tusaas.efactura.dto.maestro;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductoListaPrecioRequest(
    @NotBlank @Size(max = 50) String listaCodigo, @NotNull @DecimalMin("0.000000") BigDecimal precio) {}
