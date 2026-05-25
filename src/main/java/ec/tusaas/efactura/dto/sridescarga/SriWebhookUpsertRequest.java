package ec.tusaas.efactura.dto.sridescarga;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

public record SriWebhookUpsertRequest(
    UUID id, @NotBlank String url, String secret, List<String> eventos) {}
