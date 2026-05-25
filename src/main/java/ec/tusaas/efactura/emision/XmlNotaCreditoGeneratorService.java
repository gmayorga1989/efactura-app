package ec.tusaas.efactura.emision;

import ec.tusaas.efactura.dto.emision.FacturaItemRequest;
import ec.tusaas.efactura.dto.emision.NotaCreditoRequest;
import ec.tusaas.efactura.entity.Comprobante;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Service
public class XmlNotaCreditoGeneratorService {

  private static final DateTimeFormatter SRI_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final String COD_DOC_FACTURA = "01";
  private static final Set<String> INFO_ADICIONAL_OMITIR =
      Set.of(
          "puntoEmisionId",
          "tipoIdentificacionReceptor",
          "facturaOrigenId",
          "facturaModificadaId",
          "claveAccesoFactura",
          "numeroFactura",
          "motivo",
          "emailReceptor");

  public String generarXmlInicial(
      Comprobante c, NotaCreditoRequest req, DocumentoModificado docModificado, List<FacturaItemRequest> items) {
    try {
      Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      Element notaCredito = doc.createElement("notaCredito");
      notaCredito.setAttribute("id", "comprobante");
      notaCredito.setAttribute("version", "1.1.0");
      doc.appendChild(notaCredito);

      notaCredito.appendChild(infoTributaria(doc, c));
      notaCredito.appendChild(infoNotaCredito(doc, c, req, docModificado, items));
      notaCredito.appendChild(detalles(doc, items));
      appendInfoAdicional(doc, notaCredito, c.getCustomData());
      return serialize(doc);
    } catch (Exception e) {
      throw new IllegalStateException("No se pudo generar XML de nota de credito", e);
    }
  }

  public record DocumentoModificado(String numDocModificado, LocalDate fechaEmisionDocSustento) {}

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

  private static Element infoNotaCredito(
      Document doc,
      Comprobante c,
      NotaCreditoRequest req,
      DocumentoModificado docModificado,
      List<FacturaItemRequest> items) {
    Element info = doc.createElement("infoNotaCredito");
    text(doc, info, "fechaEmision", c.getFechaEmision().format(SRI_DATE));
    text(doc, info, "dirEstablecimiento", c.getEmpresa().getDireccionMatriz());
    text(doc, info, "tipoIdentificacionComprador", req.tipoIdentificacionReceptor());
    text(doc, info, "razonSocialComprador", c.getRazonSocialReceptor());
    text(doc, info, "identificacionComprador", c.getIdentificacionReceptor());
    text(doc, info, "obligadoContabilidad", c.getEmpresa().isObligadoContabilidad() ? "SI" : "NO");
    text(doc, info, "codDocModificado", COD_DOC_FACTURA);
    text(doc, info, "numDocModificado", docModificado.numDocModificado());
    text(doc, info, "fechaEmisionDocSustento", docModificado.fechaEmisionDocSustento().format(SRI_DATE));
    text(doc, info, "totalSinImpuestos", money(c.getSubtotalSinImpuestos()));
    text(doc, info, "valorModificacion", money(c.getValorTotal()));
    text(doc, info, "moneda", c.getMoneda() == null || c.getMoneda().isBlank() ? "DOLAR" : c.getMoneda());
    info.appendChild(
        XmlComprobanteDomUtil.totalConImpuestos(doc, items, c.getSubtotalSinImpuestos(), c.getIvaTotal()));
    text(doc, info, "motivo", motivoXml(req, c));
    return info;
  }

  private static String motivoXml(NotaCreditoRequest req, Comprobante c) {
    if (req.motivo() != null && !req.motivo().isBlank()) {
      return req.motivo().trim();
    }
    Map<String, Object> cd = c.getCustomData();
    if (cd != null && cd.get("motivo") != null) {
      return String.valueOf(cd.get("motivo")).trim();
    }
    return "Ajuste";
  }

  private static Element totalConImpuestos(Document doc, List<FacturaItemRequest> items) {
    Element totalConImpuestos = doc.createElement("totalConImpuestos");
    Map<String, ImpuestoTotal> totals =
        items.stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    XmlNotaCreditoGeneratorService::ivaCodigoPorcentaje,
                    item ->
                        new ImpuestoTotal(
                            ivaCodigoPorcentaje(item),
                            nullToZero(item.ivaPorcentaje()),
                            lineSubtotal(item),
                            ivaValor(item)),
                    ImpuestoTotal::merge,
                    LinkedHashMap::new));
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

  private static Element detalles(Document doc, List<FacturaItemRequest> items) {
    Element detalles = doc.createElement("detalles");
    for (FacturaItemRequest item : items) {
      Element detalle = doc.createElement("detalle");
      text(doc, detalle, "codigoInterno", item.codigoPrincipal());
      text(doc, detalle, "codigoAdicional", item.codigoAuxiliar());
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

  private static void appendInfoAdicional(Document doc, Element notaCredito, Map<String, Object> customData) {
    if (customData == null || customData.isEmpty()) {
      return;
    }
    Element infoAdicional = doc.createElement("infoAdicional");
    boolean any = false;
    for (Map.Entry<String, Object> entry : customData.entrySet()) {
      if (INFO_ADICIONAL_OMITIR.contains(entry.getKey())) {
        continue;
      }
      Element campo = doc.createElement("campoAdicional");
      campo.setAttribute("nombre", entry.getKey());
      campo.setTextContent(String.valueOf(entry.getValue()));
      infoAdicional.appendChild(campo);
      any = true;
    }
    if (any) {
      notaCredito.appendChild(infoAdicional);
    }
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
