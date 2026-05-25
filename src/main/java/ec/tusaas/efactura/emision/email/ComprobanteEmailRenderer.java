package ec.tusaas.efactura.emision.email;

import ec.tusaas.efactura.dto.email.EmailPlantillaDto;
import ec.tusaas.efactura.emision.ride.RideDocumentoTitulo;
import ec.tusaas.efactura.entity.Comprobante;
import ec.tusaas.efactura.entity.Empresa;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

public final class ComprobanteEmailRenderer {

  private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private ComprobanteEmailRenderer() {}

  public static String renderAsunto(EmailPlantillaDto plantilla, Comprobante comprobante, String numero) {
    String tipo = etiquetaTipoCorreo(comprobante.getTipo());
    String tpl = plantilla.asuntoPlantilla();
    if (tpl == null || tpl.isBlank()) {
      return tipo + " Nº " + numero;
    }
    String out =
        tpl.replace("{{tipo}}", tipo)
            .replace("{{TIPO}}", tipo)
            .replace("{{numero}}", numero)
            .replace("{{NUMERO}}", numero);
    if (!tpl.contains("{{tipo}}") && !tpl.contains("{{TIPO}}")) {
      return tipo + " — " + out;
    }
    return out;
  }

  static String etiquetaTipoCorreo(String tipo) {
    return switch (RideDocumentoTitulo.normalizarTipo(tipo)) {
      case "NOTA_CREDITO" -> "Nota de crédito";
      case "NOTA_DEBITO" -> "Nota de débito";
      case "GUIA_REMISION" -> "Guía de remisión";
      case "RETENCION" -> "Comprobante de retención";
      case "LIQUIDACION_COMPRA" -> "Liquidación de compra";
      default -> "Factura";
    };
  }

  public static String renderHtml(
      EmailPlantillaDto plantilla,
      Empresa empresa,
      Comprobante comprobante,
      String numero,
      String logoUrl) {
    if ("corporativo".equalsIgnoreCase(plantilla.disenoBase())) {
      return corporativo(plantilla, empresa, comprobante, numero, logoUrl);
    }
    return moderno(plantilla, empresa, comprobante, numero, logoUrl);
  }

  public static String renderText(
      EmailPlantillaDto plantilla,
      Empresa empresa,
      Comprobante comprobante,
      String numero) {
    StringBuilder sb = new StringBuilder();
    sb.append(introTexto(plantilla, comprobante.getRazonSocialReceptor())).append("\n\n");
    if (plantilla.mostrarResumen()) {
      sb.append("Comprobante: ").append(numero).append('\n');
      sb.append("Emisor: ").append(empresa.getRazonSocial()).append('\n');
      sb.append("Fecha: ").append(formatearFecha(comprobante)).append('\n');
      sb.append("Total: ").append(money(comprobante.getValorTotal())).append('\n');
      if (comprobante.getClaveAcceso() != null) {
        sb.append("Clave de acceso: ").append(comprobante.getClaveAcceso()).append('\n');
      }
    }
    sb.append("\nAdjuntamos el RIDE (PDF) y el XML del comprobante electrónico.\n");
    if (plantilla.textoPie() != null && !plantilla.textoPie().isBlank()) {
      sb.append('\n').append(plantilla.textoPie().trim()).append('\n');
    }
    return sb.toString();
  }

  private static String moderno(
      EmailPlantillaDto p,
      Empresa empresa,
      Comprobante c,
      String numero,
      String logoUrl) {
    String primary = escape(p.colorPrimario());
    String intro = introHtml(p, c.getRazonSocialReceptor());
    String resumen = p.mostrarResumen() ? resumenHtml(c, numero, primary) : "";
    String logo =
        p.mostrarLogo() && logoUrl != null && !logoUrl.isBlank()
            ? "<tr><td style=\"padding:20px 28px 0;text-align:center;\"><img src=\""
                + escape(logoUrl)
                + "\" alt=\"Logo\" style=\"max-width:180px;max-height:72px;\" /></td></tr>"
            : "";
    String pie =
        p.textoPie() == null || p.textoPie().isBlank()
            ? "Documento generado por eFactura EC. Los archivos RIDE y XML van adjuntos a este correo."
            : escape(p.textoPie());
    String emisor =
        empresa.getNombreComercial() != null && !empresa.getNombreComercial().isBlank()
            ? empresa.getNombreComercial()
            : empresa.getRazonSocial();
    return """
        <!doctype html>
        <html lang="es">
          <body style="margin:0;padding:0;background:#f5f7fb;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f5f7fb;padding:32px 16px;">
              <tr><td align="center">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:620px;background:#ffffff;border:1px solid #e5e7eb;border-radius:14px;overflow:hidden;">
                  <tr><td style="background:%s;padding:22px 28px;color:#ffffff;">
                    <div style="font-size:12px;letter-spacing:.08em;text-transform:uppercase;opacity:.9;">%s</div>
                    <div style="font-size:22px;font-weight:700;line-height:1.3;margin-top:6px;">Comprobante electrónico</div>
                    <div style="font-size:15px;margin-top:6px;opacity:.95;">Nº %s</div>
                  </td></tr>
                  %s
                  <tr><td style="padding:26px 28px 8px;">
                    <p style="font-size:16px;line-height:1.6;margin:0 0 16px;">%s</p>
                    %s
                    <p style="font-size:14px;line-height:1.6;color:#475569;margin:18px 0 0;">En este correo encontrará adjuntos el <strong>RIDE (PDF)</strong> y el <strong>XML</strong> de su comprobante.</p>
                  </td></tr>
                  <tr><td style="padding:18px 28px 24px;border-top:1px solid #eef2f7;color:#94a3b8;font-size:12px;line-height:1.5;">%s</td></tr>
                </table>
              </td></tr>
            </table>
          </body>
        </html>
        """
        .formatted(primary, escape(emisor), escape(numero), logo, intro, resumen, pie);
  }

