package ec.tusaas.efactura.dto.maestro;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Impuesto o cargo definido por nombre y porcentaje (sin catálogo central). */
public record ProductoImpuestoManualRequest(
    @NotBlank @Size(max = 200) String nombre,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal porcentaje) {}
