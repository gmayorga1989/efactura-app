package ec.tusaas.efactura.security;

import ec.tusaas.efactura.config.props.SuiteIdentityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * Valida access tokens emitidos por el Identity Gateway (mismo HS256, iss, aud) sin mezclar emisión de tokens
 * Suite en eFactura.
 */
@Component
public class SuiteIdentityJwtService {

  public static final String CLAIM_TYP = "typ";
  public static final String TYP_ACCESS = "access";
  public static final String CLAIM_COMPANY_ID = "company_id";
  public static final String CLAIM_SCOPE = "scope";

  private final SuiteIdentityProperties props;
  private final SecretKey signingKey;

  public SuiteIdentityJwtService(SuiteIdentityProperties props) {
    this.props = props;
    byte[] keyBytes = props.getSecret() == null ? new byte[0] : props.getSecret().getBytes(StandardCharsets.UTF_8);
    if (keyBytes.length >= 32) {
      this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    } else {
      this.signingKey = null;
    }
  }

  /** Issuer y secreto HS256 listos (sin mirar el flag {@code enabled}). */
  public boolean cryptoParametersReady() {
    return props.getIssuer() != null
        && !props.getIssuer().isBlank()
        && props.getSecret() != null
        && props.getSecret().getBytes(StandardCharsets.UTF_8).length >= 32;
  }

  public boolean isConfigured() {
    return props.isEnabled() && cryptoParametersReady();
  }

  public SuiteIdentityAccessClaims parseAccessToken(String token) {
    if (!isConfigured() || signingKey == null) {
      throw new JwtException("Suite Identity JWT no configurado");
    }
    Claims claims =
        Jwts.parser()
            .verifyWith(signingKey)
            .requireIssuer(props.getIssuer())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    assertAudience(claims, props.getAudience());
    if (!TYP_ACCESS.equals(claims.get(CLAIM_TYP, String.class))) {
      throw new JwtException("Token no es de acceso");
    }
    UUID companyId = UUID.fromString(claims.get(CLAIM_COMPANY_ID, String.class));
    String email = claims.get("email", String.class);
    String scopeStr = claims.get(CLAIM_SCOPE, String.class);
    List<String> scopes =
        scopeStr == null || scopeStr.isBlank()
            ? List.of()
            : List.of(scopeStr.trim().split("\\s+"));
    return new SuiteIdentityAccessClaims(companyId, email, scopes);
  }

  private static void assertAudience(Claims claims, String expected) {
    Object aud = claims.get("aud");
    if (aud instanceof String s && expected.equals(s)) {
      return;
    }
    if (aud instanceof Collection<?> col
        && col.stream().map(String::valueOf).anyMatch(expected::equals)) {
      return;
    }
    throw new JwtException("audience inválida");
  }

  public record SuiteIdentityAccessClaims(UUID companyId, String email, List<String> scopes) {

    public boolean canAccessEfactura() {
      return scopes.contains("efactura.access") || scopes.contains("suite.admin");
    }
  }
}
