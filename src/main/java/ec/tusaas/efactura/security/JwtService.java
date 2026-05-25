package ec.tusaas.efactura.security;

import ec.tusaas.efactura.config.props.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

  public static final String CLAIM_TYP = "typ";
  public static final String TYP_ACCESS = "access";
  public static final String TYP_SELECT_EMPRESA = "SELECT_EMPRESA";

  private final JwtProperties props;
  private final SecretKey signingKey;

  public JwtService(JwtProperties props) {
    this.props = props;
    byte[] keyBytes = props.getSecret().getBytes(StandardCharsets.UTF_8);
    if (keyBytes.length < 32) {
      throw new IllegalStateException("efactura.jwt.secret debe tener al menos 32 bytes (256 bits)");
    }
    this.signingKey = Keys.hmacShaKeyFor(keyBytes);
  }

  public String createAccessToken(
      UUID identidadId, UUID empresaId, String email, Collection<String> authorities) {
    Instant now = Instant.now();
    Instant exp = now.plusSeconds(props.getAccessTokenMinutes() * 60L);
    String authJoined =
        authorities == null || authorities.isEmpty()
            ? ""
            : String.join(",", authorities);
    return Jwts.builder()
        .subject(identidadId.toString())
        .issuedAt(Date.from(now))
        .expiration(Date.from(exp))
        .claim(CLAIM_TYP, TYP_ACCESS)
        .claim("email", email)
        .claim("empresaId", empresaId == null ? null : empresaId.toString())
        .claim("authorities", authJoined)
        .signWith(signingKey)
        .compact();
  }

  public String createSelectEmpresaTicket(UUID identidadId) {
    Instant now = Instant.now();
    Instant exp = now.plusSeconds(props.getEmpresaSelectorMinutes() * 60L);
    return Jwts.builder()
        .subject(identidadId.toString())
        .issuedAt(Date.from(now))
        .expiration(Date.from(exp))
        .claim(CLAIM_TYP, TYP_SELECT_EMPRESA)
        .signWith(signingKey)
        .compact();
  }

  public Claims parseAndVerify(String token) {
    return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
  }

  public UsuarioPrincipal parseAccessToken(String token) {
    Claims claims = parseAndVerify(token);
    String typ = claims.get(CLAIM_TYP, String.class);
    if (typ != null && !TYP_ACCESS.equals(typ)) {
      throw new JwtException("Token no es de acceso");
    }
    UUID identidadId = UUID.fromString(claims.getSubject());
    String email = claims.get("email", String.class);
    String empresaStr = claims.get("empresaId", String.class);
    UUID empresaId = empresaStr == null || empresaStr.isBlank() ? null : UUID.fromString(empresaStr);
    String authJoined = claims.get("authorities", String.class);
    List<String> codes =
        authJoined == null || authJoined.isBlank()
            ? List.of()
            : Arrays.stream(authJoined.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    return UsuarioPrincipal.authenticated(identidadId, empresaId, email, codes);
  }

  public UUID parseSelectEmpresaTicket(String token) {
    Claims claims = parseAndVerify(token);
    if (!TYP_SELECT_EMPRESA.equals(claims.get(CLAIM_TYP, String.class))) {
      throw new JwtException("Ticket de selección inválido");
    }
    return UUID.fromString(claims.getSubject());
  }
}
