package ec.tusaas.efactura.emision;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.tusaas.efactura.dto.emision.DocumentoModificadoRequest;
import ec.tusaas.efactura.dto.emision.FacturaItemRequest;
import ec.tusaas.efactura.entity.Comprobante;
import ec.tusaas.efactura.entity.Empresa;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class XmlNotaDebitoGeneratorServiceTest {

  private final XmlNotaDebitoGeneratorService generator = new XmlNotaDebitoGeneratorService();

  @Test
  void infoNotaDebito_debeLlevarImpuestosAntesDeValorTotal() {
    Comprobante c = comprobanteBase();
    DocumentoModificadoRequest req = requestBase();
    String xml =
        generator.generarXmlInicial(
            c,
            req,
            new XmlNotaDebitoGeneratorService.DocumentoModificado("001-001-000000001", LocalDate.of(2026, 5, 1)),
            List.of());

    int impuestos = xml.indexOf("<impuestos>");
    int impuesto = xml.indexOf("<impuesto>");
    int valorTotal = xml.indexOf("<valorTotal>");
    assertTrue(impuestos >= 0, "debe existir bloque impuestos");
    assertTrue(impuesto > impuestos, "debe existir al menos un impuesto");
    assertTrue(valorTotal > impuesto, "valorTotal debe ir despues de impuestos");
    assertTrue(!xml.contains("<totalConImpuestos>"), "nota debito no usa totalConImpuestos en info");
    assertTrue(xml.contains("<motivos>"), "debe incluir motivos");
  }

  private static Comprobante comprobanteBase() {
    Empresa empresa = new Empresa();
    empresa.setRuc("1799999999001");
    empresa.setRazonSocial("Empresa Test");
    empresa.setNombreComercial("Empresa Test");
    empresa.setDireccionMatriz("Quito");
    empresa.setObligadoContabilidad(true);
    empresa.setAmbienteSri((short) 1);
    empresa.setTipoEmision((short) 1);

    Comprobante c = new Comprobante();
    c.setEmpresa(empresa);
    c.setTipoCodigo("05");
    c.setEstablecimientoCodigo("001");
    c.setPuntoEmisionCodigo("001");
    c.setSecuencial("000000001");
    c.setClaveAcceso("2005202605179323037800110010010000000011875749011");
    c.setFechaEmision(LocalDate.of(2026, 5, 20));
    c.setRazonSocialReceptor("Cliente");
    c.setIdentificacionReceptor("0999999999");
    c.setSubtotalSinImpuestos(new BigDecimal("100.00"));
    c.setIvaTotal(BigDecimal.ZERO);
    c.setValorTotal(new BigDecimal("100.00"));
    c.setMoneda("DOLAR");
    return c;
  }

  private static DocumentoModificadoRequest requestBase() {
    return new DocumentoModificadoRequest(
        UUID.randomUUID(),
        LocalDate.of(2026, 5, 20),
        "04",
        "0999999999",
        "Cliente",
        UUID.randomUUID(),
        "Ajuste",
        List.of(
            new FacturaItemRequest(
                "ND01",
                null,
                "Cargo",
                BigDecimal.ONE,
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "0",
                Map.of())),
        Map.of("motivo", "Ajuste"));
  }
}
