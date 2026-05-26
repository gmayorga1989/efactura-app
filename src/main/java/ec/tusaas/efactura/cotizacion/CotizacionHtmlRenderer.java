package ec.tusaas.efactura.cotizacion;

import ec.tusaas.efactura.entity.Cotizacion;
import ec.tusaas.efactura.entity.CotizacionAdjunto;
import ec.tusaas.efactura.entity.CotizacionDetalle;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.entity.Vendedor;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CotizacionHtmlRenderer {

  private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private CotizacionHtmlRenderer() {}

  public static String render(
      Empresa empresa,
      Cotizacion cotizacion,
      List<CotizacionDetalle> detalles,
      List<CotizacionAdjunto> adjuntos,
      String mensajeAdicional) {
    Map<String, Object> plantilla = cotizacion.getPlantillaJson();
    String primario = CotizacionPlantillaUtil.str(plantilla, "colorPrimario", "#1e5b96");
    String acento = CotizacionPlantillaUtil.str(plantilla, "colorAcento", "#0ea5e9");
    String texto = CotizacionPlantillaUtil.str(plantilla, "colorTexto", "#0f172a");
    String pie = CotizacionPlantillaUtil.str(plantilla, "textoPie", "Gracias por su preferencia.");
    boolean mostrarVendedor = CotizacionPlantillaUtil.bool(plantilla, "mostrarVendedor", true);

    StringBuilder sb = new StringBuilder();
    sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/></head><body style=\"margin:0;padding:24px;background:#f1f5f9;font-family:Inter,Segoe UI,sans-serif;color:")
        .append(texto)
        .append(";\">");
    sb.append("<div style=\"max-width:720px;margin:0 auto;background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 12px 40px rgba(15,23,42,.12);\">");
    sb.append("<div style=\"padding:28px 32px;background:linear-gradient(135deg,")
        .append(primario)
        .append(",")
        .append(acento)
        .append(");color:#fff;\">");
    sb.append("<div style=\"font-size:12px;opacity:.9;letter-spacing:.08em;text-transform:uppercase;\">Cotización / Proforma</div>");
    sb.append("<h1 style=\"margin:8px 0 0;font-size:26px;font-weight:800;\">")
        .append(escape(empresa.getNombreComercial() != null ? empresa.getNombreComercial() : empresa.getRazonSocial()))
        .append("</h1>");
    sb.append("<div style=\"margin-top:12px;font-size:15px;\">Nº <strong>")
        .append(escape(cotizacion.getNumero()))
        .append("</strong> · ")
        .append(cotizacion.getFechaEmision().format(FECHA))
        .append("</div></div>");

    sb.append("<div style=\"padding:24px 32px;\">");
    if (cotizacion.getIntroduccionHtml() != null && !cotizacion.getIntroduccionHtml().isBlank()) {
      sb.append("<div style=\"margin-bottom:20px;font-size:14px;line-height:1.6;\">")
          .append(cotizacion.getIntroduccionHtml())
          .append("</div>");
    }
    if (mensajeAdicional != null && !mensajeAdicional.isBlank()) {
      sb.append("<p style=\"margin:0 0 20px;padding:12px 14px;background:#f8fafc;border-left:4px solid ")
          .append(acento)
          .append(";font-size:14px;line-height:1.5;\">")
          .append(escape(mensajeAdicional))
          .append("</p>");
    }

    sb.append("<table style=\"width:100%;margin-bottom:20px;font-size:14px;\"><tr><td style=\"vertical-align:top;width:50%;\">");
    sb.append("<div style=\"font-size:11px;color:#64748b;text-transform:uppercase;font-weight:700;\">Cliente</div>");
    sb.append("<div style=\"font-weight:700;\">").append(escape(cotizacion.getRazonSocialReceptor())).append("</div>");
    sb.append("<div>").append(escape(cotizacion.getIdentificacionReceptor())).append("</div>");
    if (cotizacion.getEmailReceptor() != null) {
      sb.append("<div>").append(escape(cotizacion.getEmailReceptor())).append("</div>");
    }
    sb.append("</td><td style=\"vertical-align:top;\">");
    if (mostrarVendedor && cotizacion.getVendedor() != null) {
      Vendedor v = cotizacion.getVendedor();
      sb.append("<div style=\"font-size:11px;color:#64748b;text-transform:uppercase;font-weight:700;\">Vendedor</div>");
      sb.append("<div style=\"font-weight:700;\">").append(escape(nombreVendedor(v))).append("</div>");
    }
    sb.append("<div style=\"margin-top:8px;font-size:12px;color:#64748b;\">Válida hasta ")
        .append(cotizacion.getFechaEmision().plusDays(cotizacion.getValidezDias()).format(FECHA))
        .append("</div></td></tr></table>");

    sb.append("<table style=\"width:100%;border-collapse:collapse;font-size:13px;\">");
    sb.append("<thead><tr style=\"background:")
        .append(primario)
        .append(";color:#fff;\">");
    sb.append("<th style=\"padding:10px 8px;text-align:left;\">Descripción</th>");
    sb.append("<th style=\"padding:10px 8px;text-align:right;\">Cant.</th>");
    sb.append("<th style=\"padding:10px 8px;text-align:right;\">P.Unit.</th>");
    sb.append("<th style=\"padding:10px 8px;text-align:right;\">Total</th></tr></thead><tbody>");
    for (CotizacionDetalle d : detalles) {
      sb.append("<tr style=\"border-bottom:1px solid #e2e8f0;\">");
      sb.append("<td style=\"padding:10px 8px;\">").append(escape(d.getDescripcion())).append("</td>");
      sb.append("<td style=\"padding:10px 8px;text-align:right;\">").append(d.getCantidad()).append("</td>");
      sb.append("<td style=\"padding:10px 8px;text-align:right;\">").append(money(d.getPrecioUnitario())).append("</td>");
      sb.append("<td style=\"padding:10px 8px;text-align:right;font-weight:600;\">")
          .append(money(d.getPrecioTotalSinImpuesto()))
          .append("</td></tr>");
    }
    sb.append("</tbody></table>");

    sb.append("<div style=\"margin-top:20px;text-align:right;font-size:14px;\">");
    sb.append("<div>Subtotal: <strong>").append(money(cotizacion.getSubtotalSinImpuestos())).append("</strong></div>");
    if (nz(cotizacion.getIvaTotal()).signum() > 0) {
      sb.append("<div>IVA: <strong>").append(money(cotizacion.getIvaTotal())).append("</strong></div>");
    }
    sb.append("<div style=\"margin-top:8px;font-size:18px;color:")
        .append(primario)
        .append(";\">Total: <strong>")
        .append(money(cotizacion.getValorTotal()))
        .append("</strong></div></div>");

    if (cotizacion.getCondicionesHtml() != null && !cotizacion.getCondicionesHtml().isBlank()) {
      sb.append("<div style=\"margin-top:24px;padding-top:16px;border-top:1px solid #e2e8f0;font-size:13px;line-height:1.6;\">")
          .append("<div style=\"font-weight:700;margin-bottom:8px;\">Condiciones</div>")
          .append(cotizacion.getCondicionesHtml())
          .append("</div>");
    }

    if (!adjuntos.isEmpty()) {
      sb.append("<div style=\"margin-top:20px;\"><div style=\"font-weight:700;font-size:13px;margin-bottom:8px;\">Enlaces adicionales</div><ul style=\"margin:0;padding-left:18px;font-size:13px;\">");
      for (CotizacionAdjunto a : adjuntos) {
        String label = a.getTitulo() != null && !a.getTitulo().isBlank() ? a.getTitulo() : a.getUrl();
        sb.append("<li style=\"margin-bottom:6px;\"><a href=\"")
            .append(escapeAttr(a.getUrl()))
            .append("\" style=\"color:")
            .append(primario)
            .append(";\">")
            .append(escape(label))
            .append("</a>");
        if (a.getProveedor() != null) {
          sb.append(" <span style=\"color:#94a3b8;font-size:11px;\">(")
              .append(escape(a.getProveedor()))
              .append(")</span>");
        }
        sb.append("</li>");
      }
      sb.append("</ul></div>");
    }

    sb.append("<p style=\"margin-top:28px;font-size:12px;color:#64748b;text-align:center;\">")
        .append(escape(pie))
        .append("</p>");
    sb.append("</div></div></body></html>");
    return sb.toString();
  }

  private static String nombreVendedor(Vendedor v) {
    String n = v.getNombres() != null ? v.getNombres().trim() : "";
    String a = v.getApellidos() != null ? v.getApellidos().trim() : "";
    return (n + " " + a).trim();
  }

  private static BigDecimal nz(BigDecimal v) {
    return v == null ? BigDecimal.ZERO : v;
  }

  private static String money(BigDecimal v) {
    if (v == null) {
      return "$0.00";
    }
    return String.format(Locale.US, "$%,.2f", v);
  }

  private static String escape(String s) {
    if (s == null) {
      return "";
    }
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
  }

  private static String escapeAttr(String s) {
    return escape(s);
  }
}
