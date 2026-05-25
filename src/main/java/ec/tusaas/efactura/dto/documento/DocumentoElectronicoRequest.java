package ec.tusaas.efactura.dto.documento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record DocumentoElectronicoRequest(
    @NotNull UUID puntoEmisionId,
    LocalDate fechaEmision,
    @NotBlank @Size(max = 2) String tipoIdentificacionReceptor,
    @NotBlank @Size(max = 20) String identificacionReceptor,
    @NotBlank @Size(max = 300) String razonSocialReceptor,
    BigDecimal subtotalSinImpuestos,
    BigDecimal descuentoTotal,
    BigDecimal ivaTotal,
    BigDecimal valorTotal,
    Map<String, Object> customData) {

  public LocalDate fechaEmisionOrToday() {
    return fechaEmision == null ? LocalDate.now() : fechaEmision;
  }

  public Map<String, Object> safeCustomData() {
    return customData == null ? new HashMap<>() : customData;
  }
}
