package ec.tusaas.efactura.emision.ride;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

/** Pie de página fijo en la parte inferior de cada hoja del RIDE. */
final class RidePdfFooter extends PdfPageEventHelper {

  private final Phrase phrase;
  private final float marginBottom;

  RidePdfFooter(String texto, Font font, float marginBottom) {
    this.phrase = new Phrase(texto, font);
    this.marginBottom = marginBottom;
  }

  @Override
  public void onEndPage(PdfWriter writer, Document document) {
    float y = document.bottom() + marginBottom;
    if (y > document.top() - 12f) {
      y = 18f;
    }
    ColumnText.showTextAligned(
        writer.getDirectContent(),
        Element.ALIGN_CENTER,
        phrase,
        (document.right() + document.left()) / 2f,
        y,
        0);
  }
}
