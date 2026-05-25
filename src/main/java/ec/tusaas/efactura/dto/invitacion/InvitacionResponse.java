package ec.tusaas.efactura.dto.invitacion;

import java.time.Instant;
import java.util.UUID;

public record InvitacionResponse(
    UUID id,
    String email,
    String rolCodigo,
    String estado,
    Instant expiraEn,
    String invitadoPorEmail,
    Instant fechaCreacion,
    boolean expirada) {}
