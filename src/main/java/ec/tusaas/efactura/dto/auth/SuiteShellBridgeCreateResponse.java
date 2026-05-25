package ec.tusaas.efactura.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SuiteShellBridgeCreateResponse(@JsonProperty("bridgeId") String bridgeId) {}
