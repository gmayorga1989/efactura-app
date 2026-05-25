package ec.tusaas.efactura.emision.ride;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import ec.tusaas.efactura.dto.ride.RidePlantillaDto;
import ec.tusaas.efactura.emision.DocumentoModificadoRideUtil;
import ec.tusaas.efactura.emision.DocumentoModificadoRideUtil.DatosDocumentoModificado;
import ec.tusaas.efactura.entity.Comprobante;
import ec.tusaas.efactura.entity.ComprobanteDetalle;
import ec.tusaas.efactura.entity.Empresa;
import java.time.LocalDate;
import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bloques de contenido tributario/comercial del RIDE (cliente, líneas, totales, pagos). */
public final class RideContenido {

  private static final int MAX_DETALLES = 40;

  private static final Map<String, String> FORMA_PAGO =
      Map.ofEntries(
          Map.entry("01", "Sin utilización del sistema financiero"),
          Map.entry("15", "Compensación de deudas"),
          Map.entry("16", "Tarjeta de débito"),
          Map.entry("17", "Dinero electrónico"),
          Map.entry("18", "Tarjeta prepago"),
          Map.entry("19", "Tarjeta de crédito"),
          Map.entry("20", "Otros con utilización del sistema financiero"),
          Map.entry("21", "Endoso de títulos"));

  private RideContenido() {}

  public static boolean adicionalesEnColumna(RidePlantillaDto plantilla) {
    return "columna".equals(modoAdicional(plantilla));
  }

  public static String modoAdicional(RidePlantillaDto plantilla) {
    if (plantilla == null || plantilla.detalleAdicionalModo() == null) {
      return "en_descripcion";
    }
    String m = plantilla.detalleAdicionalModo().toLowerCase();
    return "columna".equals(m) || "columna_separada".equals(m) ? "columna" : "en_descripcion";
  }

  public static boolean mostrarDesgloseIva(RidePlantillaDto plantilla) {
    return plantilla == null || plantilla.mostrarDesgloseIva();
  }

  public static boolean tieneDescuentoEnLineas(List<ComprobanteDetalle> detalles) {
    if (detalles == null) {
      return false;
    }
    for (ComprobanteDetalle d : detalles) {
      if (d.getDescuento() != null && d.getDescuento().signum() > 0) {
        return true;
      }
    }
    return false;
  }

  public static String leerGlosa(Comprobante comprobante) {
    Map<String, Object> cd = comprobante.getCustomData();
    if (cd == null) {
      return "";
    }
    Object glosa = cd.get("glosa");
    return glosa == null ? "" : String.valueOf(glosa).trim();
  }

  public static String leerDireccionReceptor(Comprobante comprobante) {
    Map<String, Object> cd = comprobante.getCustomData();
    if (cd != null) {
      Object dir = cd.get("direccionReceptor");
      if (dir == null) {
        dir = cd.get("direccionComprador");
      }
      if (dir != null && !String.valueOf(dir).isBlank()) {
        return String.valueOf(dir).trim();
      }
    }
    if (comprobante.getCliente() != null && comprobante.getCliente().getDireccion() != null) {
      return comprobante.getCliente().getDireccion().trim();
    }
    return "";
  }

  public static String etiquetaFormaPago(String codigo) {
    if (codigo == null || codigo.isBlank()) {
      return "-";
    }
    String c = codigo.trim();
    String label = FORMA_PAGO.get(c);
    return label == null ? c : c + " — " + label;
  }

  public static String textoFormasPago(Comprobante comprobante) {
    Map<String, Object> cd = comprobante.getCustomData();
    if (cd == null) {
      return "";
    }
    Object raw = cd.get("pagos");
    if (!(raw instanceof List<?> list) || list.isEmpty()) {
      return "";
    }
    List<String> partes = new ArrayList<>();
    for (Object item : list) {
      if (item instanceof Map<?, ?> map) {
        String fp = map.get("formaPago") == null ? "" : String.valueOf(map.get("formaPago")).trim();
        Object total = map.get("total");
        String monto = total == null ? "" : RidePdfSupport.money(toBigDecimal(total));
        if (!fp.isBlank()) {
          partes.add(etiquetaFormaPago(fp) + (monto.isBlank() ? "" : " (" + monto + ")"));
        }
      }
    }
    return String.join("; ", partes);
  }

