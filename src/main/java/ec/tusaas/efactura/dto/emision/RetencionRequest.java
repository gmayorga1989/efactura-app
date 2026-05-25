package ec.tusaas.efactura.dto.emision;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RetencionRequest(
    @NotNull UUID puntoEmisionId,
    LocalDate fechaEmision,
    @NotBlank @Size(max = 7) String periodoFiscal,
    @NotBlank @Size(max = 2) String tipoIdentificacionSujetoRetenido,
    @NotBlank @Size(max = 20) String identificacionSujetoRetenido,
    @NotBlank @Size(max = 300) String razonSocialSujetoRetenido,
    @NotEmpty List<@Valid RetencionImpuestoRequest> impuestos,
    Map<String, Object> customData) {

  public LocalDate fechaEmisionOrToday() {
    return fechaEmision == null ? LocalDate.now() : fechaEmision;
  }

  public Map<String, Object> safeCustomData() {
    return customData == null ? new HashMap<>() : customData;
  }
}
