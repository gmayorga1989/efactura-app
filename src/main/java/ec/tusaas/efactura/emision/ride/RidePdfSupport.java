package ec.tusaas.efactura.emision.ride;

import com.lowagie.text.Image;
import com.lowagie.text.pdf.Barcode128;
import com.lowagie.text.pdf.PdfContentByte;
import ec.tusaas.efactura.entity.ComprobanteDetalle;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

final class RidePdfSupport {

  private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();

  private RidePdfSupport() {}

  static Color color(String hex, String fallbackHex) {
    try {
      String h = hex == null ? fallbackHex : hex.trim();
      if (!h.startsWith("#")) {
        h = "#" + h;
      }
      return Color.decode(h);
    } catch (Exception e) {
      return Color.decode(fallbackHex);
    }
  }

  static Image cargarLogoHttp(String logoUrl) {
    if (logoUrl == null || logoUrl.isBlank()) {
      return null;
    }
    try {
      HttpRequest req = HttpRequest.newBuilder(URI.create(logoUrl)).GET().timeout(Duration.ofSeconds(8)).build();
      HttpResponse<byte[]> res = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
      if (res.statusCode() >= 400) {
        return null;
      }
      return imagenDesdeBytes(res.body(), logoUrl, 110, 85);
    } catch (Exception e) {
      return null;
    }
  }

  static Image imagenDesdeBytes(byte[] bytes, String hintUrl, float maxW, float maxH) {
    if (bytes == null || bytes.length == 0) {
      return null;
    }
    String formato = detectarFormatoImagen(bytes);
    if ("SVG".equals(formato) || "HTML".equals(formato)) {
      return null;
    }
    BufferedImage bi = leerBufferedImage(bytes, formato);
    if (bi != null) {
      try {
        Image img = Image.getInstance(bi, null);
        img.scaleToFit(maxW, maxH);
        return img;
      } catch (Exception ignored) {
        // intentar OpenPDF directo abajo
      }
    }
    return null;
  }

  private static String detectarFormatoImagen(byte[] b) {
    if (b == null || b.length < 4) {
      return "UNKNOWN";
    }
    if (b[0] == (byte) 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
      return "PNG";
    }
    if (b[0] == (byte) 0xFF && b[1] == (byte) 0xD8) {
      return "JPEG";
    }
    if (b.length >= 12
        && b[0] == 'R'
        && b[1] == 'I'
        && b[2] == 'F'
        && b[3] == 'F'
        && b[8] == 'W'
        && b[9] == 'E'
        && b[10] == 'B'
        && b[11] == 'P') {
      return "WEBP";
    }
    if (b[0] == 'G' && b[1] == 'I' && b[2] == 'F') {
      return "GIF";
    }
    if (b[0] == '<' || (b.length >= 5 && b[0] == '{' && b[1] == '"')) {
      return "HTML";
    }
    String hintStart = new String(b, 0, Math.min(b.length, 32), java.nio.charset.StandardCharsets.UTF_8);
    if (hintStart.contains("<svg") || hintStart.contains("<?xml")) {
      return "SVG";
    }
    return "UNKNOWN";
  }

  private static BufferedImage leerBufferedImage(byte[] bytes, String formato) {
    try {
      if ("WEBP".equals(formato)) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
          Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("webp");
          if (readers.hasNext()) {
            ImageReader reader = readers.next();
            try {
              reader.setInput(iis, true, true);
              return reader.read(0);
            } finally {
              reader.dispose();
            }
          }
        }
      }
      try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
        return ImageIO.read(in);
      }
    } catch (Exception e) {
      return null;
    }
  }

  /** Ancho máximo del código de barras dentro de la caja de factura (puntos PDF). */
  static final float BARCODE_MAX_CAJA_FACTURA = 168f;

  /** Ancho máximo del código de barras a ancho completo debajo del encabezado. */
  static final float BARCODE_MAX_ANCHO_COMPLETO = 460f;

  static Image barcodeClave(String clave, PdfContentByte canvas) {
    return barcodeClave(clave, canvas, BARCODE_MAX_ANCHO_COMPLETO);
  }

  static Image barcodeClave(String clave, PdfContentByte canvas, float maxWidth) {
    if (clave == null || clave.isBlank()) {
      return null;
    }
    float ancho = maxWidth > 0 ? maxWidth : BARCODE_MAX_ANCHO_COMPLETO;
    try {
      Barcode128 code = new Barcode128();
      code.setCode(clave.trim());
      code.setBarHeight(24f);
      code.setX(0.72f);
      java.awt.Image awt = code.createAwtImage(Color.BLACK, Color.WHITE);
      BufferedImage bi = new BufferedImage(awt.getWidth(null), awt.getHeight(null), BufferedImage.TYPE_INT_RGB);
      bi.getGraphics().drawImage(awt, 0, 0, null);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(bi, "png", baos);
      Image img = Image.getInstance(baos.toByteArray());
      img.scaleToFit(ancho, 30f);
      return img;
    } catch (Exception e) {
      return null;
    }
  }

  static String money(BigDecimal v) {
    return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP).toPlainString();
  }

  static String qty(BigDecimal v) {
    if (v == null) {
      return "0";
    }
    return v.stripTrailingZeros().toPlainString();
  }

  static String dash(String v) {
    return v == null || v.isBlank() ? "-" : v.trim();
  }

  static BigDecimal totalLinea(ComprobanteDetalle detalle) {
    BigDecimal sub = detalle.getPrecioTotalSinImpuesto();
    BigDecimal ivaPct = ivaPorcentaje(detalle.getCustomData());
    BigDecimal iva = sub.multiply(ivaPct).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
    return sub.add(iva).setScale(2, RoundingMode.HALF_UP);
  }

  private static BigDecimal ivaPorcentaje(Map<String, Object> cd) {
    if (cd == null) {
      return BigDecimal.valueOf(15);
    }
    Object raw = cd.get("ivaPorcentaje");
    if (raw instanceof Number n) {
      return BigDecimal.valueOf(n.doubleValue());
    }
    if (raw != null) {
      try {
        return new BigDecimal(String.valueOf(raw));
      } catch (NumberFormatException ignored) {
        return BigDecimal.valueOf(15);
      }
    }
    return BigDecimal.valueOf(15);
  }

  @SuppressWarnings("unchecked")
  static List<String> leerLista(Object raw) {
    if (!(raw instanceof List<?> list)) {
      return List.of();
    }
    return list.stream().map(v -> v == null ? "" : String.valueOf(v).trim()).filter(s -> !s.isBlank()).toList();
  }

  static String htmlToPlainPdf(String html) {
    if (html == null || html.isBlank()) {
      return "";
    }
    return html
        .replaceAll("(?i)<br\\s*/?>", "\n")
        .replaceAll("(?i)</p>", "\n")
        .replaceAll("(?i)</li>", "\n")
        .replaceAll("(?i)<li[^>]*>", "• ")
        .replaceAll("<[^>]+>", "")
        .replaceAll("[ \\t]+\\n", "\n")
        .replaceAll("\\n{3,}", "\n\n")
        .trim();
  }
}