  public static void agregarDatosEmisorExtendidos(
      PdfPTable emisorInner, Empresa empresa, Font font) {
    if (empresa.isExportadorHabitual()) {
      emisorInner.addCell(RideLayoutShared.cellTextPlana("Exportador habitual: SI", font, Element.ALIGN_LEFT));
    }
    if (empresa.isAgenteRetencion()) {
      emisorInner.addCell(RideLayoutShared.cellTextPlana("Agente de retención: SI", font, Element.ALIGN_LEFT));
    }
    if (empresa.isCalificacionArtesanal()) {
      String art =
          empresa.getCodigoArtesano() != null && !empresa.getCodigoArtesano().isBlank()
              ? "Calificación artesanal: SI (Cód. " + empresa.getCodigoArtesano() + ")"
              : "Calificación artesanal: SI";
      emisorInner.addCell(RideLayoutShared.cellTextPlana(art, font, Element.ALIGN_LEFT));
    }
  }

  /** Color de texto en la caja de factura (clave, autorización). */
  public static Color textoCajaFactura(RidePlantillaDto plantilla, boolean ejecutivo, Color text, Color white) {
    if (plantilla != null
        && plantilla.colorTextoCajaFactura() != null
        && !plantilla.colorTextoCajaFactura().isBlank()
        && plantilla.colorTextoCajaFactura().matches("^#[0-9A-Fa-f]{6}$")) {
      return RidePdfSupport.color(plantilla.colorTextoCajaFactura(), ejecutivo ? "#1f2937" : "#ffffff");
    }
    return ejecutivo ? text : white;
  }

