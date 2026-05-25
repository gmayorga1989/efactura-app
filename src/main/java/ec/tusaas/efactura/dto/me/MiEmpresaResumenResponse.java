package ec.tusaas.efactura.dto.me;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Resumen de una membresía para selector o listado de empresas")
public record MiEmpresaResumenResponse(
    UUID membresiaId,
    @Schema(nullable = true) UUID empresaId,
    boolean esPlataforma,
    String razonSocial,
    @Schema(nullable = true) String nombreComercial,
    @Schema(nullable = true) String ruc,
    @Schema(nullable = true) String slug,
    String estadoMembresia,
    boolean empresaActiva,
    boolean esContextoActual) {}
