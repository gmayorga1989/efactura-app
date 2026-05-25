package ec.tusaas.efactura.dto.me;

public record TwoFactorSetupResponse(
    String secret,
    String otpauthUri,
    boolean mfaHabilitado) {}
