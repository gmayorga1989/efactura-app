package ec.tusaas.efactura.emision.ride;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import ec.tusaas.efactura.dto.ride.RidePlantillaDto;
import ec.tusaas.efactura.entity.Comprobante;
import ec.tusaas.efactura.entity.ComprobanteDetalle;
import java.awt.Color;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Maquetación del cuerpo del RIDE según tipo de comprobante (ND, guía, retención, liquidación). */
public final class RideContenidoPorTipo {

  private static final int MAX_DETALLES = 40;

  private RideContenidoPorTipo() {}

  public static void agregarCuerpoPrincipal(
      Document doc,
      Comprobante comprobante,
      List<ComprobanteDetalle> detalles,
      RidePlantillaDto plantilla,
      boolean layoutSri,
      Font font,
      Font fontBold,
      Font fontMuted,
      Font fontTitle,
      Color accent,
      Color white,
      Color border,
      Color primary,
      Color headerBg,
      float fsTitle)
      throws DocumentException {
    String tipo = RideDocumentoTitulo.normalizarTipo(comprobante.getTipo());
    switch (tipo) {
      case "GUIA_REMISION" -> agregarCuerpoGuia(doc, comprobante, detalles, plantilla, layoutSri, font, fontBold, fontMuted, primary, white, border);
      case "RETENCION" -> agregarCuerpoRetencion(doc, comprobante, plantilla, layoutSri, font, fontBold, fontMuted, primary, white, border, headerBg, fsTitle);
      case "NOTA_DEBITO" -> agregarCuerpoNotaDebito(doc, comprobante, detalles, plantilla, layoutSri, font, fontBold, fontMuted, primary, white, border, headerBg, fsTitle);
      case "LIQUIDACION_COMPRA" -> agregarCuerpoLiquidacion(doc, comprobante, detalles, plantilla, layoutSri, font, fontBold, fontMuted, primary, white, border, headerBg, fsTitle);
      default -> agregarCuerpoVenta(doc, comprobante, detalles, plantilla, layoutSri, font, fontBold, fontMuted, primary, white, border, headerBg, fsTitle);
    }
  }

  private static void agregarCuerpoVenta(
      Document doc,
      Comprobante comprobante,
      List<ComprobanteDetalle> detalles,
      RidePlantillaDto plantilla,
      boolean layoutSri,
      Font font,
      Font fontBold,
      Font fontMuted,
      Color primary,
      Color white,
      Color border,
      Color headerBg,
      float fsTitle)
      throws DocumentException {
    if (layoutSri) {
      RideContenido.agregarBloqueClienteSri(doc, comprobante, font, fontBold, border);
      RideContenido.agregarTablaDetalleSri(doc, detalles, plantilla, font, fontBold, border);
    } else {
      RideContenido.agregarBloqueClienteModerno(doc, comprobante, font, fontBold, primary, white, border);
      RideContenido.agregarTablaDetalleModerno(
          doc, comprobante, detalles, plantilla, font, fontBold, fontMuted, primary, white, border);
    }
    RideContenido.agregarTotales(
        doc, comprobante, plantilla, font, fontBold, fontTitle(fontBold, fsTitle), border, headerBg, primary, fsTitle);
  }

  private static void agregarCuerpoNotaDebito(
      Document doc,
      Comprobante comprobante,
      List<ComprobanteDetalle> detalles,
      RidePlantillaDto plantilla,
      boolean layoutSri,
      Font font,
      Font fontBold,
      Font fontMuted,
      Color primary,
      Color white,
      Color border,
      Color headerBg,
      float fsTitle)
      throws DocumentException {
    if (layoutSri) {
      RideContenido.agregarBloqueClienteSri(doc, comprobante, font, fontBold, border);
    } else {
      RideContenido.agregarBloqueClienteModerno(doc, comprobante, font, fontBold, primary, white, border);
    }
    agregarTablaModificacionNd(doc, detalles, layoutSri, font, fontBold, fontMuted, primary, white, border);
    RideContenido.agregarTotales(
        doc, comprobante, plantilla, font, fontBold, fontTitle(fontBold, fsTitle), border, headerBg, primary, fsTitle);
  }

