package ec.tusaas.efactura.dto.me;

import java.time.Instant;
import java.util.UUID;

public record PresenceResponse(UUID identidadId, boolean enLinea, Instant ultimoPing) {}
