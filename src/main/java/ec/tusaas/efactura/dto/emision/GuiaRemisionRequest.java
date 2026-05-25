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

public record GuiaRemisionRequest(
    @NotNull UUID puntoEmisionId,
    LocalDate fechaEmision,
    @NotBlank @Size(max = 300) String dirPartida,
    @NotBlank @Size(max = 2) String tipoIdentificacionTransportista,
    @NotBlank @Size(max = 20) String identificacionTransportista,
    @NotBlank @Size(max = 300) String razonSocialTransportista,
    LocalDate fechaIniTransporte,
    LocalDate fechaFinTransporte,
    @NotBlank @Size(max = 20) String placa,
    @NotBlank @Size(max = 2) String tipoIdentificacionDestinatario,
    @NotBlank @Size(max = 20) String identificacionDestinatario,
    @NotBlank @Size(max = 300) String razonSocialDestinatario,
    @NotBlank @Size(max = 300) String dirDestinatario,
    @NotBlank @Size(max = 300) String motivoTraslado,
    UUID facturaSustentoId,
    List<@Valid FacturaItemRequest> items,
    Map<String, Object> customData) {

  public LocalDate fechaEmisionOrToday() {
    return fechaEmision == null ? LocalDate.now() : fechaEmision;
  }

  public LocalDate fechaIniTransporteOrToday() {
    return fechaIniTransporte == null ? fechaEmisionOrToday() : fechaIniTransporte;
  }

  public LocalDate fechaFinTransporteOrToday() {
    return fechaFinTransporte == null ? fechaIniTransporteOrToday() : fechaFinTransporte;
  }

  public Map<String, Object> safeCustomData() {
    return customData == null ? new HashMap<>() : customData;
  }
}
