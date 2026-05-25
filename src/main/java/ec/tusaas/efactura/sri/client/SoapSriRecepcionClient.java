package ec.tusaas.efactura.sri.client;

import ec.tusaas.efactura.config.props.SriProperties;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
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
public class SoapSriRecepcionClient implements SriRecepcionClient {

  private final SriProperties sriProperties;

  @Override
  public RecepcionResult enviar(String xmlFirmado, short ambienteSri) {
    String endpoint = SoapXml.stripWsdl(ambienteSri == 2 ? sriProperties.getRecepcionUrlProduccion() : sriProperties.getRecepcionUrlPruebas());
    String body = recepcionEnvelope(xmlFirmado);
    var rest =
        new RestTemplateBuilder()
            .setConnectTimeout(Duration.ofMillis(sriProperties.getRecepcionTimeoutMs()))
            .setReadTimeout(Duration.ofMillis(sriProperties.getRecepcionTimeoutMs()))
            .build();
    try {
      log.info("SRI SOAP recepción POST ambiente={} endpoint={}", ambienteSri, endpoint);
      String response = rest.postForObject(endpoint, new HttpEntity<>(body, headers()), String.class);
      String estado = SoapXml.textByLocalName(response, "estado");
      String mensaje = SriRespuestaHistorialParser.resumenLegible(response, null);
      if (mensaje.isBlank()) {
        mensaje = SoapXml.textByLocalName(response, "mensaje");
      }
      log.info("SRI SOAP recepción respuesta estado={} mensaje={}", estado, mensaje);
      if (log.isDebugEnabled()) {
        log.debug("SRI SOAP recepción XML respuesta: {}", response);
      }
      return new RecepcionResult(
          estado == null || estado.isBlank() ? "ERROR" : estado,
          mensaje,
          200,
          response == null ? "" : response);
    } catch (Exception e) {
      log.error("SRI SOAP recepción falló ambiente={} endpoint={}: {}", ambienteSri, endpoint, e.getMessage(), e);
      return new RecepcionResult("ERROR", e.getMessage(), 502, e.toString());
    }
  }

  private static HttpHeaders headers() {
    HttpHeaders h = new HttpHeaders();
    h.setContentType(MediaType.TEXT_XML);
    h.add("SOAPAction", "");
    return h;
  }

  private static String recepcionEnvelope(String xmlFirmado) {
    String base64 = Base64.getEncoder().encodeToString(xmlFirmado.getBytes(StandardCharsets.UTF_8));
    return """
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ec="http://ec.gob.sri.ws.recepcion">
          <soapenv:Header/>
          <soapenv:Body>
            <ec:validarComprobante>
              <xml>%s</xml>
            </ec:validarComprobante>
          </soapenv:Body>
        </soapenv:Envelope>
        """
        .formatted(base64);
  }
}
