package ec.tusaas.efactura.dto.invitacion;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Aceptar invitación. Si la identidad ya existe, la contraseña debe ser la actual.")
public record AcceptInviteRequest(
    @NotBlank String token,
    @NotBlank @Size(min = 8, max = 100) String password,
    @Size(max = 200) @Schema(nullable = true, description = "Requerido si el email no tenía cuenta previa") String nombre) {}
