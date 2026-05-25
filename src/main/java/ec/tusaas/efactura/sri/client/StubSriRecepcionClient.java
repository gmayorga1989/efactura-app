package ec.tusaas.efactura.sri.client;

import ec.tusaas.efactura.emision.EstadoSri;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "efactura.sri", name = "soap-enabled", havingValue = "false", matchIfMissing = true)
public class StubSriRecepcionClient implements SriRecepcionClient {

  private static final String MENSAJE =
      "Integración SRI desactivada: active efactura.sri.soap-enabled=true (variable SRI_SOAP_ENABLED=true) "
          + "y reinicie el servidor para enviar comprobantes al SRI.";

  @Override
  public RecepcionResult enviar(String xmlFirmado, short ambienteSri) {
    log.warn("SRI recepción omitida (stub): ambiente={} bytesXml={}", ambienteSri, xmlFirmado == null ? 0 : xmlFirmado.length());
    return new RecepcionResult(EstadoSri.ERROR, MENSAJE, 503, "SRI_RECEPCION_STUB");
  }
}
