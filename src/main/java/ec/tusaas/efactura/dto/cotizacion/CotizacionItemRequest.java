package ec.tusaas.efactura.dto.cotizacion;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CotizacionItemRequest(
    UUID productoId,
    @Size(max = 50) String codigoPrincipal,
    @Size(max = 50) String codigoAuxiliar,
    @NotBlank @Size(max = 500) String descripcion,
    @NotNull @DecimalMin(value = "0.000001") BigDecimal cantidad,
    @NotNull @DecimalMin(value = "0.000000") BigDecimal precioUnitario,
    @DecimalMin(value = "0.00") BigDecimal descuento,
    BigDecimal ivaPorcentaje) {}
