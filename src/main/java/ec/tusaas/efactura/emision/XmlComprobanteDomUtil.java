package ec.tusaas.efactura.emision;

import ec.tusaas.efactura.dto.emision.FacturaItemRequest;
import ec.tusaas.efactura.entity.Comprobante;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Utilidades DOM compartidas para generadores XML SRI. */
public final class XmlComprobanteDomUtil {

  public static final DateTimeFormatter SRI_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  public static final String COD_DOC_FACTURA = "01";

  private XmlComprobanteDomUtil() {}

  public static Element infoTributaria(Document doc, Comprobante c) {
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

  public static Element detallesConImpuestos(Document doc, List<FacturaItemRequest> items, boolean notaCreditoDebito) {
    Element detalles = doc.createElement("detalles");
    for (FacturaItemRequest item : items) {
      Element detalle = doc.createElement("detalle");
      if (notaCreditoDebito) {
        text(doc, detalle, "codigoInterno", item.codigoPrincipal());
        text(doc, detalle, "codigoAdicional", item.codigoAuxiliar());
      } else {
        text(doc, detalle, "codigoPrincipal", item.codigoPrincipal());
        text(doc, detalle, "codigoAuxiliar", item.codigoAuxiliar());
      }
      text(doc, detalle, "descripcion", item.descripcion());
      text(doc, detalle, "cantidad", decimal6(item.cantidad()));
      text(doc, detalle, "precioUnitario", decimal6(item.precioUnitario()));
      text(doc, detalle, "descuento", money(nullToZero(item.descuento())));
      text(doc, detalle, "precioTotalSinImpuesto", money(lineSubtotal(item)));
      appendDetallesAdicionales(doc, detalle, item);
      detalle.appendChild(impuestosDetalle(doc, item));
      detalles.appendChild(detalle);
    }
    return detalles;
  }

  public static Element totalConImpuestos(Document doc, List<FacturaItemRequest> items) {
    return totalConImpuestos(doc, items, null, null);
  }

  /** {@code totalConImpuestos} con al menos un {@code totalImpuesto} (nota de credito y similares). */
  public static Element totalConImpuestos(
      Document doc,
      List<FacturaItemRequest> items,
      BigDecimal subtotalFallback,
      BigDecimal ivaFallback) {
    Element totalConImpuestos = doc.createElement("totalConImpuestos");
    Map<String, ImpuestoTotal> totals = agruparImpuestosDesdeItems(items);
    if (totals.isEmpty()) {
      totals.put("fallback", impuestoTotalDesdeTotales(subtotalFallback, ivaFallback));
    }
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

  /** Bloque {@code impuestos} de infoNotaDebito (SRI v1.0.0), con tarifa en cada {@code impuesto}. */
  public static Element impuestosInfo(Document doc, List<FacturaItemRequest> items) {
    return impuestosInfo(doc, items, null, null);
  }

  public static Element impuestosInfo(
      Document doc,
      List<FacturaItemRequest> items,
      BigDecimal subtotalFallback,
      BigDecimal ivaFallback) {
    Element impuestos = doc.createElement("impuestos");
    Map<String, ImpuestoInfo> totals = agruparImpuestosInfoDesdeItems(items);
    if (totals.isEmpty()) {
      ImpuestoTotal fallback = impuestoTotalDesdeTotales(subtotalFallback, ivaFallback);
      totals.put(
          "fallback",
          new ImpuestoInfo(
              fallback.codigoPorcentaje(),
              tarifaDesdeCodigo(fallback.codigoPorcentaje(), fallback.baseImponible(), fallback.valor()),
              fallback.baseImponible(),
              fallback.valor()));
    }
    for (ImpuestoInfo total : totals.values()) {
      appendImpuestoInfo(doc, impuestos, total);
    }
    return impuestos;
  }

  private static Map<String, ImpuestoTotal> agruparImpuestosDesdeItems(List<FacturaItemRequest> items) {
    if (items == null || items.isEmpty()) {
      return new LinkedHashMap<>();
    }
    return items.stream()
        .collect(
            java.util.stream.Collectors.toMap(
                XmlComprobanteDomUtil::ivaCodigoPorcentaje,
                item ->
                    new ImpuestoTotal(
                        ivaCodigoPorcentaje(item), lineSubtotal(item), ivaValor(item)),
                ImpuestoTotal::merge,
                LinkedHashMap::new));
  }

  private static Map<String, ImpuestoInfo> agruparImpuestosInfoDesdeItems(List<FacturaItemRequest> items) {
    if (items == null || items.isEmpty()) {
      return new LinkedHashMap<>();
    }
    return items.stream()
        .collect(
            java.util.stream.Collectors.toMap(
                XmlComprobanteDomUtil::ivaCodigoPorcentaje,
                item ->
                    new ImpuestoInfo(
                        ivaCodigoPorcentaje(item),
                        nullToZero(item.ivaPorcentaje()),
                        lineSubtotal(item),
                        ivaValor(item)),
                ImpuestoInfo::merge,
                LinkedHashMap::new));
  }

  private static ImpuestoTotal impuestoTotalDesdeTotales(BigDecimal subtotalFallback, BigDecimal ivaFallback) {
    BigDecimal base = nullToZero(subtotalFallback);
    BigDecimal iva = nullToZero(ivaFallback);
    BigDecimal pct = BigDecimal.ZERO;
    if (base.signum() > 0 && iva.signum() > 0) {
      pct = iva.multiply(BigDecimal.valueOf(100)).divide(base, 2, RoundingMode.HALF_UP);
    }
    return new ImpuestoTotal(ivaCodigoPorcentajeDesdePct(pct), base, iva);
  }

  private static BigDecimal tarifaDesdeCodigo(
      String codigoPorcentaje, BigDecimal base, BigDecimal iva) {
    if (base.signum() > 0 && iva.signum() > 0) {
      return iva.multiply(BigDecimal.valueOf(100)).divide(base, 2, RoundingMode.HALF_UP);
    }
    return switch (codigoPorcentaje) {
      case "4" -> new BigDecimal("15.00");
      case "2" -> new BigDecimal("12.00");
      case "0" -> BigDecimal.ZERO;
      default -> BigDecimal.ZERO;
    };
  }

  private static void appendImpuestoInfo(Document doc, Element impuestos, ImpuestoInfo total) {
    Element impuesto = doc.createElement("impuesto");
    text(doc, impuesto, "codigo", "2");
    text(doc, impuesto, "codigoPorcentaje", total.codigoPorcentaje());
    text(doc, impuesto, "tarifa", money(total.tarifa()));
    text(doc, impuesto, "baseImponible", money(total.baseImponible()));
    text(doc, impuesto, "valor", money(total.valor()));
    impuestos.appendChild(impuesto);
  }

  public static String ivaCodigoPorcentajeDesdePct(BigDecimal pct) {
    BigDecimal p = nullToZero(pct).setScale(2, RoundingMode.HALF_UP);
    if (p.compareTo(BigDecimal.ZERO) == 0) {
      return "0";
    }
    if (p.compareTo(new BigDecimal("12.00")) == 0) {
      return "2";
    }
    if (p.compareTo(new BigDecimal("15.00")) == 0) {
      return "4";
    }
    return "2";
  }

  public static final Set<String> FACTURA_INFO_ADICIONAL_OMIT =
      Set.of(
          "puntoEmisionId",
          "tipoIdentificacionReceptor",
          "emailReceptor",
          "emailsReceptor",
          "desgloseImpuestos",
          "desgloseSubtotales",
          "pagos",
          "detallesAdicionales",
          "detallesAdicionalesHtml");

  public static void appendInfoAdicional(
      Document doc, Element root, Map<String, Object> customData, Set<String> omitir) {
    if (customData == null || customData.isEmpty()) {
      return;
    }
    Element infoAdicional = doc.createElement("infoAdicional");
    boolean any = false;
    Object glosa = customData.get("glosa");
    if (glosa != null && !String.valueOf(glosa).isBlank()) {
      Element campo = doc.createElement("campoAdicional");
      campo.setAttribute("nombre", "Glosa");
      campo.setTextContent(detalleAdicionalTextoXml(String.valueOf(glosa)));
      infoAdicional.appendChild(campo);
      any = true;
    }
    for (Map.Entry<String, Object> entry : customData.entrySet()) {
      if (omitir != null && omitir.contains(entry.getKey())) {
        continue;
      }
      if ("glosa".equals(entry.getKey())) {
        continue;
      }
      if (entry.getValue() == null || String.valueOf(entry.getValue()).isBlank()) {
        continue;
      }
      Element campo = doc.createElement("campoAdicional");
      campo.setAttribute("nombre", entry.getKey());
      campo.setTextContent(String.valueOf(entry.getValue()));
      infoAdicional.appendChild(campo);
      any = true;
    }
    if (any) {
      root.appendChild(infoAdicional);
    }
  }

  public static void appendDetallesAdicionales(Document doc, Element detalle, FacturaItemRequest item) {
    List<String> extras = leerDetallesAdicionales(item);
    if (extras.isEmpty()) {
      return;
    }
    Element detallesAdicionales = doc.createElement("detallesAdicionales");
    int idx = 1;
    for (String valor : extras) {
      if (valor == null || valor.isBlank()) {
        continue;
      }
      Element detAdicional = doc.createElement("detAdicional");
      detAdicional.setAttribute("nombre", "Detalle Adicional " + idx);
      detAdicional.setAttribute("valor", detalleAdicionalTextoXml(valor));
      detallesAdicionales.appendChild(detAdicional);
      idx++;
    }
    if (detallesAdicionales.hasChildNodes()) {
      detalle.appendChild(detallesAdicionales);
    }
  }

  @SuppressWarnings("unchecked")
  private static List<String> leerDetallesAdicionales(FacturaItemRequest item) {
    Object raw = item.safeCustomData().get("detallesAdicionales");
    if (!(raw instanceof List<?> list)) {
      return List.of();
    }
    return list.stream()
        .map(v -> v == null ? "" : detalleAdicionalTextoXml(String.valueOf(v)))
        .filter(s -> !s.isBlank())
        .limit(3)
        .toList();
  }

  /** Texto plano para XML: sin saltos de línea y espacios colapsados (máx. 300). */
  public static String detalleAdicionalTextoXml(String raw) {
    if (raw == null) {
      return "";
    }
    String t = htmlToPlainText(raw);
    t = t.replaceAll("[\\r\\n\\t]+", " ").replaceAll("\\s+", " ").trim();
    if (t.length() > 300) {
      return t.substring(0, 300);
    }
    return t;
  }

  private static String htmlToPlainText(String raw) {
    if (raw == null || raw.isBlank()) {
      return "";
    }
    if (!raw.contains("<")) {
      return raw;
    }
    return raw
        .replaceAll("(?i)<br\\s*/?>", " ")
        .replaceAll("(?i)</p>", " ")
        .replaceAll("(?i)</div>", " ")
        .replaceAll("(?i)</li>", " ")
        .replaceAll("<[^>]+>", "")
        .trim();
  }

  public static String serialize(Document doc) throws Exception {
    StringWriter writer = new StringWriter();
    var transformer = TransformerFactory.newInstance().newTransformer();
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
    transformer.transform(new DOMSource(doc), new StreamResult(writer));
    return writer.toString();
  }

  public static void text(Document doc, Element parent, String name, String value) {
    if (value == null || value.isBlank()) {
      return;
    }
    Element el = doc.createElement(name);
    el.setTextContent(value);
    parent.appendChild(el);
  }

  public static BigDecimal lineSubtotal(FacturaItemRequest item) {
    return item.cantidad().multiply(item.precioUnitario()).subtract(nullToZero(item.descuento()));
  }

  public static BigDecimal ivaValor(FacturaItemRequest item) {
    return lineSubtotal(item)
        .multiply(nullToZero(item.ivaPorcentaje()))
        .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
  }

  public static String ivaCodigoPorcentaje(FacturaItemRequest item) {
    if (item.ivaCodigoPorcentaje() != null && !item.ivaCodigoPorcentaje().isBlank()) {
      return item.ivaCodigoPorcentaje();
    }
    return ivaCodigoPorcentajeDesdePct(item.ivaPorcentaje());
  }

  public static BigDecimal nullToZero(BigDecimal v) {
    return v == null ? BigDecimal.ZERO : v;
  }

  public static String money(BigDecimal v) {
    return nullToZero(v).setScale(2, RoundingMode.HALF_UP).toPlainString();
  }

  public static String decimal6(BigDecimal v) {
    return nullToZero(v).setScale(6, RoundingMode.HALF_UP).toPlainString();
  }

  public static String moneda(Comprobante c) {
    return c.getMoneda() == null || c.getMoneda().isBlank() ? "DOLAR" : c.getMoneda();
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

  private record ImpuestoTotal(String codigoPorcentaje, BigDecimal baseImponible, BigDecimal valor) {
    private ImpuestoTotal merge(ImpuestoTotal other) {
      return new ImpuestoTotal(
          codigoPorcentaje, baseImponible.add(other.baseImponible), valor.add(other.valor));
    }
  }

  private record ImpuestoInfo(
      String codigoPorcentaje, BigDecimal tarifa, BigDecimal baseImponible, BigDecimal valor) {
    private ImpuestoInfo merge(ImpuestoInfo other) {
      return new ImpuestoInfo(
          codigoPorcentaje, tarifa, baseImponible.add(other.baseImponible), valor.add(other.valor));
    }
  }
}
