package ec.tusaas.efactura.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "efactura.integraciones.cloud")
public record CloudDriveProperties(
    boolean enabled,
    String googleClientId,
    String googleClientSecret,
    String googleRedirectUri,
    String googleApiKey,
    String microsoftClientId,
    String microsoftClientSecret,
    String microsoftRedirectUri,
    String microsoftTenant) {

  public boolean googleConfigured() {
    return googleClientId != null
        && !googleClientId.isBlank()
        && googleClientSecret != null
        && !googleClientSecret.isBlank()
        && googleRedirectUri != null
        && !googleRedirectUri.isBlank();
  }

  public boolean microsoftConfigured() {
    return microsoftClientId != null
        && !microsoftClientId.isBlank()
        && microsoftClientSecret != null
        && !microsoftClientSecret.isBlank()
        && microsoftRedirectUri != null
        && !microsoftRedirectUri.isBlank();
  }
}
