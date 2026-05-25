package ec.tusaas.efactura.sri.client;

import ec.tusaas.efactura.emision.EstadoSri;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "efactura.sri", name = "soap-enabled", havingValue = "false", matchIfMissing = true)
public class StubSriAutorizacionClient implements SriAutorizacionClient {

  private static final String MENSAJE =
      "Integración SRI desactivada: no se puede consultar autorización sin SOAP (SRI_SOAP_ENABLED=true).";

  @Override
  public AutorizacionResult consultar(String claveAcceso, short ambienteSri) {
    log.warn("SRI autorización omitida (stub): clave={} ambiente={}", claveAcceso, ambienteSri);
    return new AutorizacionResult(EstadoSri.ERROR, null, null, MENSAJE, "SRI_AUTORIZACION_STUB");
  }
}