  private static String corporativo(
      EmailPlantillaDto p,
      Empresa empresa,
      Comprobante c,
      String numero,
      String logoUrl) {
    String accent = escape(p.colorAcento());
    String intro = introHtml(p, c.getRazonSocialReceptor());
    String resumen = p.mostrarResumen() ? resumenHtml(c, numero, accent) : "";
    String logo =
        p.mostrarLogo() && logoUrl != null && !logoUrl.isBlank()
            ? "<p style=\"margin:0 0 16px;\"><img src=\""
                + escape(logoUrl)
                + "\" alt=\"Logo\" style=\"max-width:160px;max-height:64px;\" /></p>"
            : "";
    String pie =
        p.textoPie() == null || p.textoPie().isBlank()
            ? "Los archivos RIDE (PDF) y XML se envían como adjuntos."
            : escape(p.textoPie());
    return """
        <!doctype html>
        <html lang="es">
          <body style="margin:0;padding:0;background:#ffffff;font-family:Georgia,'Times New Roman',serif;color:#111827;">
            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="padding:28px 16px;">
              <tr><td align="center">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:600px;border:1px solid #d1d5db;">
                  <tr><td style="padding:24px 28px;border-bottom:3px solid %s;">
                    %s
                    <div style="font-size:20px;font-weight:700;">%s</div>
                    <div style="font-size:13px;color:#6b7280;margin-top:4px;">Comprobante Nº %s</div>
                  </td></tr>
                  <tr><td style="padding:24px 28px;font-family:Arial,Helvetica,sans-serif;font-size:15px;line-height:1.6;">
                    <p style="margin:0 0 14px;">%s</p>
                    %s
                    <p style="margin:16px 0 0;color:#374151;">Adjuntos: RIDE (PDF) y XML del comprobante electrónico.</p>
                  </td></tr>
                  <tr><td style="padding:16px 28px;background:#f9fafb;font-family:Arial,Helvetica,sans-serif;font-size:12px;color:#6b7280;">%s</td></tr>
                </table>
              </td></tr>
            </table>
          </body>
        </html>
        """
        .formatted(accent, logo, escape(empresa.getRazonSocial()), escape(numero), intro, resumen, pie);
  }

  private static String resumenHtml(Comprobante c, String numero, String accentColor) {
    return """
        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f8fafc;border:1px solid #e5e7eb;border-radius:10px;margin:0 0 8px;">
          <tr><td style="padding:14px 16px;font-family:Arial,Helvetica,sans-serif;">
            <div style="font-size:12px;color:#64748b;text-transform:uppercase;">Resumen</div>
            <div style="font-size:15px;margin-top:8px;"><strong>Número:</strong> %s</div>
            <div style="font-size:15px;margin-top:4px;"><strong>Fecha emisión:</strong> %s</div>
            <div style="font-size:15px;margin-top:4px;"><strong>Total:</strong> <span style="color:%s;font-weight:700;">%s</span></div>
            <div style="font-size:12px;color:#64748b;margin-top:10px;word-break:break-all;"><strong>Clave de acceso:</strong> %s</div>
          </td></tr>
        </table>
        """
        .formatted(
            escape(numero),
            escape(formatearFecha(c)),
            escape(accentColor),
            escape(money(c.getValorTotal())),
            escape(c.getClaveAcceso() == null ? "-" : c.getClaveAcceso()));
  }

  private static String introHtml(EmailPlantillaDto p, String receptor) {
    if (p.textoIntro() != null && !p.textoIntro().isBlank()) {
      return escape(p.textoIntro().trim()).replace("\n", "<br />");
    }
    String nombre = receptor == null || receptor.isBlank() ? "cliente" : receptor.trim();
    return "Estimado(a) <strong>" + escape(nombre) + "</strong>,";
  }

  private static String introTexto(EmailPlantillaDto p, String receptor) {
    if (p.textoIntro() != null && !p.textoIntro().isBlank()) {
      return p.textoIntro().trim();
    }
    String nombre = receptor == null || receptor.isBlank() ? "cliente" : receptor.trim();
    return "Estimado(a) " + nombre + ",";
  }

  private static String formatearFecha(Comprobante c) {
    if (c.getFechaEmision() == null) {
      return "-";
    }
    return FECHA.format(c.getFechaEmision());
  }

  private static String money(BigDecimal v) {
    if (v == null) {
      return "0.00";
    }
    return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }

  private static String escape(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }
}
