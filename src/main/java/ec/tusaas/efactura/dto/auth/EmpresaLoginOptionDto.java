package ec.tusaas.efactura.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Opción de empresa en el selector post-login")
public record EmpresaLoginOptionDto(
    @Schema(nullable = true) UUID empresaId,
    boolean esPlataforma,
    String razonSocial,
    @Schema(nullable = true) String nombreComercial,
    @Schema(nullable = true) String ruc,
    @Schema(nullable = true) String slug,
    boolean seleccionable,
    @Schema(nullable = true, description = "Motivo si seleccionable=false") String motivoNoSeleccion) {}
