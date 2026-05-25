package ec.tusaas.efactura.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record SuiteShellBridgeCreateRequest(
    @NotBlank @JsonProperty("identityAccess") String identityAccess,
    @JsonProperty("identityRefresh") String identityRefresh) {}
