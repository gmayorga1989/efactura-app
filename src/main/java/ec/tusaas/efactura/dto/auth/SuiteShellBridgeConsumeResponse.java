package ec.tusaas.efactura.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SuiteShellBridgeConsumeResponse(
    @JsonProperty("identityAccess") String identityAccess,
    @JsonProperty("identityRefresh") String identityRefresh) {}
