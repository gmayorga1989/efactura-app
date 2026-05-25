package ec.tusaas.efactura.dto.auth;

public record TokenResponse(
    String tokenType, String accessToken, String refreshToken, long expiresInSeconds) {}