  private static void agregarCuerpoLiquidacion(
      Document doc,
      Comprobante comprobante,
      List<ComprobanteDetalle> detalles,
      RidePlantillaDto plantilla,
      boolean layoutSri,
      Font font,
      Font fontBold,
      Font fontMuted,
      Color primary,
      Color white,
      Color border,
      Color headerBg,
      float fsTitle)
      throws DocumentException {
    Color headerBgLiq = new Color(236, 253, 245);
    Color headerFgLiq = new Color(6, 95, 70);
    agregarBloqueSeccion(
        doc,
        "INFORMACIÓN DEL PROVEEDOR",
        headerBgLiq,
        headerFgLiq,
        font,
        fontBold,
        List.of(
            fila("Razón social", RidePdfSupport.dash(comprobante.getRazonSocialReceptor()), 2),
            fila("Identificación", RidePdfSupport.dash(comprobante.getIdentificacionReceptor()), 1),
            fila("Fecha emisión", RideFechaFormat.fecha(comprobante.getFechaEmision()), 1),
            fila("Dirección", RideContenido.leerDireccionReceptor(comprobante), 2)));
    if (layoutSri) {
      RideContenido.agregarTablaDetalleSri(doc, detalles, plantilla, font, fontBold, border);
    } else {
      RideContenido.agregarTablaDetalleModerno(
          doc, comprobante, detalles, plantilla, font, fontBold, fontMuted, primary, white, border);
    }
    RideContenido.agregarTotales(
        doc, comprobante, plantilla, font, fontBold, fontTitle(fontBold, fsTitle), border, headerBg, primary, fsTitle);
  }

  private static void agregarCuerpoGuia(
      Document doc,
      Comprobante comprobante,
      List<ComprobanteDetalle> detalles,
      RidePlantillaDto plantilla,
      boolean layoutSri,
      Font font,
      Font fontBold,
      Font fontMuted,
      Color primary,
      Color white,
      Color border)
      throws DocumentException {
    Map<String, Object> cd = comprobante.getCustomData();
    Color headerTrans = new Color(239, 246, 255);
    Color fgTrans = new Color(30, 64, 175);
    agregarBloqueSeccion(
        doc,
        "TRANSPORTISTA",
        headerTrans,
        fgTrans,
        font,
        fontBold,
        List.of(
            fila("Identificación", cdStr(cd, "identificacionTransportista"), 1),
            fila("Razón social", cdStr(cd, "razonSocialTransportista"), 1),
            fila("Placa", cdStr(cd, "placa"), 1),
            fila("Punto de partida", cdStr(cd, "dirPartida"), 1),
            fila("Inicio transporte", RideFechaFormat.fecha(parseFecha(cd.get("fechaIniTransporte"))), 1),
            fila("Fin transporte", RideFechaFormat.fecha(parseFecha(cd.get("fechaFinTransporte"))), 1)));

    Color headerDest = new Color(255, 247, 237);
    Color fgDest = new Color(180, 83, 9);
    List<FilaSeccion> filasDest = new ArrayList<>();
    String numVenta = cdStr(cd, "numeroComprobanteVenta");
    if (!numVenta.isBlank()) {
      filasDest.add(fila("Comprobante de venta", "FACTURA: " + numVenta, 2));
      String fechaVenta = RideFechaFormat.fecha(parseFecha(cd.get("fechaEmisionComprobanteVenta")));
      if (!fechaVenta.isBlank()) {
        filasDest.add(fila("Fecha comprobante venta", fechaVenta, 1));
      }
      String claveVenta = cdStr(cd, "claveAccesoComprobanteVenta");
      if (!claveVenta.isBlank()) {
        filasDest.add(fila("Nº autorización venta", claveVenta, 1));
      }
    }
    filasDest.add(fila("Motivo traslado", cdStr(cd, "motivoTraslado"), 2));
    filasDest.add(fila("Destinatario", RidePdfSupport.dash(comprobante.getRazonSocialReceptor()), 2));
    filasDest.add(
        fila("Identificación destinatario", RidePdfSupport.dash(comprobante.getIdentificacionReceptor()), 1));
    filasDest.add(fila("Destino (llegada)", cdStr(cd, "dirDestinatario"), 1));
    String ruta = cdStr(cd, "ruta");
    if (!ruta.isBlank()) {
      filasDest.add(fila("Ruta", ruta, 2));
    }
    agregarBloqueSeccion(doc, "DESTINO Y TRASLADO", headerDest, fgDest, font, fontBold, filasDest);

    agregarTablaItemsGuia(doc, detalles, layoutSri, font, fontBold, fontMuted, primary, white, border);
  }

