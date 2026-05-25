package ec.tusaas.efactura.dto.empresa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmpresaCreateRequest(
    @NotBlank @Size(min = 13, max = 13) @Pattern(regexp = "\\d{13}") String ruc,
    @NotBlank @Size(max = 300) String razonSocial,
    @Size(max = 300) String nombreComercial,
    boolean obligadoContabilidad,
    @Size(max = 20) String contribuyenteEspecial,
    boolean exportadorHabitual,
    boolean calificacionArtesanal,
    @Size(max = 50) String codigoArtesano,
    boolean agenteRetencion,
    @Size(max = 500) String direccionMatriz,
    @Size(max = 50) String timezone) {}
