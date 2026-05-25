package ec.tusaas.efactura.dto.invitacion;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(
    description =
        "Token en claro para enlaces de aceptación. En producción conviene enviar solo por correo y no devolverlo en la API.")
public record InvitacionCreadaResponse(
    UUID id, String token, Instant expiraEn, String acceptUrl, boolean emailEnviado) {}