  private static void agregarCuerpoRetencion(
      Document doc,
      Comprobante comprobante,
      RidePlantillaDto plantilla,
      boolean layoutSri,
      Font font,
      Font fontBold,
      Font fontMuted,
      Color primary,
      Color white,
      Color border,
      Color headerBg,
      float fsTitle)
      throws DocumentException {
    Map<String, Object> cd = comprobante.getCustomData();
    Color headerSuj = new Color(245, 243, 255);
    Color fgSuj = new Color(91, 33, 182);
    List<FilaSeccion> filas = new ArrayList<>();
    filas.add(fila("Razón social / nombres", RidePdfSupport.dash(comprobante.getRazonSocialReceptor()), 2));
    filas.add(
        fila("Identificación", RidePdfSupport.dash(comprobante.getIdentificacionReceptor()), 1));
    filas.add(fila("Fecha emisión", RideFechaFormat.fecha(comprobante.getFechaEmision()), 1));
    String periodo = cdStr(cd, "periodoFiscal");
    if (!periodo.isBlank()) {
      filas.add(fila("Ejercicio fiscal", periodo, 2));
    }
    agregarBloqueSeccion(doc, "SUJETO RETENIDO", headerSuj, fgSuj, font, fontBold, filas);
    agregarTablaRetenciones(doc, comprobante, layoutSri, font, fontBold, fontMuted, primary, white, border);
    agregarTotalRetenido(doc, comprobante, font, fontBold, border, headerBg, primary, fsTitle);
  }

  private static void agregarTablaModificacionNd(
      Document doc,
      List<ComprobanteDetalle> detalles,
      boolean layoutSri,
      Font font,
      Font fontBold,
      Font fontMuted,
      Color primary,
      Color white,
      Color border)
      throws DocumentException {
    if (detalles == null || detalles.isEmpty()) {
      return;
    }
    PdfPTable grid = new PdfPTable(new float[] {2.2f, 1f});
    grid.setWidthPercentage(100);
    grid.setSpacingAfter(6f);
    grid.setHeaderRows(1);
    Color headerBg = layoutSri ? new Color(245, 245, 245) : primary;
    Color headerFg = layoutSri ? fontBold.getColor() : white;
    RideLayoutShared.addHeader(grid, "Razón de la modificación", headerBg, headerFg == null ? Color.BLACK : headerFg, font.getSize());
    RideLayoutShared.addHeader(grid, "Valor", headerBg, headerFg == null ? Color.BLACK : headerFg, font.getSize());
    int count = 0;
    for (ComprobanteDetalle d : detalles) {
      if (count++ >= MAX_DETALLES) {
        break;
      }
      String razon = RidePdfSupport.dash(d.getDescripcion());
      BigDecimal valor = d.getPrecioTotalSinImpuesto() != null ? d.getPrecioTotalSinImpuesto() : d.getPrecioUnitario();
      grid.addCell(RideLayoutShared.bodyCell(razon, font, Color.WHITE, Element.ALIGN_LEFT, border));
      grid.addCell(RideLayoutShared.bodyCell(RidePdfSupport.money(valor), fontBold, Color.WHITE, Element.ALIGN_RIGHT, border));
    }
    doc.add(grid);
  }

  private static void agregarTablaItemsGuia(
      Document doc,
      List<ComprobanteDetalle> detalles,
      boolean layoutSri,
      Font font,
      Font fontBold,
      Font fontMuted,
      Color primary,
      Color white,
      Color border)
      throws DocumentException {
    if (detalles == null || detalles.isEmpty()) {
      return;
    }
    PdfPTable grid = new PdfPTable(new float[] {0.8f, 2.6f, 1.1f, 1.1f});
    grid.setWidthPercentage(100);
    grid.setSpacingAfter(6f);
    grid.setHeaderRows(1);
    Color headerBg = layoutSri ? new Color(245, 245, 245) : primary;
    Color headerFg = layoutSri ? Color.BLACK : white;
    RideLayoutShared.addHeader(grid, "Cant.", headerBg, headerFg, font.getSize());
    RideLayoutShared.addHeader(grid, "Descripción", headerBg, headerFg, font.getSize());
    RideLayoutShared.addHeader(grid, "Cód. principal", headerBg, headerFg, font.getSize());
    RideLayoutShared.addHeader(grid, "Cód. auxiliar", headerBg, headerFg, font.getSize());
    int count = 0;
    for (ComprobanteDetalle d : detalles) {
      if (count++ >= MAX_DETALLES) {
        break;
      }
      grid.addCell(
          RideLayoutShared.bodyCell(
              RidePdfSupport.qty(d.getCantidad()), font, Color.WHITE, Element.ALIGN_RIGHT, border));
      grid.addCell(
          RideLayoutShared.bodyCell(
              RidePdfSupport.dash(d.getDescripcion()), font, Color.WHITE, Element.ALIGN_LEFT, border));
      grid.addCell(
          RideLayoutShared.bodyCell(
              RidePdfSupport.dash(d.getCodigoPrincipal()), font, Color.WHITE, Element.ALIGN_LEFT, border));
      grid.addCell(
          RideLayoutShared.bodyCell(
              RidePdfSupport.dash(d.getCodigoAuxiliar()), font, Color.WHITE, Element.ALIGN_LEFT, border));
    }
    doc.add(grid);
  }

