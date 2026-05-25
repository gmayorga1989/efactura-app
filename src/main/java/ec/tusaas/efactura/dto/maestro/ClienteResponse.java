package ec.tusaas.efactura.dto.maestro;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ClienteResponse(
    UUID id,
    UUID empresaId,
    String tipoIdentificacion,
    String identificacion,
    String razonSocial,
    String nombreComercial,
    String tipoTercero,
    String direccion,
    String telefono,
    String email,
    String contactoNombre,
    String contactoTelefono,
    String contactoEmail,
    String obligadoContabilidad,
    String contribuyenteEspecial,
    String regimen,
    String estadoSri,
    String actividadEconomica,
    String fuenteDatos,
    List<ClienteDireccionResponse> direcciones,
    String estado,
    Map<String, Object> customData) {}
