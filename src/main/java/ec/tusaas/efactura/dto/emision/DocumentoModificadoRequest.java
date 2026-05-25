package ec.tusaas.efactura.dto.emision;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Solicitud de nota de credito/debito u otro documento que modifica una factura. */
public record DocumentoModificadoRequest(
    @NotNull UUID puntoEmisionId,
    LocalDate fechaEmision,
    @NotBlank @Size(max = 2) String tipoIdentificacionReceptor,
    @NotBlank @Size(max = 20) String identificacionReceptor,
    @NotBlank @Size(max = 300) String razonSocialReceptor,
    UUID facturaModificadaId,
    @Size(max = 300) String motivo,
    List<@Valid FacturaItemRequest> items,
    Map<String, Object> customData) {

  public LocalDate fechaEmisionOrToday() {
    return fechaEmision == null ? LocalDate.now() : fechaEmision;
  }

  public Map<String, Object> safeCustomData() {
    return customData == null ? new HashMap<>() : customData;
  }
}
