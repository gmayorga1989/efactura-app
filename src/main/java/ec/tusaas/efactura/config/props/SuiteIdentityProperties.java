package ec.tusaas.efactura.config.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "efactura.suite.identity")
public class SuiteIdentityProperties {

  /**
   * Si es true, se expone POST /api/web/v1/auth/suite/exchange y se validan JWT emitidos por el Identity
   * Gateway.
   */
  private boolean enabled = false;

  /** Issuer del JWT (claim iss), p. ej. http://localhost:8092 */
  private String issuer = "";

  /** Audience (claim aud), p. ej. suite */
  private String audience = "suite";

  /** Misma clave HS256 que suite.identity.jwt.secret en el gateway. */
  private String secret = "";

  /** URL base del gateway (respuesta pública para el front). */
  private String identityBaseUrl = "";

  /** Slug de compañía en Identity (auth.company.slug) para el login desde el front. */
  private String companySlug = "";

  /**
   * Si es true, expone {@code POST /api/public/v1/auth/suite/bootstrap} para crear empresa + identidad + membresía
   * validando el access token de Identity.
   */
  private boolean publicBootstrapEnabled = true;

  /** URL base del shell Suite (product picker); usada en respuesta publica y enlaces desde eFactura. */
  private String suiteShellBaseUrl = "";

  /** URL base de la UI Cartera (informativo / futuros enlaces directos). */
  private String carteraBaseUrl = "";

  /** URL base de la UI POS (informativo / futuros enlaces directos). */
  private String posBaseUrl = "";
}
