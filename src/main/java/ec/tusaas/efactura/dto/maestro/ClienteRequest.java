package ec.tusaas.efactura.dto.maestro;

import jakarta.validation.constraints.Email;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record ClienteRequest(
    @NotBlank @Size(max = 2) String tipoIdentificacion,
    @NotBlank @Size(max = 20) String identificacion,
    @NotBlank @Size(max = 300) String razonSocial,
    @Size(max = 300) String nombreComercial,
    @Size(max = 20) String tipoTercero,
    @Size(max = 500) String direccion,
    @Size(max = 50) String telefono,
    @Email @Size(max = 255) String email,
    @Size(max = 200) String contactoNombre,
    @Size(max = 50) String contactoTelefono,
    @Email @Size(max = 255) String contactoEmail,
    @Size(max = 2) String obligadoContabilidad,
    @Size(max = 50) String contribuyenteEspecial,
    @Size(max = 100) String regimen,
    @Size(max = 50) String estadoSri,
    @Size(max = 1000) String actividadEconomica,
    @Size(max = 30) String fuenteDatos,
    List<@Valid ClienteDireccionRequest> direcciones,
    Map<String, Object> customData) {

  public Map<String, Object> safeCustomData() {
    return customData == null ? new HashMap<>() : customData;
  }

  public List<ClienteDireccionRequest> safeDirecciones() {
    return direcciones == null ? List.of() : direcciones;
  }
}
