package ec.tusaas.efactura.emision.ride;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
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
import java.util.List;

/** Maquetación tipo formulario SRI (cuadros, logo superior izquierdo, clave en caja derecha). */
final class RideLayoutSriClasicoBuilder {

  private static final int MAX_DETALLES = 40;
  private RideLayoutSriClasicoBuilder() {}

  static byte[] generar(
      Comprobante comprobante,
      List<ComprobanteDetalle> detalles,
      RidePlantillaDto plantilla,
      RideDocumentoTitulo tituloDoc,
      Image logo) {
    RidePlantillaDto p = plantilla == null ? RidePlantillaDto.presetSriClasico() : plantilla;
    float fs = 8f;
    Color border = new Color(0, 0, 0);
    Color text = RidePdfSupport.color(p.colorTexto(), "#111111");
    Font font = FontFactory.getFont(FontFactory.HELVETICA, fs, text);
    Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, fs, text);
    Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f, text);

    Empresa empresa = comprobante.getEmpresa();
    String numero =
        comprobante.getEstablecimientoCodigo()
            + "-"
            + comprobante.getPuntoEmisionCodigo()
            + "-"
            + comprobante.getSecuencial();
    String amb = comprobante.getAmbienteSri() == 2 ? "PRODUCCIÓN" : "PRUEBAS";

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      Font fontPie = FontFactory.getFont(FontFactory.HELVETICA, 7f, new Color(80, 80, 80));
      Document doc = new Document(PageSize.A4, 36, 36, 36, 52);
      PdfWriter writer = PdfWriter.getInstance(doc, baos);
      String pie = p.textoPie() == null || p.textoPie().isBlank() ? "" : p.textoPie().trim() + "\n";
      writer.setPageEvent(
          new RidePdfFooter(
              pie + "Representación impresa del comprobante electrónico.\nDocumento generado por eFactura EC.",
              fontPie,
              12f));
      doc.open();

      // —— Cabecera en 2 secciones: emisor (logo arriba + datos) | caja factura+clave ——
      PdfPTable top = new PdfPTable(new float[] {1.35f, 1f});
      top.setWidthPercentage(100);
      top.setSpacingAfter(6f);

      PdfPTable emisorCol = new PdfPTable(1);
      emisorCol.setWidthPercentage(100);
      RideLayoutShared.agregarLogoAntesEmisor(
          emisorCol, p, logo, p.ubicacionLogo(), Rectangle.NO_BORDER, border);
      RideLayoutShared.agregarBloqueEmisor(emisorCol, empresa, p, font, fontBold, fontTitle, fontTitle);
      PdfPCell emisorWrap = new PdfPCell(emisorCol);
      emisorWrap.setBorder(Rectangle.BOX);
      emisorWrap.setBorderColor(border);
      emisorWrap.setPadding(6f);
      emisorWrap.setVerticalAlignment(Element.ALIGN_TOP);
      top.addCell(emisorWrap);

      PdfPTable facturaCol = new PdfPTable(1);
      facturaCol.setWidthPercentage(100);
      facturaCol.addCell(RideLayoutShared.cellText("R.U.C.: " + empresa.getRuc(), fontBold, Element.ALIGN_LEFT, Rectangle.NO_BORDER));
      RideDocumentoTitulo titulo = tituloDoc == null ? RideDocumentoTitulo.fromTipo(comprobante.getTipo()) : tituloDoc;
      facturaCol.addCell(
          RideLayoutShared.cellText(
              titulo.linea1() + " " + titulo.linea2(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11f, text), Element.ALIGN_CENTER, Rectangle.NO_BORDER));
      facturaCol.addCell(
          RideLayoutShared.cellText("No. " + numero, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f, text), Element.ALIGN_CENTER, Rectangle.NO_BORDER));
      PdfPCell ambCell = RideLayoutShared.cellLabelValue("Ambiente", amb, font, fontBold, border);
      ambCell.setPaddingBottom(6f);
      facturaCol.addCell(ambCell);
      facturaCol.addCell(RideLayoutShared.cellLabelValue("Emisión", "NORMAL", font, fontBold, border));

      boolean claveEnCaja = "caja_factura".equalsIgnoreCase(p.ubicacionClave());
      if (claveEnCaja) {
        if (p.mostrarCodigoBarras()) {
          Image barcode =
              RidePdfSupport.barcodeClave(
                  comprobante.getClaveAcceso(), writer.getDirectContent(), RidePdfSupport.BARCODE_MAX_CAJA_FACTURA);
          PdfPCell bc = RideLayoutShared.celdaCodigoBarras(barcode, 8f);
          if (bc != null) {
            facturaCol.addCell(bc);
          }
        }
        PdfPCell clave =
            RideLayoutShared.celdaClaveAcceso(
                comprobante.getClaveAcceso(),
                FontFactory.getFont(FontFactory.HELVETICA, 7f, text),
                border,
                Rectangle.BOX);
        facturaCol.addCell(clave);
        RideContenido.agregarFilasAutorizacion(
            facturaCol, comprobante, font, fontBold, border, Rectangle.BOX, text);
      }

      PdfPCell facturaWrap = new PdfPCell(facturaCol);
      facturaWrap.setBorder(Rectangle.BOX);
      facturaWrap.setBorderColor(border);
      facturaWrap.setPadding(6f);
      top.addCell(facturaWrap);
      doc.add(top);

      if (!claveEnCaja) {
        RideContenido.agregarBloqueClaveAccesoExterno(doc, comprobante, writer, p, font, fontBold, border);
      }

      Color primary = Color.BLACK;
      Color headerBg = new Color(245, 245, 245);
      RideContenidoPorTipo.agregarCuerpoPrincipal(
          doc,
          comprobante,
          detalles,
          p,
          true,
          font,
          fontBold,
          font,
          fontTitle,
          primary,
          Color.WHITE,
          border,
          primary,
          headerBg,
          fs + 2);

      doc.close();
      return baos.toByteArray();
    } catch (Exception e) {
      throw new IllegalStateException("No se pudo generar el PDF RIDE (SRI clásico): " + e.getMessage(), e);
    }
  }
}
