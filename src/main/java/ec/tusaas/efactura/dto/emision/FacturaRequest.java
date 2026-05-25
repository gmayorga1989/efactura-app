package ec.tusaas.efactura.dto.emision;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record FacturaRequest(
    @NotNull UUID puntoEmisionId,
    LocalDate fechaEmision,
    @NotBlank @Size(max = 2) String tipoIdentificacionReceptor,
    @NotBlank @Size(max = 20) String identificacionReceptor,
    @NotBlank @Size(max = 300) String razonSocialReceptor,
    @Email @Size(max = 255) String emailReceptor,
    @NotEmpty List<@Valid FacturaItemRequest> items,
    List<@Valid PagoRequest> pagos,
    Map<String, Object> customData,
    List<@Email String> notificarEmails) {

  public LocalDate fechaEmisionOrToday() {
    return fechaEmision == null ? LocalDate.now() : fechaEmision;
  }

  public Map<String, Object> safeCustomData() {
    return customData == null ? new HashMap<>() : customData;
  }
}
