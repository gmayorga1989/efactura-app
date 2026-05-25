package ec.tusaas.efactura.emision.ride;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import ec.tusaas.efactura.dto.ride.RidePlantillaDto;
import ec.tusaas.efactura.entity.Comprobante;
import ec.tusaas.efactura.entity.ComprobanteDetalle;
import ec.tusaas.efactura.entity.Empresa;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Genera RIDE en PDF con maquetación profesional y colores configurables. */
public final class RideLayoutPdfBuilder {

  private static final int MAX_DETALLES = 40;
  private RideLayoutPdfBuilder() {}

  public static byte[] generarFactura(
      Comprobante comprobante, List<ComprobanteDetalle> detalles, RidePlantillaDto plantilla) {
    return generar(comprobante, detalles, plantilla, RideDocumentoTitulo.fromTipo(comprobante.getTipo()));
  }

  public static byte[] generar(
      Comprobante comprobante,
      List<ComprobanteDetalle> detalles,
      RidePlantillaDto plantilla,
      RideDocumentoTitulo tituloDoc) {
    return generar(comprobante, detalles, plantilla, tituloDoc, null);
  }

  public static byte[] generar(
      Comprobante comprobante,
      List<ComprobanteDetalle> detalles,
      RidePlantillaDto plantilla,
      RideDocumentoTitulo tituloDoc,
      Image logoPreCargado) {
    RidePlantillaDto p = plantilla == null ? RidePlantillaDto.porDefecto() : plantilla;
    String diseno = p.disenoBase() == null ? "moderno" : p.disenoBase().toLowerCase();
    if ("sri_clasico".equals(diseno) || "sri".equals(diseno) || "clasico".equals(diseno)) {
      return RideLayoutSriClasicoBuilder.generar(comprobante, detalles, p, tituloDoc, logoPreCargado);
    }
    boolean compact = "compact".equalsIgnoreCase(p.densidad());
    float fs = compact ? 8f : 9f;
    float fsTitle = compact ? 11f : 12f;
    float fsHeader = compact ? 14f : 16f;

    Color primary = RidePdfSupport.color(p.colorPrimario(), "#1e5b96");
    Color accent = RidePdfSupport.color(p.colorAcento(), "#0d9488");
    Color text = RidePdfSupport.color(p.colorTexto(), "#1f2937");
    Color headerBg = RidePdfSupport.color(p.colorFondoEncabezado(), "#eef5fb");
    Color white = Color.WHITE;
    Color border = new Color(220, 226, 234);

    Font font = FontFactory.getFont(FontFactory.HELVETICA, fs, text);
    Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, fs, text);
    Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, fsTitle, primary);
    Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, fsHeader, white);
    Font fontEmisorName = FontFactory.getFont(FontFactory.HELVETICA_BOLD, fsHeader, primary);
    Font fontMuted = FontFactory.getFont(FontFactory.HELVETICA, fs - 1, new Color(100, 116, 139));

    boolean ejecutivo = "ejecutivo".equalsIgnoreCase(p.disenoBase());
    boolean claveEnCaja = "caja_factura".equalsIgnoreCase(p.ubicacionClave());
    Color textoCaja = RideContenido.textoCajaFactura(p, ejecutivo, text, white);
    String ubicacionLogo = ejecutivo ? "centro" : p.ubicacionLogo();

    Empresa empresa = comprobante.getEmpresa();
    String numero =
        comprobante.getEstablecimientoCodigo()
            + "-"
            + comprobante.getPuntoEmisionCodigo()
            + "-"
            + comprobante.getSecuencial();

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      Document doc = new Document(PageSize.A4, 40, 40, 42, 56);
      PdfWriter writer = PdfWriter.getInstance(doc, baos);
      configurarPiePagina(writer, p, fontMuted);
      doc.open();

      // —— Encabezado emisor + caja factura ——
      PdfPTable top = new PdfPTable(new float[] {1.4f, 1f});
      top.setWidthPercentage(100);
      top.setSpacingAfter(8f);

      PdfPCell emisorCell = new PdfPCell();
      emisorCell.setBorder(Rectangle.NO_BORDER);
      emisorCell.setPadding(5f);
      emisorCell.setBackgroundColor(headerBg);

      PdfPTable emisorInner = new PdfPTable(1);
      emisorInner.setWidthPercentage(100);
      RideLayoutShared.agregarLogoAntesEmisor(
          emisorInner, p, logoPreCargado, ubicacionLogo, Rectangle.NO_BORDER, null);
      RideLayoutShared.agregarBloqueEmisor(emisorInner, empresa, p, font, fontBold, fontTitle, fontEmisorName);
      emisorCell.addElement(emisorInner);
      top.addCell(emisorCell);

      PdfPCell facturaBox = new PdfPCell();
      facturaBox.setPadding(10f);
      facturaBox.setHorizontalAlignment(Element.ALIGN_CENTER);
      if (ejecutivo) {
        facturaBox.setBackgroundColor(white);
        facturaBox.setBorder(Rectangle.BOX);
        facturaBox.setBorderColor(primary);
        facturaBox.setBorderWidth(1.5f);
      } else {
        facturaBox.setBackgroundColor(primary);
        facturaBox.setBorder(Rectangle.NO_BORDER);
      }
      RideDocumentoTitulo titulo = tituloDoc == null ? RideDocumentoTitulo.fromTipo(comprobante.getTipo()) : tituloDoc;
      Color tituloColor = ejecutivo ? primary : white;
      Paragraph rideTitle = new Paragraph(titulo.linea1(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, fsHeader, tituloColor));
      rideTitle.setAlignment(Element.ALIGN_CENTER);
      rideTitle.setSpacingAfter(0f);
      Paragraph rideSub = new Paragraph(titulo.linea2(), FontFactory.getFont(FontFactory.HELVETICA, fs, tituloColor));
      rideSub.setAlignment(Element.ALIGN_CENTER);
      rideSub.setSpacingAfter(0f);
      Paragraph no = new Paragraph("Nº " + numero, FontFactory.getFont(FontFactory.HELVETICA_BOLD, fsTitle + 2, tituloColor));
      no.setAlignment(Element.ALIGN_CENTER);
      no.setSpacingBefore(3f);
      no.setSpacingAfter(0f);
      facturaBox.addElement(rideTitle);
      facturaBox.addElement(rideSub);
      facturaBox.addElement(no);
      String amb = comprobante.getAmbienteSri() == 2 ? "PRODUCCIÓN" : "PRUEBAS";
      Paragraph ambP = new Paragraph("Ambiente: " + amb, FontFactory.getFont(FontFactory.HELVETICA, fs - 1, tituloColor));
      ambP.setAlignment(Element.ALIGN_CENTER);
      ambP.setSpacingBefore(2f);
      ambP.setSpacingAfter(claveEnCaja && p.mostrarCodigoBarras() ? 4f : 2f);
      facturaBox.addElement(ambP);

      if (claveEnCaja && p.mostrarCodigoBarras()) {
        PdfPTable claveInner = new PdfPTable(1);
        claveInner.setWidthPercentage(100);
        Image barcode =
            RidePdfSupport.barcodeClave(
                comprobante.getClaveAcceso(), writer.getDirectContent(), RidePdfSupport.BARCODE_MAX_CAJA_FACTURA);
        PdfPCell bcCell = RideLayoutShared.celdaCodigoBarras(barcode, 0f);
        if (bcCell != null) {
          claveInner.addCell(bcCell);
        }
        Font claveFont = FontFactory.getFont(FontFactory.HELVETICA, fs - 2f, ejecutivo ? text : white);
        PdfPCell claveCell =
            RideLayoutShared.celdaClaveAcceso(
                comprobante.getClaveAcceso(), claveFont, ejecutivo ? border : null, Rectangle.NO_BORDER);
        if (ejecutivo) {
          claveCell.setBackgroundColor(new Color(248, 250, 252));
        }
        claveInner.addCell(claveCell);
        RideContenido.agregarFilasAutorizacion(
            claveInner, comprobante, font, fontBold, border, Rectangle.NO_BORDER, textoCaja);
        facturaBox.addElement(claveInner);
      } else if (claveEnCaja) {
        PdfPTable claveInner = new PdfPTable(1);
        claveInner.setWidthPercentage(100);
        Font claveFont = FontFactory.getFont(FontFactory.HELVETICA, fs - 2f, ejecutivo ? text : white);
        PdfPCell claveCell =
            RideLayoutShared.celdaClaveAcceso(
                comprobante.getClaveAcceso(), claveFont, ejecutivo ? border : null, Rectangle.NO_BORDER);
        if (ejecutivo) {
          claveCell.setBackgroundColor(new Color(248, 250, 252));
        }
        claveInner.addCell(claveCell);
        RideContenido.agregarFilasAutorizacion(
            claveInner, comprobante, font, fontBold, border, Rectangle.NO_BORDER, textoCaja);
        facturaBox.addElement(claveInner);
      }

      top.addCell(facturaBox);
      doc.add(top);

      if (!claveEnCaja) {
        RideContenido.agregarBloqueClaveAccesoExterno(doc, comprobante, writer, p, font, fontBold, border);
      }

      RideContenidoPorTipo.agregarCuerpoPrincipal(
          doc,
          comprobante,
          detalles,
          p,
          false,
          font,
          fontBold,
          fontMuted,
          fontTitle,
          accent,
          white,
          border,
          primary,
          headerBg,
          fsTitle);

      doc.close();
      return baos.toByteArray();
    } catch (Exception e) {
      throw new IllegalStateException("No se pudo generar el PDF RIDE: " + e.getMessage(), e);
    }
  }

  private static void configurarPiePagina(PdfWriter writer, RidePlantillaDto p, Font fontMuted) {
    String pie = p.textoPie() == null || p.textoPie().isBlank() ? "" : p.textoPie().trim() + "\n";
    String legal = pie + "Representación impresa del comprobante electrónico.\nDocumento generado por eFactura EC.";
    writer.setPageEvent(new RidePdfFooter(legal, fontMuted, 14f));
  }

  private static String extraMotivo(Comprobante comprobante) {
    Map<String, Object> cd = comprobante.getCustomData();
    if (cd == null || cd.get("motivo") == null) {
      return "";
    }
    return String.valueOf(cd.get("motivo")).trim();
  }

  private static void appendAdicionales(PdfPTable grid, ComprobanteDetalle detalle, Font fontMuted, Color rowBg) {
    Map<String, Object> cd = detalle.getCustomData();
    if (cd == null) {
      return;
    }
    List<String> html = RidePdfSupport.leerLista(cd.get("detallesAdicionalesHtml"));
    List<String> plain = RidePdfSupport.leerLista(cd.get("detallesAdicionales"));
    int slots = Math.max(html.size(), plain.size());
    for (int i = 0; i < slots && i < 3; i++) {
      String h = i < html.size() ? html.get(i) : "";
      String p = i < plain.size() ? plain.get(i) : "";
      String texto = !h.isBlank() ? RidePdfSupport.htmlToPlainPdf(h) : p;
      if (texto.isBlank()) {
        continue;
      }
      PdfPCell c = new PdfPCell(new Phrase("  › " + texto.replace("\n", " "), fontMuted));
      c.setColspan(5);
      c.setBackgroundColor(rowBg);
      c.setBorderColor(new Color(230, 234, 240));
      c.setPadding(4f);
      grid.addCell(c);
    }
  }

  private static void addHeader(PdfPTable t, String label, Color bg, Color fg, float fs) {
    Font f = FontFactory.getFont(FontFactory.HELVETICA_BOLD, fs, fg);
    PdfPCell c = new PdfPCell(new Phrase(label, f));
    c.setBackgroundColor(bg);
    c.setPadding(5f);
    c.setHorizontalAlignment(Element.ALIGN_CENTER);
    t.addCell(c);
  }

  private static PdfPCell bodyCell(String text, Font font, Color bg, int align) {
    PdfPCell c = new PdfPCell(new Phrase(text, font));
    c.setBackgroundColor(bg);
    c.setPadding(4f);
    c.setHorizontalAlignment(align);
    c.setBorderColor(new Color(230, 234, 240));
    return c;
  }

  private static PdfPCell cellText(String text, Font font, int align, int border) {
    PdfPCell c = new PdfPCell(new Phrase(text, font));
    c.setBorder(border);
    c.setPadding(5f);
    c.setHorizontalAlignment(align);
    return c;
  }

  private static PdfPCell cellLabelValue(String label, String value, Font font, Font fontBold, Color border) {
    Paragraph p = new Paragraph();
    p.add(new Chunk(label + ": ", font));
    p.add(new Chunk(value, fontBold));
    PdfPCell c = new PdfPCell(p);
    c.setPadding(5f);
    c.setBorder(Rectangle.BOX);
    c.setBorderColor(border);
    return c;
  }

  private static void addTotalRow(PdfPTable t, String label, String value, Font font, Font fontBold, Color border) {
    PdfPCell l = cellText(label, font, Element.ALIGN_LEFT, Rectangle.BOX);
    l.setBorderColor(border);
    PdfPCell v = cellText(value, fontBold, Element.ALIGN_RIGHT, Rectangle.BOX);
    v.setBorderColor(border);
    t.addCell(l);
    t.addCell(v);
  }

  private static PdfPCell emptyCell() {
    PdfPCell c = new PdfPCell(new Phrase(""));
    c.setBorder(Rectangle.NO_BORDER);
    return c;
  }
}
