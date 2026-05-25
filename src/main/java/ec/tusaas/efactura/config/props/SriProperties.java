package ec.tusaas.efactura.config.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "efactura.sri")
public class SriProperties {

  /** Activa llamadas SOAP reales al SRI. En local queda desactivado para evitar llamadas accidentales. */
  private boolean soapEnabled = false;

  /** URL recepción comprobantes (ambiente pruebas por defecto). */
  private String recepcionUrlPruebas =
      "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl";

  private String autorizacionUrlPruebas =
      "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl";

  private String recepcionUrlProduccion =
      "https://cel.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl";

  private String autorizacionUrlProduccion =
      "https://cel.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl";

  private int recepcionTimeoutMs = 30000;
  private int autorizacionTimeoutMs = 90000;

  private boolean xsdValidationEnabled = false;

  /** Ruta local al XSD de factura SRI vigente. */
  private String facturaXsdPath;

  /** Ruta local al XSD de nota de credito SRI vigente. */
  private String notaCreditoXsdPath;

  private String notaDebitoXsdPath;
  private String liquidacionCompraXsdPath;
  private String guiaRemisionXsdPath;
  private String retencionXsdPath;

  private String catastroBaseUrl =
      "https://srienlinea.sri.gob.ec/sri-catastro-sujeto-servicio-internet/rest";
}
