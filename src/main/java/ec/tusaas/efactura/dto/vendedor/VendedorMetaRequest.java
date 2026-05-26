package ec.tusaas.efactura.dto.vendedor;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record VendedorMetaRequest(
    @NotNull @Min(2000) @Max(2100) Integer periodoAnio,
    @NotNull @Min(1) @Max(12) Integer periodoMes,
    @NotNull @DecimalMin("0") BigDecimal metaMonto,
    Integer metaCantidad,
    String notas) {}
