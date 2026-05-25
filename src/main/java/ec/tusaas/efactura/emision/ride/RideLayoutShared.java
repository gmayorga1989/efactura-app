package ec.tusaas.efactura.emision.ride;

import com.lowagie.text.Chunk;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import ec.tusaas.efactura.dto.ride.RidePlantillaDto;
import ec.tusaas.efactura.entity.Comprobante;
import ec.tusaas.efactura.entity.ComprobanteDetalle;
import ec.tusaas.efactura.entity.Empresa;
import java.awt.Color;
import java.util.List;
import java.util.Map;

final class RideLayoutShared {

  private RideLayoutShared() {}

  /** Logo siempre arriba del bloque emisor (antes de razón social). */
  static void agregarLogoAntesEmisor(
      PdfPTable target, RidePlantillaDto p, Image logo, String ubicacionLogo, int border, Color borderColor) {
    agregarLogoEnCelda(target, p, logo, alinearLogo(ubicacionLogo), border, borderColor, 8f);
  }

  static int alinearLogo(String ubicacionLogo) {
    if (ubicacionLogo == null) {
      return Element.ALIGN_LEFT;
    }
    return switch (ubicacionLogo.toLowerCase()) {
      case "centro", "center" -> Element.ALIGN_CENTER;
      case "derecha", "right" -> Element.ALIGN_RIGHT;
      default -> Element.ALIGN_LEFT;
    };
  }

