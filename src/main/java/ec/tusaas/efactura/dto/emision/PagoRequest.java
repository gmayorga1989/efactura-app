package ec.tusaas.efactura.dto.emision;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record PagoRequest(
    /** Código SRI de forma de pago, por ejemplo 20 = otros con utilización del sistema financiero. */
    @NotBlank String formaPago,
    @DecimalMin(value = "0.00") BigDecimal total,
    Integer plazo,
    String unidadTiempo) {}
