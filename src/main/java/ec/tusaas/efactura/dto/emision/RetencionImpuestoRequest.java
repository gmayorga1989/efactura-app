package ec.tusaas.efactura.dto.emision;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RetencionImpuestoRequest(
    @NotBlank String codigo,
    @NotBlank String codigoRetencion,
    @NotNull @DecimalMin("0.00") BigDecimal baseImponible,
    @NotNull @DecimalMin("0.00") BigDecimal porcentajeRetener,
    @NotNull @DecimalMin("0.00") BigDecimal valorRetenido,
    String codDocSustento,
    String numDocSustento,
    LocalDate fechaEmisionDocSustento) {}
