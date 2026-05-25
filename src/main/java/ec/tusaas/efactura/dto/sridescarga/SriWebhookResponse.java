package ec.tusaas.efactura.dto.sridescarga;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SriWebhookResponse(
    UUID id, String url, List<String> eventos, String estado, Instant fechaCreacion) {}
