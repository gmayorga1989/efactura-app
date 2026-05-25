package ec.tusaas.efactura.integration.sridownload;

import ec.tusaas.efactura.config.props.SriDownloadIntegrationProperties;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Component
@RequiredArgsConstructor
public class SriDownloadSyncEventStreamClient {

  private final SriDownloadIntegrationProperties properties;
  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

  public StreamingResponseBody openStream(String relativePath) {
    return outputStream -> {
      HttpRequest.Builder builder =
          HttpRequest.newBuilder()
              .uri(URI.create(properties.baseUrl() + relativePath))
              .timeout(Duration.ZERO)
              .GET()
              .header("Accept", "text/event-stream");
      String key = properties.serviceApiKey();
      if (key != null && !key.isBlank()) {
        builder.header("X-Api-Key", key);
      }
      HttpResponse<InputStream> response;
      try {
        response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new IOException("SSE interrumpido", ex);
      }
      if (response.statusCode() >= 400) {
        throw new IllegalStateException("SSE remoto respondió HTTP " + response.statusCode());
      }
      try (InputStream in = response.body()) {
        in.transferTo(outputStream);
      }
    };
  }
}
