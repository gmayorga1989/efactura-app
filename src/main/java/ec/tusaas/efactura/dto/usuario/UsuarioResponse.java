package ec.tusaas.efactura.dto.usuario;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UsuarioResponse(
    UUID membresiaId,
    UUID identidadId,
    UUID empresaId,
    String email,
    String nombre,
    String avatarUrl,
    String estado,
    boolean enLinea,
    Instant ultimoPing,
    List<String> roles) {}
