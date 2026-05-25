package ec.tusaas.efactura.config;

import ec.tusaas.efactura.config.props.BootstrapProperties;
import ec.tusaas.efactura.config.props.InvitacionProperties;
import ec.tusaas.efactura.config.props.JwtProperties;
import ec.tusaas.efactura.config.props.NotificationProperties;
import ec.tusaas.efactura.config.props.SriDownloadIntegrationProperties;
import ec.tusaas.efactura.config.props.SriProperties;
import ec.tusaas.efactura.config.props.StorageProperties;
import ec.tusaas.efactura.config.props.SuiteIdentityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
  JwtProperties.class,
  BootstrapProperties.class,
  SriProperties.class,
  StorageProperties.class,
  InvitacionProperties.class,
  NotificationProperties.class,
  SuiteIdentityProperties.class,
  SriDownloadIntegrationProperties.class
})
public class EfacturaPropertiesConfig {}
