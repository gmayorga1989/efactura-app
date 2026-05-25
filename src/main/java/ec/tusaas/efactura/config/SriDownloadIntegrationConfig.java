package ec.tusaas.efactura.config;

import ec.tusaas.efactura.config.props.SriDownloadIntegrationProperties;
import java.time.Duration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class SriDownloadIntegrationConfig {

  @Bean
  RestTemplate sriDownloadRestTemplate(
      SriDownloadIntegrationProperties properties, RestTemplateBuilder builder) {
    String baseUrl =
        properties.baseUrl() != null && !properties.baseUrl().isBlank()
            ? properties.baseUrl().replaceAll("/$", "")
            : "http://localhost:8085";
    int connectSec = properties.connectTimeoutSeconds() > 0 ? properties.connectTimeoutSeconds() : 15;
    int readSec = properties.readTimeoutSeconds() > 0 ? properties.readTimeoutSeconds() : 120;
    return builder
        .rootUri(baseUrl)
        .setConnectTimeout(Duration.ofSeconds(connectSec))
        .setReadTimeout(Duration.ofSeconds(readSec))
        .build();
  }
}
