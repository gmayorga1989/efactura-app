package ec.tusaas.efactura.integration.sridownload;

import ec.tusaas.efactura.config.props.SriDownloadIntegrationProperties;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class SriDownloadIntegrationClient {

  private static final String API_PREFIX = "/api/sri-download/v1/integrations/efactura";

  private final RestTemplate sriDownloadRestTemplate;
  private final SriDownloadIntegrationProperties properties;

  public Map<String, Object> provision(Map<String, Object> body) {
    HttpHeaders headers = apiHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return exchange(
        HttpMethod.POST,
        API_PREFIX + "/provision",
        new HttpEntity<>(body, headers),
        new ParameterizedTypeReference<>() {});
  }

  public Map<String, Object> getCredentialStatus(String empresaId) {
    return exchange(
        HttpMethod.GET,
        API_PREFIX + "/empresas/" + empresaId + "/credentials",
        null,
        new ParameterizedTypeReference<>() {});
  }

  public Map<String, Object> upsertCredentials(String empresaId, Map<String, Object> body) {
    HttpHeaders headers = apiHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return exchange(
        HttpMethod.PUT,
        API_PREFIX + "/empresas/" + empresaId + "/credentials",
        new HttpEntity<>(body, headers),
        new ParameterizedTypeReference<>() {});
  }

  public void deleteCredentials(String empresaId) {
    try {
      sriDownloadRestTemplate.exchange(
          API_PREFIX + "/empresas/" + empresaId + "/credentials",
          HttpMethod.DELETE,
          new HttpEntity<>(apiHeaders()),
          Void.class);
    } catch (HttpStatusCodeException ex) {
      throw ex;
    }
  }

  public Map<String, Object> getLatestSync(String empresaId) {
    return exchange(
        HttpMethod.GET,
        API_PREFIX + "/empresas/" + empresaId + "/sync/latest",
        null,
        new ParameterizedTypeReference<>() {});
  }

  public Map<String, Object> triggerSync(String empresaId, Map<String, Object> body) {
    HttpHeaders headers = apiHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return exchange(
        HttpMethod.POST,
        API_PREFIX + "/empresas/" + empresaId + "/sync",
        new HttpEntity<>(body != null ? body : Map.of(), headers),
        new ParameterizedTypeReference<>() {});
  }

  public Map<String, Object> getIntegrationStatus(String empresaId) {
    return exchange(
        HttpMethod.GET,
        API_PREFIX + "/empresas/" + empresaId + "/status",
        null,
        new ParameterizedTypeReference<>() {});
  }

  public Map<String, Object> listComprobantes(
      String empresaId,
      int page,
      int size,
      java.time.LocalDate fechaDesde,
      java.time.LocalDate fechaHasta,
      String claveAcceso,
      String rucEmisor,
      String razonSocial) {
    StringBuilder path =
        new StringBuilder(
            API_PREFIX
                + "/empresas/"
                + empresaId
                + "/comprobantes?page="
                + page
                + "&size="
                + size);
    appendQuery(path, "fechaDesde", fechaDesde);
    appendQuery(path, "fechaHasta", fechaHasta);
    appendQuery(path, "claveAcceso", claveAcceso);
    appendQuery(path, "rucEmisor", rucEmisor);
    appendQuery(path, "razonSocial", razonSocial);
    return exchange(HttpMethod.GET, path.toString(), null, new ParameterizedTypeReference<>() {});
  }

  public List<Map<String, Object>> resumenMensual(String empresaId, int anio) {
    return exchange(
        HttpMethod.GET,
        API_PREFIX + "/empresas/" + empresaId + "/comprobantes/resumen-mensual?anio=" + anio,
        null,
        new ParameterizedTypeReference<>() {});
  }

  public byte[] downloadComprobanteXml(String empresaId, UUID comprobanteId) {
    return sriDownloadRestTemplate.exchange(
        API_PREFIX + "/empresas/" + empresaId + "/comprobantes/" + comprobanteId + "/xml",
        HttpMethod.GET,
        new HttpEntity<>(apiHeaders()),
        byte[].class).getBody();
  }

  public String syncEventsUrl(String empresaId, UUID syncRunId) {
    return API_PREFIX + "/empresas/" + empresaId + "/sync-runs/" + syncRunId + "/events";
  }

  private static void appendQuery(StringBuilder path, String key, Object value) {
    if (value == null) {
      return;
    }
    String s = String.valueOf(value).trim();
    if (s.isEmpty()) {
      return;
    }
    path.append('&').append(key).append('=').append(java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8));
  }

  public Map<String, Object> getComprobante(String empresaId, UUID comprobanteId) {
    return exchange(
        HttpMethod.GET,
        API_PREFIX + "/empresas/" + empresaId + "/comprobantes/" + comprobanteId,
        null,
        new ParameterizedTypeReference<>() {});
  }

  public List<Map<String, Object>> listSyncRuns(String empresaId, int limit) {
    return exchange(
        HttpMethod.GET,
        API_PREFIX + "/empresas/" + empresaId + "/sync-runs?limit=" + limit,
        null,
        new ParameterizedTypeReference<>() {});
  }

  public List<Map<String, Object>> listBotLogs(String empresaId, UUID syncRunId) {
    return exchange(
        HttpMethod.GET,
        API_PREFIX + "/empresas/" + empresaId + "/sync-runs/" + syncRunId + "/bot-logs",
        null,
        new ParameterizedTypeReference<>() {});
  }

  public Map<String, Object> getApiKeyStatus(String empresaId) {
    return exchange(
        HttpMethod.GET,
        API_PREFIX + "/empresas/" + empresaId + "/integration/api-key",
        null,
        new ParameterizedTypeReference<>() {});
  }

  public Map<String, Object> generateApiKey(String empresaId) {
    return exchange(
        HttpMethod.POST,
        API_PREFIX + "/empresas/" + empresaId + "/integration/api-key",
        new HttpEntity<>(Map.of(), apiHeaders()),
        new ParameterizedTypeReference<>() {});
  }

  public void revokeApiKey(String empresaId) {
    sriDownloadRestTemplate.exchange(
        API_PREFIX + "/empresas/" + empresaId + "/integration/api-key",
        HttpMethod.DELETE,
        new HttpEntity<>(apiHeaders()),
        Void.class);
  }

  public List<Map<String, Object>> listWebhooks(String empresaId) {
    return exchange(
        HttpMethod.GET,
        API_PREFIX + "/empresas/" + empresaId + "/integration/webhooks",
        null,
        new ParameterizedTypeReference<>() {});
  }

  public Map<String, Object> upsertWebhook(String empresaId, Map<String, Object> body) {
    HttpHeaders headers = apiHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return exchange(
        HttpMethod.PUT,
        API_PREFIX + "/empresas/" + empresaId + "/integration/webhooks",
        new HttpEntity<>(body, headers),
        new ParameterizedTypeReference<>() {});
  }

  public void deleteWebhook(String empresaId, UUID webhookId) {
    sriDownloadRestTemplate.exchange(
        API_PREFIX + "/empresas/" + empresaId + "/integration/webhooks/" + webhookId,
        HttpMethod.DELETE,
        new HttpEntity<>(apiHeaders()),
        Void.class);
  }

  private <T> T exchange(
      HttpMethod method,
      String path,
      HttpEntity<?> entity,
      ParameterizedTypeReference<T> type) {
    HttpEntity<?> request = entity != null ? entity : new HttpEntity<>(apiHeaders());
    ResponseEntity<T> response = sriDownloadRestTemplate.exchange(path, method, request, type);
    return response.getBody();
  }

  private HttpHeaders apiHeaders() {
    HttpHeaders headers = new HttpHeaders();
    String key = properties.serviceApiKey();
    if (key != null && !key.isBlank()) {
      headers.set("X-Api-Key", key);
    }
    return headers;
  }
}
