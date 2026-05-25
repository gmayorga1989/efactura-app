package ec.tusaas.efactura.emision;

import ec.tusaas.efactura.dto.emision.FacturaItemRequest;
import ec.tusaas.efactura.dto.emision.FacturaRequest;
import ec.tusaas.efactura.dto.emision.PagoRequest;
import ec.tusaas.efactura.entity.Comprobante;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Service
public class XmlFacturaGeneratorService {

  private static final DateTimeFormatter SRI_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  /**
   * Genera XML de factura con estructura cercana a la ficha técnica SRI. La validación final debe ejecutarse contra el
   * XSD oficial versionado antes de enviar a recepción.
   */
  public String generarXmlInicial(Comprobante c, FacturaRequest req) {
    try {
      Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      Element factura = doc.createElement("factura");
      factura.setAttribute("id", "comprobante");
      factura.setAttribute("version", "1.1.0");
      doc.appendChild(factura);

      factura.appendChild(infoTributaria(doc, c));
      factura.appendChild(infoFactura(doc, c, req));
      factura.appendChild(detalles(doc, req.items()));
      appendInfoAdicional(doc, factura, c.getCustomData());
      return serialize(doc);
    } catch (Exception e) {
      throw new IllegalStateException("No se pudo generar XML de factura", e);
    }
  }

  private static Element infoTributaria(Document doc, Comprobante c) {
    Element info = doc.createElement("infoTributaria");
    text(doc, info, "ambiente", Short.toString(c.getAmbienteSri()));
    text(doc, info, "tipoEmision", Short.toString(c.getTipoEmision()));
    text(doc, info, "razonSocial", c.getEmpresa().getRazonSocial());
    text(doc, info, "nombreComercial", c.getEmpresa().getNombreComercial());
    text(doc, info, "ruc", c.getEmpresa().getRuc());
    text(doc, info, "claveAcceso", c.getClaveAcceso());
    text(doc, info, "codDoc", c.getTipoCodigo());
    text(doc, info, "estab", c.getEstablecimientoCodigo());
    text(doc, info, "ptoEmi", c.getPuntoEmisionCodigo());
    text(doc, info, "secuencial", c.getSecuencial());
    text(doc, info, "dirMatriz", c.getEmpresa().getDireccionMatriz());
    return info;
  }

  private static Element infoFactura(Document doc, Comprobante c, FacturaRequest req) {
    Element info = doc.createElement("infoFactura");
    text(doc, info, "fechaEmision", c.getFechaEmision().format(SRI_DATE));
    text(doc, info, "obligadoContabilidad", c.getEmpresa().isObligadoContabilidad() ? "SI" : "NO");
    text(doc, info, "tipoIdentificacionComprador", req.tipoIdentificacionReceptor());
    text(doc, info, "razonSocialComprador", c.getRazonSocialReceptor());
    text(doc, info, "identificacionComprador", c.getIdentificacionReceptor());
    text(doc, info, "totalSinImpuestos", money(c.getSubtotalSinImpuestos()));
    text(doc, info, "totalDescuento", money(c.getDescuentoTotal()));
    info.appendChild(totalConImpuestos(doc, req.items()));
    text(doc, info, "propina", money(c.getPropina()));
    text(doc, info, "importeTotal", money(c.getValorTotal()));
    text(doc, info, "moneda", c.getMoneda());
    info.appendChild(pagos(doc, req.pagos(), c.getValorTotal()));
    return info;
  }

