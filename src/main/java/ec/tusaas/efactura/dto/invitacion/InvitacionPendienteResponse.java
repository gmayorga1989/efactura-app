package ec.tusaas.efactura.dto.invitacion;

import java.time.Instant;
import java.util.UUID;

public record InvitacionPendienteResponse(
    UUID id,
    String email,
    String rolCodigo,
    Instant expiraEn,
    String invitadoPorEmail,
    Instant fechaCreacion) {}
