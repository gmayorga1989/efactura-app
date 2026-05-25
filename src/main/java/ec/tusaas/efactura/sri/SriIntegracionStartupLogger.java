package ec.tusaas.efactura.sri;

import ec.tusaas.efactura.config.props.SriProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SriIntegracionStartupLogger implements ApplicationRunner {

  private final SriProperties sriProperties;

  @Override
  public void run(ApplicationArguments args) {
    if (sriProperties.isSoapEnabled()) {
      log.info(
          "SRI SOAP activo. Recepción pruebas: {} | Autorización pruebas: {}",
          sriProperties.getRecepcionUrlPruebas(),
          sriProperties.getAutorizacionUrlPruebas());
      return;
    }
    log.warn(
        """
        SRI SOAP DESACTIVADO (efactura.sri.soap-enabled=false / SRI_SOAP_ENABLED=false).
        Los comprobantes se firman localmente pero NO se envían al SRI.
        Para habilitar envío real en pruebas: export SRI_SOAP_ENABLED=true y reinicie la aplicación.
        """);
  }
}
