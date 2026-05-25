package ec.tusaas.efactura.dto.apikey;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApiKeyResponse(
    UUID id,
    String nombre,
    String prefix,
    List<String> scopes,
    String estado,
    Instant fechaExpiracion,
    Instant ultimoUso,
    Instant fechaCreacion) {}
