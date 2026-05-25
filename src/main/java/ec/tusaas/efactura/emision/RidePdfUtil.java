package ec.tusaas.efactura.emision;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class RidePdfUtil {

  private RidePdfUtil() {}

  static byte[] simplePdf(List<String> lines) {
    StringBuilder content = new StringBuilder();
    content.append("BT\n/F1 10 Tf\n50 790 Td\n14 TL\n");
    for (String line : lines) {
      content.append("(").append(escape(line)).append(") Tj\nT*\n");
    }
    content.append("ET\n");
    byte[] stream = content.toString().getBytes(StandardCharsets.ISO_8859_1);

    List<byte[]> objects =
        List.of(
            ascii("<< /Type /Catalog /Pages 2 0 R >>\n"),
            ascii("<< /Type /Pages /Kids [3 0 R] /Count 1 >>\n"),
            ascii(
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                    + "/Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\n"),
            ascii("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\n"),
            concat(ascii("<< /Length " + stream.length + " >>\nstream\n"), stream, ascii("\nendstream\n")));

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    write(out, ascii("%PDF-1.4\n"));
    List<Integer> offsets = new ArrayList<>();
    for (int i = 0; i < objects.size(); i++) {
      offsets.add(out.size());
      write(out, ascii((i + 1) + " 0 obj\n"));
      write(out, objects.get(i));
      write(out, ascii("endobj\n"));
    }
    int xref = out.size();
    write(out, ascii("xref\n0 " + (objects.size() + 1) + "\n"));
    write(out, ascii("0000000000 65535 f \n"));
    for (Integer offset : offsets) {
      write(out, ascii(String.format("%010d 00000 n \n", offset)));
    }
    write(out, ascii("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\n"));
    write(out, ascii("startxref\n" + xref + "\n%%EOF\n"));
    return out.toByteArray();
  }

  static String money(BigDecimal value) {
    return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP).toPlainString();
  }

  static String numeric(BigDecimal value) {
    return value == null ? "0.00" : value.stripTrailingZeros().toPlainString();
  }

  static String fit(String value, int length) {
    String clean = value == null ? "" : value.replaceAll("\\s+", " ").trim();
    if (clean.length() > length) {
      return clean.substring(0, Math.max(0, length - 1)) + " ";
    }
    return String.format("%-" + length + "s", clean);
  }

  static String valueOrDash(String value) {
    return value == null || value.isBlank() ? "-" : value;
  }

  private static String escape(String value) {
    return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
  }

  private static byte[] ascii(String value) {
    return value.getBytes(StandardCharsets.ISO_8859_1);
  }

  private static byte[] concat(byte[]... parts) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    for (byte[] part : parts) {
      write(out, part);
    }
    return out.toByteArray();
  }

  private static void write(ByteArrayOutputStream out, byte[] value) {
    out.write(value, 0, value.length);
  }
}
