package ec.tusaas.efactura.sri.client;

import ec.tusaas.efactura.config.props.SriProperties;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "efactura.sri", name = "soap-enabled", havingValue = "true")
public class SoapSriAutorizacionClient implements SriAutorizacionClient {

  private final SriProperties sriProperties;

  @Override
  public AutorizacionResult consultar(String claveAcceso, short ambienteSri) {
    String endpoint = SoapXml.stripWsdl(ambienteSri == 2 ? sriProperties.getAutorizacionUrlProduccion() : sriProperties.getAutorizacionUrlPruebas());
    String body = autorizacionEnvelope(claveAcceso);
    var rest =
        new RestTemplateBuilder()
            .setConnectTimeout(Duration.ofMillis(sriProperties.getAutorizacionTimeoutMs()))
            .setReadTimeout(Duration.ofMillis(sriProperties.getAutorizacionTimeoutMs()))
            .build();
    try {
      log.info("SRI SOAP autorización POST ambiente={} clave={} endpoint={}", ambienteSri, claveAcceso, endpoint);
      String response = rest.postForObject(endpoint, new HttpEntity<>(body, headers()), String.class);
      String estado = SoapXml.textByLocalName(response, "estado");
      String numeroAutorizacion = SoapXml.textByLocalName(response, "numeroAutorizacion");
      Instant fechaAutorizacion = parseInstant(SoapXml.textByLocalName(response, "fechaAutorizacion"));
      String mensaje = SoapXml.textByLocalName(response, "mensaje");
      log.info(
          "SRI SOAP autorización respuesta clave={} estado={} nro={} mensaje={}",
          claveAcceso,
          estado,
          numeroAutorizacion,
          mensaje);
      if (log.isDebugEnabled()) {
        log.debug("SRI SOAP autorización XML respuesta: {}", response);
      }
      return new AutorizacionResult(
          estado == null || estado.isBlank() ? "PENDIENTE_AUTORIZACION" : estado,
          numeroAutorizacion,
          fechaAutorizacion,
          mensaje,
          response == null ? "" : response);
    } catch (Exception e) {
      log.error(
          "SRI SOAP autorización falló clave={} ambiente={} endpoint={}: {}",
          claveAcceso,
          ambienteSri,
          endpoint,
          e.getMessage(),
          e);
      return new AutorizacionResult("ERROR", null, null, e.getMessage(), e.toString());
    }
  }

  private static HttpHeaders headers() {
    HttpHeaders h = new HttpHeaders();
    h.setContentType(MediaType.TEXT_XML);
    h.add("SOAPAction", "");
    return h;
  }

  private static String autorizacionEnvelope(String claveAcceso) {
    return """
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ec="http://ec.gob.sri.ws.autorizacion">
          <soapenv:Header/>
          <soapenv:Body>
            <ec:autorizacionComprobante>
              <claveAccesoComprobante>%s</claveAccesoComprobante>
            </ec:autorizacionComprobante>
          </soapenv:Body>
        </soapenv:Envelope>
        """
        .formatted(claveAcceso);
  }

  private static Instant parseInstant(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return OffsetDateTime.parse(value).toInstant();
    } catch (Exception ignored) {
      return null;
    }
  }
}
