package ec.tusaas.efactura.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "efactura.sri-download")
public record SriDownloadIntegrationProperties(
    boolean enabled,
    String baseUrl,
    String serviceApiKey,
    int connectTimeoutSeconds,
    int readTimeoutSeconds) {}