  static void agregarLogoEnCelda(
      PdfPTable target,
      RidePlantillaDto p,
      Image logo,
      int align,
      int border,
      Color borderColor,
      float paddingBottom) {
    if (!p.mostrarLogo()) {
      return;
    }
    if (logo != null) {
      PdfPCell logoCell = new PdfPCell(logo, false);
      logoCell.setBorder(border);
      if (borderColor != null) {
        logoCell.setBorderColor(borderColor);
      }
      logoCell.setHorizontalAlignment(align);
      logoCell.setPaddingBottom(paddingBottom);
      logoCell.setPaddingTop(2f);
      logoCell.setPaddingLeft(2f);
      logoCell.setPaddingRight(2f);
      target.addCell(logoCell);
      return;
    }
    if (p.marcaSinLogo()) {
      Font f = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, new Color(180, 40, 40));
      PdfPCell ph = new PdfPCell(new Phrase("SIN LOGO", f));
      ph.setBorder(border);
      if (borderColor != null) {
        ph.setBorderColor(borderColor);
      }
      ph.setHorizontalAlignment(align);
      ph.setPadding(8f);
      ph.setMinimumHeight(44f);
      ph.setPaddingBottom(paddingBottom);
      target.addCell(ph);
    }
  }

  static PdfPCell celdaCodigoBarras(Image barcode, float espacioSuperior) {
    if (barcode == null) {
      return null;
    }
    PdfPCell bc = new PdfPCell(barcode, false);
    bc.setBorder(Rectangle.NO_BORDER);
    bc.setHorizontalAlignment(Element.ALIGN_CENTER);
    bc.setVerticalAlignment(Element.ALIGN_MIDDLE);
    bc.setPaddingTop(espacioSuperior);
    bc.setPaddingBottom(4f);
    bc.setPaddingLeft(6f);
    bc.setPaddingRight(6f);
    return bc;
  }

  static PdfPCell celdaClaveAcceso(String clave, Font font, Color borderColor, int border) {
    PdfPCell claveCell =
        cellText("CLAVE DE ACCESO\n" + clave, font, Element.ALIGN_CENTER, border);
    claveCell.setPadding(3f);
    claveCell.setPaddingTop(2f);
    if (borderColor != null) {
      claveCell.setBorderColor(borderColor);
    }
    return claveCell;
  }

  static PdfPCell cellText(String text, Font font, int align, int border) {
    PdfPCell c = new PdfPCell(new Phrase(text, font));
    c.setBorder(border);
    c.setPadding(5f);
    c.setHorizontalAlignment(align);
    return c;
  }

  /** Texto sin borde y con poco interlineado (cabecera emisor). */
  static PdfPCell cellTextPlana(String text, Font font, int align) {
    Paragraph para = new Paragraph(text, font);
    para.setLeading(font.getSize() + 1f);
    PdfPCell c = new PdfPCell(para);
    c.setBorder(Rectangle.NO_BORDER);
    c.setPaddingTop(0.5f);
    c.setPaddingBottom(0.5f);
    c.setPaddingLeft(2f);
    c.setPaddingRight(2f);
    c.setHorizontalAlignment(align);
    return c;
  }

  static PdfPCell cellLabelValue(String label, String value, Font font, Font fontBold, Color border) {
    Paragraph para = new Paragraph();
    para.add(new Chunk(label + ": ", font));
    para.add(new Chunk(value, fontBold));
    PdfPCell c = new PdfPCell(para);
    c.setPadding(5f);
    c.setBorder(Rectangle.BOX);
    c.setBorderColor(border);
    return c;
  }

  /** Celda etiqueta/valor sin bordes ni rejilla (bloque cliente). */
  static PdfPCell cellLabelValuePlana(String label, String value, Font font, Font fontBold) {
    Paragraph para = new Paragraph();
    para.setLeading(font.getSize() + 1.5f);
    para.add(new Chunk(label + ": ", font));
    para.add(new Chunk(value == null || value.isBlank() ? "-" : value, fontBold));
    PdfPCell c = new PdfPCell(para);
    c.setBorder(Rectangle.NO_BORDER);
    c.setPaddingLeft(4f);
    c.setPaddingRight(4f);
    c.setPaddingTop(1.5f);
    c.setPaddingBottom(1.5f);
    return c;
  }

  static void addHeader(PdfPTable t, String label, Color bg, Color fg, float fs) {
    Font f = FontFactory.getFont(FontFactory.HELVETICA_BOLD, fs, fg);
    PdfPCell c = new PdfPCell(new Phrase(label, f));
    c.setBackgroundColor(bg);
    c.setPaddingTop(3f);
    c.setPaddingBottom(3f);
    c.setPaddingLeft(4f);
    c.setPaddingRight(4f);
    c.setHorizontalAlignment(Element.ALIGN_CENTER);
    t.addCell(c);
  }

  static PdfPCell bodyCell(String text, Font font, Color bg, int align, Color borderColor) {
    PdfPCell c = new PdfPCell(new Phrase(text, font));
    c.setBackgroundColor(bg);
    c.setPadding(4f);
    c.setHorizontalAlignment(align);
    c.setBorderColor(borderColor);
    return c;
  }

  static PdfPCell bodyCellMultiline(String text, Font font, Color bg, int align, Color borderColor) {
    Paragraph para = new Paragraph();
    if (text == null || text.isBlank()) {
      para.add(new Chunk("-", font));
    } else {
      String[] lines = text.split("\n");
      for (int i = 0; i < lines.length; i++) {
        if (i > 0) {
          para.add(Chunk.NEWLINE);
        }
        para.add(new Chunk(lines[i], font));
      }
    }
    PdfPCell c = new PdfPCell(para);
    c.setBackgroundColor(bg);
    c.setPadding(4f);
    c.setHorizontalAlignment(align);
    c.setBorderColor(borderColor);
    return c;
  }

  static void addTotalRow(PdfPTable t, String label, String value, Font font, Font fontBold, Color border) {
    PdfPCell l = cellText(label, font, Element.ALIGN_LEFT, Rectangle.BOX);
    l.setBorderColor(border);
    PdfPCell v = cellText(value, fontBold, Element.ALIGN_RIGHT, Rectangle.BOX);
    v.setBorderColor(border);
    t.addCell(l);
    t.addCell(v);
  }

  static PdfPCell emptyCell() {
    PdfPCell c = new PdfPCell(new Phrase(""));
    c.setBorder(Rectangle.NO_BORDER);
    return c;
  }

  static void appendAdicionales(
      PdfPTable grid, ComprobanteDetalle detalle, Font fontMuted, Color rowBg, int colspan, Color borderColor) {
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
      c.setColspan(colspan);
      c.setBackgroundColor(rowBg);
      c.setBorderColor(borderColor);
      c.setPadding(4f);
      grid.addCell(c);
    }
  }

  static void agregarBloqueEmisor(
      PdfPTable emisorInner,
      Empresa empresa,
      RidePlantillaDto p,
      Font font,
      Font fontBold,
      Font fontTitle,
      Font fontEmisorName) {
    emisorInner.addCell(cellTextPlana(empresa.getRazonSocial(), fontEmisorName, Element.ALIGN_LEFT));
    String nc = empresa.getNombreComercial();
    if (p.mostrarNombreComercial()
        && nc != null
        && !nc.isBlank()
        && !nc.equalsIgnoreCase(empresa.getRazonSocial())) {
      emisorInner.addCell(cellTextPlana(nc, fontTitle, Element.ALIGN_LEFT));
    }
    emisorInner.addCell(cellTextPlana("RUC: " + empresa.getRuc(), fontBold, Element.ALIGN_LEFT));
    emisorInner.addCell(
        cellTextPlana(
            "Dir. matriz: " + RidePdfSupport.dash(empresa.getDireccionMatriz()), font, Element.ALIGN_LEFT));
    emisorInner.addCell(
        cellTextPlana(
            "Obligado contabilidad: " + (empresa.isObligadoContabilidad() ? "SI" : "NO"),
            font,
            Element.ALIGN_LEFT));
    if (empresa.getContribuyenteEspecial() != null && !empresa.getContribuyenteEspecial().isBlank()) {
      emisorInner.addCell(
          cellTextPlana(
              "Contribuyente especial: " + empresa.getContribuyenteEspecial(), font, Element.ALIGN_LEFT));
    }
    RideContenido.agregarDatosEmisorExtendidos(emisorInner, empresa, font);
  }

  static String extraMotivo(Comprobante comprobante) {
    Map<String, Object> cd = comprobante.getCustomData();
    if (cd == null || cd.get("motivo") == null) {
      return "";
    }
    return String.valueOf(cd.get("motivo")).trim();
  }
}