  @SuppressWarnings("unchecked")
  private static void agregarTablaRetenciones(
      Document doc,
      Comprobante comprobante,
      boolean layoutSri,
      Font font,
      Font fontBold,
      Font fontMuted,
      Color primary,
      Color white,
      Color border)
      throws DocumentException {
    Map<String, Object> cd = comprobante.getCustomData();
    if (cd == null) {
      return;
    }
    Object raw = cd.get("impuestos");
    if (!(raw instanceof List<?> list) || list.isEmpty()) {
      return;
    }
    PdfPTable grid =
        new PdfPTable(new float[] {0.9f, 1.2f, 0.9f, 0.7f, 0.7f, 0.7f, 0.8f, 0.9f});
    grid.setWidthPercentage(100);
    grid.setSpacingAfter(6f);
    grid.setHeaderRows(1);
    Color headerBg = layoutSri ? new Color(245, 245, 245) : primary;
    Color headerFg = layoutSri ? Color.BLACK : white;
    String[] headers = {
      "Comprobante", "Número", "Fecha emisión", "Ejercicio fiscal", "Base imponible", "Impuesto", "% Ret.", "Valor retenido"
    };
    for (String h : headers) {
      RideLayoutShared.addHeader(grid, h, headerBg, headerFg, font.getSize() - 1f);
    }
    String periodo = cdStr(cd, "periodoFiscal");
    int count = 0;
    for (Object item : list) {
      if (!(item instanceof Map<?, ?> map) || count++ >= MAX_DETALLES) {
        continue;
      }
      String codDoc = texto(map.get("codDocSustento"));
      if (codDoc.isBlank()) {
        codDoc = "01";
      }
      String tipoDoc = "01".equals(codDoc) ? "FACTURA" : codDoc;
      grid.addCell(RideLayoutShared.bodyCell(tipoDoc, font, Color.WHITE, Element.ALIGN_LEFT, border));
      grid.addCell(RideLayoutShared.bodyCell(texto(map.get("numDocSustento")), font, Color.WHITE, Element.ALIGN_LEFT, border));
      grid.addCell(
          RideLayoutShared.bodyCell(
              RideFechaFormat.fecha(parseFecha(map.get("fechaEmisionDocSustento"))),
              font,
              Color.WHITE,
              Element.ALIGN_LEFT,
              border));
      grid.addCell(RideLayoutShared.bodyCell(periodo, font, Color.WHITE, Element.ALIGN_CENTER, border));
      grid.addCell(
          RideLayoutShared.bodyCell(
              RidePdfSupport.money(toBigDecimal(map.get("baseImponible"))),
              font,
              Color.WHITE,
              Element.ALIGN_RIGHT,
              border));
      String imp = etiquetaImpuesto(texto(map.get("codigo")));
      grid.addCell(RideLayoutShared.bodyCell(imp, font, Color.WHITE, Element.ALIGN_CENTER, border));
      grid.addCell(
          RideLayoutShared.bodyCell(
              RidePdfSupport.qty(toBigDecimal(map.get("porcentajeRetener"))),
              font,
              Color.WHITE,
              Element.ALIGN_RIGHT,
              border));
      grid.addCell(
          RideLayoutShared.bodyCell(
              RidePdfSupport.money(toBigDecimal(map.get("valorRetenido"))),
              fontBold,
              Color.WHITE,
              Element.ALIGN_RIGHT,
              border));
    }
    doc.add(grid);
  }

