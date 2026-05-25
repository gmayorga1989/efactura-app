package ec.tusaas.efactura.config.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "efactura.notifications")
public class NotificationProperties {

  private boolean enabled = false;
  private String provider = "LOG";
  private String appBaseUrl = "http://localhost:3000";
  private Brevo brevo = new Brevo();

  @Getter
  @Setter
  public static class Brevo {
    private String baseUrl = "https://api.brevo.com";
    private String apiKey = "";
    private String senderEmail = "no-reply@efactura.local";
    private String senderName = "eFactura";
  }
}
