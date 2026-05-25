package ec.tusaas.efactura.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

@Schema(description = "Paso 2 del login. empresaId null selecciona sesión de plataforma (si existe membresía).")
public record SelectEmpresaRequest(
    @NotBlank @Schema(description = "JWT devuelto en login_step SELECT_EMPRESA") String sessionTicket,
    @Schema(
            nullable = true,
            description = "UUID de empresa destino; null solo si el usuario tiene membresía de plataforma") UUID empresaId) {}
