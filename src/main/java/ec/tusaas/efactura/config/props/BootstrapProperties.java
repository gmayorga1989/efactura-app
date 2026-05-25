package ec.tusaas.efactura.config.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "efactura.bootstrap")
public class BootstrapProperties {

  private boolean enabled = true;
  private String platformAdminEmail = "platform@efactura.local";
  private String platformAdminPassword = "Admin123!";
  private String demoRuc = "1790012345001";
  private String demoAdminEmail = "admin@demo.local";
  private String demoAdminPassword = "Admin123!";
}
