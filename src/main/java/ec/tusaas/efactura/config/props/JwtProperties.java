package ec.tusaas.efactura.config.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "efactura.jwt")
public class JwtProperties {

  /** Secreto HS256 (mín. 256 bits). */
  private String secret = "";

  private int accessTokenMinutes = 15;
  private int refreshTokenDays = 7;

  /** JWT de un solo uso para POST /auth/select-empresa (minutos). */
  private int empresaSelectorMinutes = 10;

  /**
   * Si es mayor que 0, POST /auth/switch-empresa exige que el access token no sea más antiguo que estos
   * minutos (recomendado con MFA).
   */
  private int switchMaxTokenAgeMinutes = 0;

  /** Si es true y la identidad tiene MFA habilitado, switch-empresa responde 401. */
  private boolean switchRequireMfa = false;
}
