package ec.tusaas.efactura.sri.xml;

import ec.tusaas.efactura.config.props.SriProperties;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class XmlXsdValidatorService {

  private final SriProperties sriProperties;

  public void validarFactura(String xml) {
    validar(xml, sriProperties.getFacturaXsdPath(), "factura", "SRI_FACTURA_XSD_PATH");
  }

  public void validarNotaCredito(String xml) {
    validar(xml, sriProperties.getNotaCreditoXsdPath(), "nota de credito", "SRI_NOTA_CREDITO_XSD_PATH");
  }

  public void validarNotaDebito(String xml) {
    validar(xml, sriProperties.getNotaDebitoXsdPath(), "nota de debito", "SRI_NOTA_DEBITO_XSD_PATH");
  }

  public void validarLiquidacionCompra(String xml) {
    validar(
        xml, sriProperties.getLiquidacionCompraXsdPath(), "liquidacion de compra", "SRI_LIQUIDACION_COMPRA_XSD_PATH");
  }

  public void validarGuiaRemision(String xml) {
    validar(xml, sriProperties.getGuiaRemisionXsdPath(), "guia de remision", "SRI_GUIA_REMISION_XSD_PATH");
  }

  public void validarRetencion(String xml) {
    validar(xml, sriProperties.getRetencionXsdPath(), "comprobante de retencion", "SRI_RETENCION_XSD_PATH");
  }

  private void validar(String xml, String path, String etiqueta, String envVar) {
    if (!sriProperties.isXsdValidationEnabled()) {
      return;
    }
    if (path == null || path.isBlank()) {
      throw new IllegalStateException(envVar + " es obligatorio si SRI_XSD_VALIDATION_ENABLED=true");
    }
    Path xsd = Path.of(path).toAbsolutePath().normalize();
    if (!Files.exists(xsd)) {
      throw new IllegalStateException("XSD de " + etiqueta + " no existe: " + xsd);
    }
    try {
      SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      factory.newSchema(xsd.toFile()).newValidator().validate(new StreamSource(new StringReader(xml)));
    } catch (Exception e) {
      throw new IllegalStateException("XML de " + etiqueta + " no cumple XSD SRI: " + e.getMessage(), e);
    }
  }
}
