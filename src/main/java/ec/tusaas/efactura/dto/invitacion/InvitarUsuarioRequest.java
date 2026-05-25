package ec.tusaas.efactura.dto.invitacion;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InvitarUsuarioRequest(
    @NotBlank @Email String email, @NotBlank @Size(max = 50) String rolCodigo) {}
