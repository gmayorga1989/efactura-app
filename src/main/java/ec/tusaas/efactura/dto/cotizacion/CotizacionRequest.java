package ec.tusaas.efactura.dto.cotizacion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CotizacionRequest(
    LocalDate fechaEmision,
    Integer validezDias,
    UUID clienteId,
    UUID vendedorId,
    @NotBlank @Size(max = 2) String tipoIdentificacionReceptor,
    @NotBlank @Size(max = 20) String identificacionReceptor,
    @NotBlank @Size(max = 300) String razonSocialReceptor,
    @Email @Size(max = 255) String emailReceptor,
    @NotEmpty List<@Valid CotizacionItemRequest> items,
    List<@Valid CotizacionAdjuntoRequest> adjuntos,
    String introduccionHtml,
    String condicionesHtml,
    Map<String, Object> plantillaJson) {

  public LocalDate fechaEmisionOrToday() {
    return fechaEmision == null ? LocalDate.now() : fechaEmision;
  }

  public int validezDiasOrDefault() {
    return validezDias == null || validezDias < 1 ? 15 : validezDias;
  }
}