  /** Filas Nº autorización y fecha autorización (junto a clave de acceso). */
  public static void agregarFilasAutorizacion(
      PdfPTable destino,
      Comprobante comprobante,
      Font font,
      Font fontBold,
      Color border,
      int borderStyle,
      Color texto) {
    Font labelFont = FontFactory.getFont(FontFactory.HELVETICA, font.getSize(), texto);
    Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, fontBold.getSize(), texto);
    boolean plano = borderStyle == Rectangle.NO_BORDER;
    PdfPCell numAuth =
        plano
            ? RideLayoutShared.cellLabelValuePlana(
                "Nº autorización", RidePdfSupport.dash(comprobante.getNumeroAutorizacion()), labelFont, valueFont)
            : RideLayoutShared.cellLabelValue(
                "Nº autorización",
                RidePdfSupport.dash(comprobante.getNumeroAutorizacion()),
                labelFont,
                valueFont,
                border);
    if (!plano) {
      numAuth.setBorder(borderStyle);
    }
    destino.addCell(numAuth);
    PdfPCell fechaAuth =
        plano
            ? RideLayoutShared.cellLabelValuePlana(
                "Fecha autorización",
                RideFechaFormat.fechaHora(comprobante.getFechaAutorizacion()),
                labelFont,
                valueFont)
            : RideLayoutShared.cellLabelValue(
                "Fecha autorización",
                RideFechaFormat.fechaHora(comprobante.getFechaAutorizacion()),
                labelFont,
                valueFont,
                border);
    if (!plano) {
      fechaAuth.setBorder(borderStyle);
    }
    destino.addCell(fechaAuth);
  }

  /** Clave de acceso, código de barras y autorización debajo del encabezado (clave fuera de caja). */
  public static void agregarBloqueClaveAccesoExterno(
      Document doc,
      Comprobante comprobante,
      PdfWriter writer,
      RidePlantillaDto plantilla,
      Font font,
      Font fontBold,
      Color border)
      throws DocumentException {
    PdfPTable claveTable = new PdfPTable(1);
    claveTable.setWidthPercentage(100);
    claveTable.setSpacingAfter(6f);
    PdfPCell claveCell =
        RideLayoutShared.cellText(
            "CLAVE DE ACCESO\n" + comprobante.getClaveAcceso(),
            fontBold,
            Element.ALIGN_CENTER,
            Rectangle.BOX);
    claveCell.setPadding(8f);
    claveCell.setBorderColor(border);
    claveTable.addCell(claveCell);
    doc.add(claveTable);

    boolean conBarcode = plantilla != null && plantilla.mostrarCodigoBarras();
    if (conBarcode) {
      Image barcode =
          RidePdfSupport.barcodeClave(
              comprobante.getClaveAcceso(),
              writer.getDirectContent(),
              RidePdfSupport.BARCODE_MAX_ANCHO_COMPLETO);
      PdfPTable bcRow = new PdfPTable(1);
      bcRow.setWidthPercentage(92);
      bcRow.setHorizontalAlignment(Element.ALIGN_CENTER);
      bcRow.setSpacingAfter(6f);
      PdfPCell bcCell = RideLayoutShared.celdaCodigoBarras(barcode, 6f);
      if (bcCell != null) {
        bcRow.addCell(bcCell);
        doc.add(bcRow);
      }
    }

    PdfPTable auth = new PdfPTable(new float[] {1f, 1f});
    auth.setWidthPercentage(100);
    auth.setSpacingAfter(8f);
    agregarFilasAutorizacion(auth, comprobante, font, fontBold, border, Rectangle.BOX, RidePdfSupport.color("#1f2937", "#1f2937"));
    doc.add(auth);
  }

  public static void agregarBloqueClienteModerno(
      Document doc,
      Comprobante comprobante,
      Font font,
      Font fontBold,
      Color accent,
      Color white,
      Color border)
      throws DocumentException {
    agregarBloqueClienteCompacto(doc, comprobante, font, fontBold, accent, white);
  }

  public static void agregarBloqueClienteSri(
      Document doc,
      Comprobante comprobante,
      Font font,
      Font fontBold,
      Color border)
      throws DocumentException {
    Color headerBg = new Color(241, 245, 249);
    Color headerFg = fontBold.getColor() == null ? Color.BLACK : fontBold.getColor();
    agregarBloqueClienteCompacto(doc, comprobante, font, fontBold, headerBg, headerFg);
  }

  /**
   * Bloque cliente sin rejilla interna: razón social, identificación y fecha en la misma fila,
   * dirección debajo; cabecera compacta.
   */
  private static void agregarBloqueClienteCompacto(
      Document doc,
      Comprobante comprobante,
      Font font,
      Font fontBold,
      Color headerBg,
      Color headerFg)
      throws DocumentException {
    PdfPTable cliente = new PdfPTable(new float[] {1f, 1f});
    cliente.setWidthPercentage(100);
    cliente.setSpacingAfter(6f);

    Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, font.getSize(), headerFg);
    PdfPCell clienteHead =
        RideLayoutShared.cellText("INFORMACIÓN DEL CLIENTE", headFont, Element.ALIGN_LEFT, Rectangle.NO_BORDER);
    clienteHead.setBackgroundColor(headerBg);
    clienteHead.setColspan(2);
    clienteHead.setPaddingTop(2.5f);
    clienteHead.setPaddingBottom(2f);
    clienteHead.setPaddingLeft(4f);
    clienteHead.setPaddingRight(4f);
    cliente.addCell(clienteHead);

    PdfPCell razonCell =
        RideLayoutShared.cellLabelValuePlana(
            "Razón social", RidePdfSupport.dash(comprobante.getRazonSocialReceptor()), font, fontBold);
    razonCell.setColspan(2);
    cliente.addCell(razonCell);

    cliente.addCell(
        RideLayoutShared.cellLabelValuePlana(
            "Identificación", RidePdfSupport.dash(comprobante.getIdentificacionReceptor()), font, fontBold));
    cliente.addCell(
        RideLayoutShared.cellLabelValuePlana(
            "Fecha emisión", RideFechaFormat.fecha(comprobante.getFechaEmision()), font, fontBold));

    PdfPCell dirCell =
        RideLayoutShared.cellLabelValuePlana("Dirección", leerDireccionReceptor(comprobante), font, fontBold);
    dirCell.setColspan(2);
    cliente.addCell(dirCell);

    doc.add(cliente);
    if (DocumentoModificadoRideUtil.esNotaCreditoODebito(comprobante)) {
      agregarBloqueDocumentoModificado(doc, comprobante, font, fontBold);
    } else {
      agregarMotivoSiAplica(doc, comprobante, font, fontBold);
    }
  }

  /** Bloque separado y destacado para NC/ND (no mezclado con datos del cliente). */
  private static void agregarBloqueDocumentoModificado(
      Document doc, Comprobante comprobante, Font font, Font fontBold) throws DocumentException {
    DatosDocumentoModificado datos = DocumentoModificadoRideUtil.leerParaRide(comprobante);
    if (datos == null) {
      return;
    }
    LocalDate fechaMod = DocumentoModificadoRideUtil.parseFechaModificada(datos.fechaEmisionModificado());
    String fechaModStr = RideFechaFormat.fecha(fechaMod);
    String tipoMod = etiquetaTipoComprobanteModificado(datos.tipoComprobanteModificado());

    Color headerBg = new Color(255, 247, 237);
    Color headerFg = new Color(180, 83, 9);
    Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, font.getSize(), headerFg);

    PdfPTable mod = new PdfPTable(new float[] {1f, 1f});
    mod.setWidthPercentage(100);
    mod.setSpacingAfter(6f);

    PdfPCell modHead =
        RideLayoutShared.cellText("DOCUMENTO MODIFICADO", headFont, Element.ALIGN_LEFT, Rectangle.NO_BORDER);
    modHead.setBackgroundColor(headerBg);
    modHead.setColspan(2);
    modHead.setPaddingTop(2.5f);
    modHead.setPaddingBottom(2f);
    modHead.setPaddingLeft(4f);
    modHead.setPaddingRight(4f);
    mod.addCell(modHead);

    mod.addCell(RideLayoutShared.cellLabelValuePlana("Tipo", tipoMod, font, fontBold));
    if (!datos.numeroComprobante().isBlank()) {
      mod.addCell(RideLayoutShared.cellLabelValuePlana("Nº comprobante", datos.numeroComprobante(), font, fontBold));
    } else {
      PdfPCell filler = RideLayoutShared.cellLabelValuePlana("", "", font, fontBold);
      mod.addCell(filler);
    }
    PdfPCell fechaCell = RideLayoutShared.cellLabelValuePlana("Fecha emisión doc.", fechaModStr, font, fontBold);
    fechaCell.setColspan(2);
    mod.addCell(fechaCell);

    if (!datos.motivo().isBlank()) {
      PdfPCell motivoCell = RideLayoutShared.cellLabelValuePlana("Razón de modificación", datos.motivo(), font, fontBold);
      motivoCell.setColspan(2);
      mod.addCell(motivoCell);
    }

    doc.add(mod);
    agregarMotivoSiAplica(doc, comprobante, font, fontBold);
  }

  private static String etiquetaTipoComprobanteModificado(String tipo) {
    if (tipo == null || tipo.isBlank()) {
      return "Factura";
    }
    return switch (tipo.trim().toUpperCase()) {
      case "FACTURA", "01" -> "Factura";
      case "NOTA_CREDITO", "04" -> "Nota de crédito";
      case "NOTA_DEBITO", "05" -> "Nota de débito";
      default -> tipo.trim();
    };
  }

  private static void agregarMotivoSiAplica(Document doc, Comprobante comprobante, Font font, Font fontBold)
      throws DocumentException {
    String motivo = RideLayoutShared.extraMotivo(comprobante);
    if (motivo.isBlank()) {
      return;
    }
    PdfPTable motivoTbl = new PdfPTable(1);
    motivoTbl.setWidthPercentage(100);
    motivoTbl.setSpacingAfter(6f);
    motivoTbl.addCell(RideLayoutShared.cellLabelValuePlana("Motivo", motivo, font, fontBold));
    doc.add(motivoTbl);
  }

  public static void agregarTablaDetalleModerno(
      Document doc,
      Comprobante comprobante,
      List<ComprobanteDetalle> detalles,
      RidePlantillaDto plantilla,
      Font font,
      Font fontBold,
      Font fontMuted,
      Color primary,
      Color white,
      Color border)
      throws DocumentException {
    boolean colAdicional = adicionalesEnColumna(plantilla);
    boolean colDescuento = tieneDescuentoEnLineas(detalles);
    List<Float> widths = new ArrayList<>();
    widths.add(0.7f);
    widths.add(1.1f);
    widths.add(colAdicional ? 2.4f : 3.2f);
    if (colAdicional) {
      widths.add(1.4f);
    }
    if (colDescuento) {
      widths.add(0.9f);
    }
    widths.add(1f);
    widths.add(1f);

    float[] w = new float[widths.size()];
    for (int i = 0; i < widths.size(); i++) {
      w[i] = widths.get(i);
    }
    int cols = w.length;

    PdfPTable grid = new PdfPTable(w);
    grid.setWidthPercentage(100);
    grid.setSpacingAfter(6f);
    grid.setHeaderRows(1);
    RideLayoutShared.addHeader(grid, "Cant.", primary, white, font.getSize());
    RideLayoutShared.addHeader(grid, "Código", primary, white, font.getSize());
    RideLayoutShared.addHeader(grid, "Descripción", primary, white, font.getSize());
    if (colAdicional) {
      RideLayoutShared.addHeader(grid, "Detalle adicional", primary, white, font.getSize());
    }
    if (colDescuento) {
      RideLayoutShared.addHeader(grid, "Descuento", primary, white, font.getSize());
    }
    RideLayoutShared.addHeader(grid, "Subtotal", primary, white, font.getSize());
    RideLayoutShared.addHeader(grid, "Total", primary, white, font.getSize());

    int count = 0;
    for (ComprobanteDetalle d : detalles) {
      if (count++ >= MAX_DETALLES) {
        break;
      }
      boolean alt = count % 2 == 0;
      Color rowBg = alt ? new Color(249, 250, 251) : Color.WHITE;
      grid.addCell(
          RideLayoutShared.bodyCell(
              RidePdfSupport.qty(d.getCantidad()), font, rowBg, Element.ALIGN_RIGHT, border));
      grid.addCell(
          RideLayoutShared.bodyCell(
              RidePdfSupport.dash(d.getCodigoPrincipal()), font, rowBg, Element.ALIGN_LEFT, border));
      String desc = textoDescripcionLinea(d, plantilla);
      grid.addCell(RideLayoutShared.bodyCellMultiline(desc, font, rowBg, Element.ALIGN_LEFT, border));
      if (colAdicional) {
        grid.addCell(
            RideLayoutShared.bodyCellMultiline(
                textoAdicionalesColumna(d), fontMuted, rowBg, Element.ALIGN_LEFT, border));
      }
      if (colDescuento) {
        grid.addCell(
            RideLayoutShared.bodyCell(
                RidePdfSupport.money(d.getDescuento()), font, rowBg, Element.ALIGN_RIGHT, border));
      }
      grid.addCell(
          RideLayoutShared.bodyCell(
              RidePdfSupport.money(d.getPrecioTotalSinImpuesto()), font, rowBg, Element.ALIGN_RIGHT, border));
      grid.addCell(
          RideLayoutShared.bodyCell(
              RidePdfSupport.money(RidePdfSupport.totalLinea(d)), fontBold, rowBg, Element.ALIGN_RIGHT, border));
    }
    if (detalles.size() > MAX_DETALLES) {
      PdfPCell more =
          new PdfPCell(
              new com.lowagie.text.Phrase(
                  "… " + (detalles.size() - MAX_DETALLES) + " líneas adicionales", fontMuted));
      more.setColspan(cols);
      more.setPadding(6f);
      more.setBorderColor(border);
      grid.addCell(more);
    }
    doc.add(grid);
  }

  public static void agregarTablaDetalleSri(
      Document doc,
      List<ComprobanteDetalle> detalles,
      RidePlantillaDto plantilla,
      Font font,
      Font fontBold,
      Color border)
      throws DocumentException {
    boolean colAdicional = adicionalesEnColumna(plantilla);
    boolean colDescuento = tieneDescuentoEnLineas(detalles);
    List<Float> widths = new ArrayList<>();
    widths.add(0.8f);
    widths.add(0.9f);
    widths.add(0.6f);
    widths.add(colAdicional ? 2.2f : 2.8f);
    if (colAdicional) {
      widths.add(1.2f);
    }
    if (colDescuento) {
      widths.add(0.7f);
    }
    widths.add(0.9f);
    widths.add(0.9f);
    float[] w = new float[widths.size()];
    for (int i = 0; i < widths.size(); i++) {
      w[i] = widths.get(i);
    }

    PdfPTable grid = new PdfPTable(w);
    grid.setWidthPercentage(100);
    grid.setSpacingAfter(6f);
    grid.setHeaderRows(1);
    List<String> headers = new ArrayList<>();
    headers.add("Cod. principal");
    headers.add("Cod. auxiliar");
    headers.add("Cant.");
    headers.add("Descripción");
    if (colAdicional) {
      headers.add("Detalle adicional");
    }
    if (colDescuento) {
      headers.add("Descuento");
    }
    headers.add("P. unitario");
    headers.add("Precio total");
    for (String h : headers) {
      PdfPCell hc = RideLayoutShared.cellText(h, fontBold, Element.ALIGN_CENTER, Rectangle.BOX);
      hc.setBorderColor(border);
      hc.setBackgroundColor(new Color(245, 245, 245));
      grid.addCell(hc);
    }
    int count = 0;
    for (ComprobanteDetalle d : detalles) {
      if (count++ >= MAX_DETALLES) {
        break;
      }
      grid.addCell(
          RideLayoutShared.bodyCell(
              RidePdfSupport.dash(d.getCodigoPrincipal()), font, Color.WHITE, Element.ALIGN_LEFT, border));
      grid.addCell(
          RideLayoutShared.bodyCell(
              RidePdfSupport.dash(d.getCodigoAuxiliar()), font, Color.WHITE, Element.ALIGN_LEFT, border));
      grid.addCell(
          RideLayoutShared.bodyCell(
              RidePdfSupport.qty(d.getCantidad()), font, Color.WHITE, Element.ALIGN_RIGHT, border));
      grid.addCell(
          RideLayoutShared.bodyCellMultiline(
              textoDescripcionLinea(d, plantilla), font, Color.WHITE, Element.ALIGN_LEFT, border));
      if (colAdicional) {
        grid.addCell(
            RideLayoutShared.bodyCellMultiline(
                textoAdicionalesColumna(d), font, Color.WHITE, Element.ALIGN_LEFT, border));
      }
      if (colDescuento) {
        grid.addCell(
            RideLayoutShared.bodyCell(
                RidePdfSupport.money(d.getDescuento()), font, Color.WHITE, Element.ALIGN_RIGHT, border));
      }
      grid.addCell(
          RideLayoutShared.bodyCell(
              RidePdfSupport.money(d.getPrecioUnitario()), font, Color.WHITE, Element.ALIGN_RIGHT, border));
      grid.addCell(
          RideLayoutShared.bodyCell(
              RidePdfSupport.money(d.getPrecioTotalSinImpuesto()), font, Color.WHITE, Element.ALIGN_RIGHT, border));
    }
    doc.add(grid);
  }

  public static void agregarTotales(
      Document doc,
      Comprobante comprobante,
      RidePlantillaDto plantilla,
      Font font,
      Font fontBold,
      Font fontTitle,
      Color border,
      Color headerBg,
      Color primary,
      float fsTitle)
      throws DocumentException {
    agregarTotales(doc, comprobante, plantilla, font, fontBold, fontTitle, border, headerBg, primary, fsTitle, null, null);
  }

  public static void agregarTotales(
      Document doc,
      Comprobante comprobante,
      RidePlantillaDto plantilla,
      Font font,
      Font fontBold,
      Font fontTitle,
      Color border,
      Color headerBg,
      Color primary,
      float fsTitle,
      Color accentHeader,
      Color accentFg)
      throws DocumentException {
    PdfPTable totalsWrap = new PdfPTable(new float[] {1.6f, 1f});
    totalsWrap.setWidthPercentage(100);
    totalsWrap.addCell(celdaPanelInformacionAdicional(comprobante, font, fontBold, border, accentHeader, accentFg));
    PdfPTable totals = new PdfPTable(2);
    totals.setWidthPercentage(100);
    for (String fila : RideTotalesOpciones.filasActivas(plantilla)) {
      agregarFilaTotal(totals, fila, comprobante, plantilla, font, fontBold, border, headerBg, primary, fsTitle);
    }
    PdfPCell totalsCell = new PdfPCell(totals);
    totalsCell.setBorder(Rectangle.NO_BORDER);
    totalsWrap.addCell(totalsCell);
    doc.add(totalsWrap);
  }

  private static void agregarFilaTotal(
      PdfPTable totals,
      String fila,
      Comprobante comprobante,
      RidePlantillaDto plantilla,
      Font font,
      Font fontBold,
      Color border,
      Color headerBg,
      Color primary,
      float fsTitle) {
    switch (fila) {
      case RideTotalesOpciones.SUBTOTAL_SIN_IMPUESTOS ->
          RideLayoutShared.addTotalRow(
              totals,
              "Subtotal sin impuestos",
              RidePdfSupport.money(comprobante.getSubtotalSinImpuestos()),
              font,
              fontBold,
              border);
      case RideTotalesOpciones.DESCUENTO -> {
        BigDecimal desc = comprobante.getDescuentoTotal();
        if (desc != null && desc.signum() > 0) {
          RideLayoutShared.addTotalRow(totals, "Descuento", RidePdfSupport.money(desc), font, fontBold, border);
        }
      }
      case RideTotalesOpciones.SUBTOTAL_0 ->
          agregarTotalSiPositivo(totals, "Subtotal 0%", comprobante.getSubtotal0(), font, fontBold, border);
      case RideTotalesOpciones.SUBTOTAL_GRAVADO ->
          agregarTotalSiPositivo(totals, "Subtotal gravado", comprobante.getSubtotal12(), font, fontBold, border);
      case RideTotalesOpciones.SUBTOTAL_EXENTO ->
          agregarTotalSiPositivo(totals, "Subtotal exento", comprobante.getSubtotalExento(), font, fontBold, border);
      case RideTotalesOpciones.SUBTOTAL_NO_OBJETO ->
          agregarTotalSiPositivo(
              totals, "Subtotal no objeto", comprobante.getSubtotalNoObjeto(), font, fontBold, border);
      case RideTotalesOpciones.SUBTOTAL_TARIFA_5 ->
          agregarSubtotalTarifa(totals, comprobante, BigDecimal.valueOf(5), font, fontBold, border);
      case RideTotalesOpciones.SUBTOTAL_TARIFA_15 ->
          agregarSubtotalTarifa(totals, comprobante, BigDecimal.valueOf(15), font, fontBold, border);
      case RideTotalesOpciones.ICE ->
          agregarTotalSiPositivo(totals, "ICE", comprobante.getIceTotal(), font, fontBold, border);
      case RideTotalesOpciones.IRBPNR ->
          agregarTotalSiPositivo(
              totals, "IRBPNR (botellas plásticas)", comprobante.getIrbpnrTotal(), font, fontBold, border);
      case RideTotalesOpciones.IVA ->
          RideLayoutShared.addTotalRow(
              totals, "IVA", RidePdfSupport.money(comprobante.getIvaTotal()), font, fontBold, border);
      case RideTotalesOpciones.IVA_DESGLOSE -> {
        if (mostrarDesgloseIva(plantilla)) {
          for (IvaFila filaIva : leerDesgloseIva(comprobante)) {
            RideLayoutShared.addTotalRow(
                totals,
                "IVA " + filaIva.porcentaje().stripTrailingZeros().toPlainString() + "%",
                RidePdfSupport.money(filaIva.iva()),
                font,
                fontBold,
                border);
          }
        }
      }
      case RideTotalesOpciones.PROPINA ->
          agregarTotalSiPositivo(totals, "Propina", comprobante.getPropina(), font, fontBold, border);
      case RideTotalesOpciones.VALOR_TOTAL -> {
        PdfPCell grandLabel = RideLayoutShared.cellText("VALOR TOTAL", fontBold, Element.ALIGN_RIGHT, Rectangle.BOX);
        grandLabel.setBackgroundColor(headerBg);
        PdfPCell grandVal =
            RideLayoutShared.cellText(
                RidePdfSupport.money(comprobante.getValorTotal()),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, fsTitle, primary),
                Element.ALIGN_RIGHT,
                Rectangle.BOX);
        grandVal.setBackgroundColor(headerBg);
        totals.addCell(grandLabel);
        totals.addCell(grandVal);
      }
      default -> {}
    }
  }

  private static void agregarTotalSiPositivo(
      PdfPTable totals, String label, BigDecimal valor, Font font, Font fontBold, Color border) {
    if (valor != null && valor.signum() > 0) {
      RideLayoutShared.addTotalRow(totals, label, RidePdfSupport.money(valor), font, fontBold, border);
    }
  }

  private static void agregarSubtotalTarifa(
      PdfPTable totals,
      Comprobante comprobante,
      BigDecimal tarifa,
      Font font,
      Font fontBold,
      Color border) {
    for (Map<String, Object> row : RideTotalesOpciones.leerDesgloseImpuestosMap(comprobante)) {
      BigDecimal pct = toBigDecimal(row.get("porcentaje"));
      if (pct.compareTo(tarifa) != 0) {
        continue;
      }
      BigDecimal base = toBigDecimal(row.get("baseImponible"));
      if (base.signum() == 0) {
        BigDecimal iva = toBigDecimal(row.get("iva"));
        if (iva.signum() > 0 && tarifa.signum() > 0) {
          base = iva.multiply(BigDecimal.valueOf(100)).divide(tarifa, 2, RoundingMode.HALF_UP);
        }
      }
      if (base.signum() > 0) {
        RideLayoutShared.addTotalRow(
            totals,
            "Subtotal " + tarifa.stripTrailingZeros().toPlainString() + "%",
            RidePdfSupport.money(base),
            font,
            fontBold,
            border);
      }
      return;
    }
  }

  private static PdfPCell celdaPanelInformacionAdicional(
      Comprobante comprobante,
      Font font,
      Font fontBold,
      Color border,
      Color accentHeader,
      Color accentFg) {
    PdfPTable panel = new PdfPTable(1);
    panel.setWidthPercentage(100);
    boolean modern = accentHeader != null && accentFg != null;

    if (modern) {
      PdfPCell head =
          RideLayoutShared.cellText(
              "INFORMACIÓN ADICIONAL",
              FontFactory.getFont(FontFactory.HELVETICA_BOLD, font.getSize(), accentFg),
              Element.ALIGN_LEFT,
              Rectangle.NO_BORDER);
      head.setBackgroundColor(accentHeader);
      head.setPadding(5f);
      panel.addCell(head);
    } else {
      PdfPCell head = RideLayoutShared.cellText("INFORMACIÓN ADICIONAL", fontBold, Element.ALIGN_LEFT, Rectangle.BOX);
      head.setBorderColor(border);
      head.setBackgroundColor(new Color(245, 245, 245));
      panel.addCell(head);
    }

    String glosa = leerGlosa(comprobante);
    if (!glosa.isBlank()) {
      PdfPCell glosaCell = RideLayoutShared.cellText(glosa, font, Element.ALIGN_LEFT, Rectangle.BOX);
      glosaCell.setBorderColor(border);
      glosaCell.setPadding(6f);
      panel.addCell(glosaCell);
    }

    String pagos = textoFormasPago(comprobante);
    if (!pagos.isBlank()) {
      panel.addCell(RideLayoutShared.cellLabelValue("Forma de pago SRI", pagos, font, fontBold, border));
    }

    PdfPCell wrap = new PdfPCell(panel);
    wrap.setBorder(Rectangle.NO_BORDER);
    wrap.setVerticalAlignment(Element.ALIGN_TOP);
    wrap.setPaddingRight(8f);
    return wrap;
  }

  public static String textoDescripcionLinea(ComprobanteDetalle detalle, RidePlantillaDto plantilla) {
    String base = RidePdfSupport.dash(detalle.getDescripcion());
    if (adicionalesEnColumna(plantilla)) {
      return base;
    }
    String ad = textoAdicionalesColumna(detalle);
    if (ad.isBlank()) {
      return base;
    }
    return base + "\n\n" + ad;
  }

  public static String textoAdicionalesColumna(ComprobanteDetalle detalle) {
    Map<String, Object> cd = detalle.getCustomData();
    if (cd == null) {
      return "";
    }
    List<String> html = RidePdfSupport.leerLista(cd.get("detallesAdicionalesHtml"));
    List<String> plain = RidePdfSupport.leerLista(cd.get("detallesAdicionales"));
    int slots = Math.max(html.size(), plain.size());
    List<String> bloques = new ArrayList<>();
    for (int i = 0; i < slots && i < 3; i++) {
      String h = i < html.size() ? html.get(i) : "";
      String p = i < plain.size() ? plain.get(i) : "";
      String texto = !h.isBlank() ? RidePdfSupport.htmlToPlainPdf(h) : p;
      if (!texto.isBlank()) {
        bloques.add(texto);
      }
    }
    return String.join("\n\n", bloques);
  }

  @SuppressWarnings("unchecked")
  public static List<IvaFila> leerDesgloseIva(Comprobante comprobante) {
    Map<String, Object> cd = comprobante.getCustomData();
    if (cd != null) {
      Object raw = cd.get("desgloseImpuestos");
      if (raw instanceof List<?> list && !list.isEmpty()) {
        List<IvaFila> filas = new ArrayList<>();
        for (Object item : list) {
          if (item instanceof Map<?, ?> map) {
            BigDecimal pct = toBigDecimal(map.get("porcentaje"));
            BigDecimal iva = toBigDecimal(map.get("iva"));
            if (iva.signum() != 0 || pct.signum() != 0) {
              filas.add(new IvaFila(pct, iva));
            }
          }
        }
        if (!filas.isEmpty()) {
          return filas;
        }
      }
    }
    if (comprobante.getIvaTotal() != null && comprobante.getIvaTotal().signum() > 0) {
      return List.of(new IvaFila(BigDecimal.valueOf(15), comprobante.getIvaTotal()));
    }
    return List.of();
  }

  private static BigDecimal toBigDecimal(Object raw) {
    if (raw == null) {
      return BigDecimal.ZERO;
    }
    if (raw instanceof Number n) {
      return BigDecimal.valueOf(n.doubleValue()).setScale(2, RoundingMode.HALF_UP);
    }
    try {
      return new BigDecimal(String.valueOf(raw)).setScale(2, RoundingMode.HALF_UP);
    } catch (NumberFormatException e) {
      return BigDecimal.ZERO;
    }
  }

  public record IvaFila(BigDecimal porcentaje, BigDecimal iva) {}
}
