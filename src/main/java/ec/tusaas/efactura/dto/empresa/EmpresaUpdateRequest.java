package ec.tusaas.efactura.dto.empresa;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmpresaUpdateRequest(
    @Size(min = 13, max = 13) @Pattern(regexp = "\\d{13}") String ruc,
    @Size(max = 300) String razonSocial,
    @Size(max = 300) String nombreComercial,
    Boolean obligadoContabilidad,
    @Size(max = 20) String contribuyenteEspecial,
    Boolean exportadorHabitual,
    Boolean calificacionArtesanal,
    @Size(max = 50) String codigoArtesano,
    Boolean agenteRetencion,
    @Size(max = 500) String direccionMatriz,
    @Size(max = 50) String timezone,
    @Pattern(regexp = "[A-Z]{2}") @Size(min = 2, max = 2) String paisIso) {}
