package ec.tusaas.efactura.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record SwitchEmpresaRequest(
    @Schema(description = "Empresa destino; null para contexto plataforma") UUID empresaId) {}