  private static Element totalConImpuestos(Document doc, List<FacturaItemRequest> items) {
    Element totalConImpuestos = doc.createElement("totalConImpuestos");
    Map<String, ImpuestoTotal> totals =
        items.stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    XmlFacturaGeneratorService::ivaCodigoPorcentaje,
                    item ->
                        new ImpuestoTotal(
                            ivaCodigoPorcentaje(item),
                            nullToZero(item.ivaPorcentaje()),
                            lineSubtotal(item),
                            ivaValor(item)),
                    ImpuestoTotal::merge,
                    java.util.LinkedHashMap::new));
    for (ImpuestoTotal total : totals.values()) {
      Element totalImpuesto = doc.createElement("totalImpuesto");
      text(doc, totalImpuesto, "codigo", "2");
      text(doc, totalImpuesto, "codigoPorcentaje", total.codigoPorcentaje());
      text(doc, totalImpuesto, "baseImponible", money(total.baseImponible()));
      text(doc, totalImpuesto, "valor", money(total.valor()));
      totalConImpuestos.appendChild(totalImpuesto);
    }
    return totalConImpuestos;
  }

  private static Element pagos(Document doc, List<PagoRequest> pagosReq, BigDecimal totalFactura) {
    Element pagos = doc.createElement("pagos");
    List<PagoRequest> normalized =
        pagosReq == null || pagosReq.isEmpty()
            ? List.of(new PagoRequest("20", totalFactura, null, null))
            : pagosReq;
    for (PagoRequest pago : normalized) {
      Element pagoEl = doc.createElement("pago");
      text(doc, pagoEl, "formaPago", pago.formaPago());
      text(doc, pagoEl, "total", money(pago.total() == null ? totalFactura : pago.total()));
      if (pago.plazo() != null) {
        text(doc, pagoEl, "plazo", pago.plazo().toString());
      }
      text(doc, pagoEl, "unidadTiempo", pago.unidadTiempo());
      pagos.appendChild(pagoEl);
    }
    return pagos;
  }

  private static Element detalles(Document doc, List<FacturaItemRequest> items) {
    Element detalles = doc.createElement("detalles");
    for (FacturaItemRequest item : items) {
      Element detalle = doc.createElement("detalle");
      text(doc, detalle, "codigoPrincipal", item.codigoPrincipal());
      text(doc, detalle, "codigoAuxiliar", item.codigoAuxiliar());
      text(doc, detalle, "descripcion", item.descripcion());
      text(doc, detalle, "cantidad", decimal6(item.cantidad()));
      text(doc, detalle, "precioUnitario", decimal6(item.precioUnitario()));
      text(doc, detalle, "descuento", money(nullToZero(item.descuento())));
      text(doc, detalle, "precioTotalSinImpuesto", money(lineSubtotal(item)));
      XmlComprobanteDomUtil.appendDetallesAdicionales(doc, detalle, item);
      detalle.appendChild(impuestosDetalle(doc, item));
      detalles.appendChild(detalle);
    }
    return detalles;
  }

  private static Element impuestosDetalle(Document doc, FacturaItemRequest item) {
    Element impuestos = doc.createElement("impuestos");
    Element impuesto = doc.createElement("impuesto");
    text(doc, impuesto, "codigo", "2");
    text(doc, impuesto, "codigoPorcentaje", ivaCodigoPorcentaje(item));
    text(doc, impuesto, "tarifa", money(nullToZero(item.ivaPorcentaje())));
    text(doc, impuesto, "baseImponible", money(lineSubtotal(item)));
    text(doc, impuesto, "valor", money(ivaValor(item)));
    impuestos.appendChild(impuesto);
    return impuestos;
  }

  private static void appendInfoAdicional(Document doc, Element factura, Map<String, Object> customData) {
    XmlComprobanteDomUtil.appendInfoAdicional(
        doc, factura, customData, XmlComprobanteDomUtil.FACTURA_INFO_ADICIONAL_OMIT);
  }

  private static void text(Document doc, Element parent, String name, String value) {
    if (value == null || value.isBlank()) {
      return;
    }
    Element el = doc.createElement(name);
    el.setTextContent(value);
    parent.appendChild(el);
  }

  private static String serialize(Document doc) throws Exception {
    StringWriter writer = new StringWriter();
    var transformer = TransformerFactory.newInstance().newTransformer();
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
    transformer.transform(new DOMSource(doc), new StreamResult(writer));
    return writer.toString();
  }

  private static BigDecimal lineSubtotal(FacturaItemRequest item) {
    return item.cantidad().multiply(item.precioUnitario()).subtract(nullToZero(item.descuento()));
  }

  private static BigDecimal ivaValor(FacturaItemRequest item) {
    return lineSubtotal(item)
        .multiply(nullToZero(item.ivaPorcentaje()))
        .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
  }

  private static String ivaCodigoPorcentaje(FacturaItemRequest item) {
    if (item.ivaCodigoPorcentaje() != null && !item.ivaCodigoPorcentaje().isBlank()) {
      return item.ivaCodigoPorcentaje();
    }
    BigDecimal pct = nullToZero(item.ivaPorcentaje()).setScale(2, RoundingMode.HALF_UP);
    if (pct.compareTo(new BigDecimal("0.00")) == 0) {
      return "0";
    }
    if (pct.compareTo(new BigDecimal("12.00")) == 0) {
      return "2";
    }
    if (pct.compareTo(new BigDecimal("15.00")) == 0) {
      return "4";
    }
    return "2";
  }

  private static BigDecimal nullToZero(BigDecimal v) {
    return v == null ? BigDecimal.ZERO : v;
  }

  private static String money(BigDecimal v) {
    return nullToZero(v).setScale(2, RoundingMode.HALF_UP).toPlainString();
  }

  private static String decimal6(BigDecimal v) {
    return nullToZero(v).setScale(6, RoundingMode.HALF_UP).toPlainString();
  }

  private record ImpuestoTotal(
      String codigoPorcentaje, BigDecimal tarifa, BigDecimal baseImponible, BigDecimal valor) {
    private ImpuestoTotal merge(ImpuestoTotal other) {
      return new ImpuestoTotal(
          codigoPorcentaje,
          tarifa,
          baseImponible.add(other.baseImponible),
          valor.add(other.valor));
    }
  }
}
