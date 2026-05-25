package ec.tusaas.efactura.emision;

import ec.tusaas.efactura.entity.Comprobante;
import java.time.LocalDate;
import java.util.Map;

/** Datos de factura/documento sustento para RIDE de notas de crédito y débito. */
public final class DocumentoModificadoRideUtil {

  private static final String COD_DOC_FACTURA = "01";

  private DocumentoModificadoRideUtil() {}

  public record DatosDocumentoModificado(
      String tipoComprobanteModificado, String numeroComprobante, String fechaEmisionModificado, String motivo) {}

  /** Persiste en customData los campos necesarios para el RIDE impreso. */
  public static void enriquecerCustomDataDesdeFactura(Map<String, Object> cd, Comprobante factura) {
    if (cd == null || factura == null) {
      return;
    }
    String numero =
        factura.getEstablecimientoCodigo()
            + "-"
            + factura.getPuntoEmisionCodigo()
            + "-"
            + factura.getSecuencial();
    cd.put("numeroFactura", numero);
    cd.put("codDocModificado", COD_DOC_FACTURA);
    cd.put("tipoComprobanteModificado", "FACTURA");
    if (factura.getFechaEmision() != null) {
      cd.put("fechaEmisionFacturaModificada", factura.getFechaEmision().toString());
    }
    if (factura.getClaveAcceso() != null && !factura.getClaveAcceso().isBlank()) {
      cd.put("claveAccesoFactura", factura.getClaveAcceso());
    }
  }

  public static boolean esNotaCreditoODebito(Comprobante comprobante) {
    if (comprobante == null || comprobante.getTipo() == null) {
      return false;
    }
    String t = comprobante.getTipo().trim().toUpperCase();
    return "NOTA_CREDITO".equals(t) || "NOTA_DEBITO".equals(t);
  }

  public static DatosDocumentoModificado leerParaRide(Comprobante comprobante) {
    if (!esNotaCreditoODebito(comprobante)) {
      return null;
    }
    Map<String, Object> cd = comprobante.getCustomData();
    if (cd == null || cd.isEmpty()) {
      return null;
    }
    String tipo = texto(cd.get("tipoComprobanteModificado"));
    if (tipo.isBlank()) {
      tipo = "FACTURA";
    }
    String numero = texto(cd.get("numeroFactura"));
    if (numero.isBlank()) {
      numero = texto(cd.get("numDocModificado"));
    }
    String fecha = texto(cd.get("fechaEmisionFacturaModificada"));
    if (fecha.isBlank()) {
      fecha = texto(cd.get("fechaEmisionDocSustento"));
    }
    String motivo = texto(cd.get("motivo"));
    if (numero.isBlank() && fecha.isBlank() && motivo.isBlank()) {
      return null;
    }
    return new DatosDocumentoModificado(tipo, numero, fecha, motivo);
  }

  public static LocalDate parseFechaModificada(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String s = raw.trim();
    try {
      if (s.length() >= 10 && s.charAt(4) == '-') {
        return LocalDate.parse(s.substring(0, 10));
      }
      if (s.contains("/")) {
        String[] p = s.split("/");
        if (p.length == 3) {
          return LocalDate.of(Integer.parseInt(p[2]), Integer.parseInt(p[1]), Integer.parseInt(p[0]));
        }
      }
    } catch (Exception ignored) {
      return null;
    }
    return null;
  }

  private static String texto(Object o) {
    if (o == null) {
      return "";
    }
    return String.valueOf(o).trim();
  }
}
