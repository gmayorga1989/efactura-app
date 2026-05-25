package ec.tusaas.efactura.dto.sridescarga;

import java.time.Instant;
import java.util.UUID;

public record SriBotLogResponse(
    UUID id,
    UUID syncRunId,
    String tipoOperacion,
    String estado,
    String mensaje,
    Integer duracionMs,
    Instant fecha) {}
