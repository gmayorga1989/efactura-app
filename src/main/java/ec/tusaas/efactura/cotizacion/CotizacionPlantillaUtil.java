package ec.tusaas.efactura.cotizacion;

import java.util.HashMap;
import java.util.Map;

/** Valores por defecto del diseño de cotización / proforma (JSON en {@code cotizacion.plantilla_json}). */
public final class CotizacionPlantillaUtil {

  public static final String CONFIG_EMPRESA_KEY = "cotizacionPlantilla";

  private CotizacionPlantillaUtil() {}

  public static Map<String, Object> porDefecto() {
    Map<String, Object> m = new HashMap<>();
    m.put("colorPrimario", "#1e5b96");
    m.put("colorAcento", "#0ea5e9");
    m.put("colorTexto", "#0f172a");
    m.put("colorFondoEncabezado", "#f8fafc");
    m.put("disenoBase", "moderno");
    m.put("mostrarLogo", true);
    m.put("mostrarVendedor", true);
    m.put("mostrarValidez", true);
    m.put("densidad", "normal");
    m.put("textoPie", "Gracias por su preferencia.");
    m.put("fontFamily", "Inter, Segoe UI, sans-serif");
    m.put("mostrarBordes", true);
    m.put("bannerImageUrl", "");
    return m;
  }

  public static Map<String, Object> merge(Map<String, Object> base, Map<String, Object> override) {
    Map<String, Object> out = new HashMap<>(base == null ? porDefecto() : base);
    if (override != null) {
      override.forEach((k, v) -> {
        if (v != null) {
          out.put(k, v);
        }
      });
    }
    return out;
  }

  public static String str(Map<String, Object> m, String key, String fallback) {
    if (m == null) {
      return fallback;
    }
    Object v = m.get(key);
    return v == null ? fallback : String.valueOf(v);
  }

  public static boolean bool(Map<String, Object> m, String key, boolean fallback) {
    if (m == null) {
      return fallback;
    }
    Object v = m.get(key);
    if (v instanceof Boolean b) {
      return b;
    }
    if (v instanceof String s) {
      return Boolean.parseBoolean(s);
    }
    return fallback;
  }
}