  private static void agregarTotalRetenido(
      Document doc,
      Comprobante comprobante,
      Font font,
      Font fontBold,
      Color border,
      Color headerBg,
      Color primary,
      float fsTitle)
      throws DocumentException {
    PdfPTable totals = new PdfPTable(new float[] {1.6f, 1f});
    totals.setWidthPercentage(100);
  totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
    PdfPCell spacer = new PdfPCell(new com.lowagie.text.Phrase(""));
    spacer.setBorder(Rectangle.NO_BORDER);
    totals.addCell(spacer);
    PdfPTable inner = new PdfPTable(2);
    inner.setWidthPercentage(100);
    RideLayoutShared.addTotalRow(
        inner, "VALOR TOTAL RETENIDO", RidePdfSupport.money(comprobante.getValorTotal()), font, fontBold, border);
    PdfPCell innerWrap = new PdfPCell(inner);
    innerWrap.setBorder(Rectangle.NO_BORDER);
    totals.addCell(innerWrap);
    doc.add(totals);
  }

  private record FilaSeccion(String etiqueta, String valor, int colspan) {}

  private static FilaSeccion fila(String etiqueta, String valor, int colspan) {
    return new FilaSeccion(etiqueta, valor, colspan);
  }

  private static void agregarBloqueSeccion(
      Document doc,
      String titulo,
      Color headerBg,
      Color headerFg,
      Font font,
      Font fontBold,
      List<FilaSeccion> filas)
      throws DocumentException {
    PdfPTable block = new PdfPTable(new float[] {1f, 1f});
    block.setWidthPercentage(100);
    block.setSpacingAfter(6f);
    Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, font.getSize(), headerFg);
    PdfPCell head = RideLayoutShared.cellText(titulo, headFont, Element.ALIGN_LEFT, Rectangle.NO_BORDER);
    head.setBackgroundColor(headerBg);
    head.setColspan(2);
    head.setPaddingTop(2.5f);
    head.setPaddingBottom(2f);
    head.setPaddingLeft(4f);
    head.setPaddingRight(4f);
    block.addCell(head);
    int col = 0;
    for (FilaSeccion f : filas) {
      if (f.valor() == null || f.valor().isBlank()) {
        continue;
      }
      if (f.colspan() >= 2) {
        if (col == 1) {
          block.addCell(celdaVacia());
          col = 0;
        }
        PdfPCell cell = RideLayoutShared.cellLabelValuePlana(f.etiqueta(), f.valor(), font, fontBold);
        cell.setColspan(2);
        block.addCell(cell);
        col = 0;
        continue;
      }
      block.addCell(RideLayoutShared.cellLabelValuePlana(f.etiqueta(), f.valor(), font, fontBold));
      col = (col + 1) % 2;
    }
    if (col == 1) {
      block.addCell(celdaVacia());
    }
    doc.add(block);
  }

  private static PdfPCell celdaVacia() {
    PdfPCell c = new PdfPCell(new com.lowagie.text.Phrase(""));
    c.setBorder(Rectangle.NO_BORDER);
    return c;
  }

  private static String cdStr(Map<String, Object> cd, String key) {
    if (cd == null) {
      return "";
    }
    Object v = cd.get(key);
    return v == null ? "" : String.valueOf(v).trim();
  }

  private static String texto(Object raw) {
    return raw == null ? "" : String.valueOf(raw).trim();
  }

  private static String etiquetaImpuesto(String codigo) {
    if ("1".equals(codigo)) {
      return "RENTA";
    }
    if ("2".equals(codigo)) {
      return "IVA";
    }
    if ("6".equals(codigo)) {
      return "ISD";
    }
    return codigo.isBlank() ? "-" : codigo;
  }

  private static BigDecimal toBigDecimal(Object v) {
    if (v == null) {
      return BigDecimal.ZERO;
    }
    if (v instanceof BigDecimal bd) {
      return bd;
    }
    try {
      return new BigDecimal(String.valueOf(v));
    } catch (Exception e) {
      return BigDecimal.ZERO;
    }
  }

  private static java.time.LocalDate parseFecha(Object raw) {
    if (raw == null) {
      return null;
    }
    String s = String.valueOf(raw).trim();
    if (s.isBlank()) {
      return null;
    }
    try {
      if (s.length() >= 10 && s.charAt(4) == '-') {
        return java.time.LocalDate.parse(s.substring(0, 10));
      }
      if (s.length() >= 10 && s.charAt(2) == '/') {
        String[] p = s.substring(0, 10).split("/");
        return java.time.LocalDate.of(Integer.parseInt(p[2]), Integer.parseInt(p[1]), Integer.parseInt(p[0]));
      }
    } catch (Exception ignored) {
      return null;
    }
    return null;
  }

  private static Font fontTitle(Font fontBold, float fsTitle) {
    return FontFactory.getFont(FontFactory.HELVETICA_BOLD, fsTitle, fontBold.getColor());
  }
}
