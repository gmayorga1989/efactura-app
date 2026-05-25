package ec.tusaas.efactura.dto.ride;



import java.util.HashMap;

import java.util.Map;



/** Personalización visual del RIDE (representación impresa). */

public record RidePlantillaDto(

    String colorPrimario,

    String colorAcento,

    String colorTexto,

    String colorFondoEncabezado,

    boolean mostrarLogo,

    boolean mostrarCodigoBarras,

    boolean mostrarNombreComercial,

    String textoPie,

    String densidad,

    /** sri_clasico | moderno | ejecutivo */

    String disenoBase,

    /** izquierda | centro | derecha */

    String ubicacionLogo,

    /** bajo_encabezado | caja_factura */

    String ubicacionClave,

    boolean mostrarBordes,

    boolean marcaSinLogo,

    /** en_descripcion | columna */

    String detalleAdicionalModo,

    boolean mostrarDesgloseIva,

    /** Color del texto en la caja de factura (clave, autorización). Vacío = automático según diseño. */
    String colorTextoCajaFactura,

    /** Filas visibles en totales (ver RideTotalesOpciones). Vacío = conjunto por defecto. */
    java.util.List<String> filasTotales) {



  /** Plantillas por tipo de comprobante (FACTURA, NOTA_CREDITO, …). */

  public static final String PLANTILLAS_KEY = "ridePlantillas";



  /** Clave legada (una sola plantilla para todos). */

  public static final String CONFIG_KEY = "ridePlantilla";



  public static RidePlantillaDto porDefecto() {

    return new RidePlantillaDto(

        "#1e5b96",

        "#0d9488",

        "#1f2937",

        "#eef5fb",

        true,

        true,

        true,

        "",

        "normal",

        "moderno",

        "izquierda",

        "bajo_encabezado",

        false,

        true,

        "en_descripcion",

        true,
        "",
        null);

  }



  public static RidePlantillaDto presetSriClasico() {

    return new RidePlantillaDto(

        "#000000",

        "#333333",

        "#111111",

        "#ffffff",

        true,

        true,

        true,

        "",

        "normal",

        "sri_clasico",

        "izquierda",

        "caja_factura",

        true,

        true,

        "en_descripcion",

        true,
        "",
        null);

  }



  public static RidePlantillaDto presetEjecutivo() {

    return new RidePlantillaDto(

        "#1e3a5f",

        "#64748b",

        "#1f2937",

        "#f8fafc",

        true,

        true,

        true,

        "",

        "normal",

        "ejecutivo",

        "centro",

        "bajo_encabezado",

        false,

        true,

        "en_descripcion",

        true,
        "#1f2937",
        null);

  }



  @SuppressWarnings("unchecked")

  public static RidePlantillaDto fromMap(Object raw) {

    if (!(raw instanceof Map<?, ?> map)) {

      return porDefecto();

    }

    RidePlantillaDto d = porDefecto();

    return new RidePlantillaDto(

        color(map.get("colorPrimario"), d.colorPrimario()),

        color(map.get("colorAcento"), d.colorAcento()),

        color(map.get("colorTexto"), d.colorTexto()),

        color(map.get("colorFondoEncabezado"), d.colorFondoEncabezado()),

        bool(map.get("mostrarLogo"), d.mostrarLogo()),

        bool(map.get("mostrarCodigoBarras"), d.mostrarCodigoBarras()),

        bool(map.get("mostrarNombreComercial"), d.mostrarNombreComercial()),

        str(map.get("textoPie"), d.textoPie()),

        densidad(map.get("densidad"), d.densidad()),

        disenoBase(map.get("disenoBase"), d.disenoBase()),

        ubicacionLogo(map.get("ubicacionLogo"), d.ubicacionLogo()),

        ubicacionClave(map.get("ubicacionClave"), d.ubicacionClave()),

        bool(map.get("mostrarBordes"), d.mostrarBordes()),

        bool(map.get("marcaSinLogo"), d.marcaSinLogo()),

        detalleAdicionalModo(map.get("detalleAdicionalModo"), d.detalleAdicionalModo()),

        bool(map.get("mostrarDesgloseIva"), d.mostrarDesgloseIva()),
        color(map.get("colorTextoCajaFactura"), d.colorTextoCajaFactura()),
        filasTotales(map.get("filasTotales"), d.filasTotales()));

  }



  public Map<String, Object> toMap() {

    Map<String, Object> m = new HashMap<>();

    m.put("colorPrimario", colorPrimario);

    m.put("colorAcento", colorAcento);

    m.put("colorTexto", colorTexto);

    m.put("colorFondoEncabezado", colorFondoEncabezado);

    m.put("mostrarLogo", mostrarLogo);

    m.put("mostrarCodigoBarras", mostrarCodigoBarras);

    m.put("mostrarNombreComercial", mostrarNombreComercial);

    m.put("textoPie", textoPie == null ? "" : textoPie);

    m.put("densidad", densidad == null ? "normal" : densidad);

    m.put("disenoBase", disenoBase == null ? "moderno" : disenoBase);

    m.put("ubicacionLogo", ubicacionLogo == null ? "izquierda" : ubicacionLogo);

    m.put("ubicacionClave", ubicacionClave == null ? "bajo_encabezado" : ubicacionClave);

    m.put("mostrarBordes", mostrarBordes);

    m.put("marcaSinLogo", marcaSinLogo);

    m.put("detalleAdicionalModo", detalleAdicionalModo == null ? "en_descripcion" : detalleAdicionalModo);

    m.put("mostrarDesgloseIva", mostrarDesgloseIva);
    m.put("colorTextoCajaFactura", colorTextoCajaFactura == null ? "" : colorTextoCajaFactura);
    if (filasTotales != null && !filasTotales.isEmpty()) {
      m.put("filasTotales", filasTotales);
    }

    return m;

  }

  @SuppressWarnings("unchecked")
  private static java.util.List<String> filasTotales(Object raw, java.util.List<String> fallback) {
    if (!(raw instanceof java.util.List<?> list) || list.isEmpty()) {
      return fallback;
    }
    java.util.List<String> out = new java.util.ArrayList<>();
    for (Object item : list) {
      if (item != null && !String.valueOf(item).isBlank()) {
        out.add(String.valueOf(item).trim().toUpperCase());
      }
    }
    return out.isEmpty() ? fallback : out;
  }



  private static String disenoBase(Object v, String fallback) {

    String s = str(v, fallback).toLowerCase();

    return switch (s) {

      case "sri_clasico", "sri", "clasico" -> "sri_clasico";

      case "ejecutivo", "executive" -> "ejecutivo";

      default -> "moderno";

    };

  }



  private static String ubicacionLogo(Object v, String fallback) {

    String s = str(v, fallback).toLowerCase();

    return switch (s) {

      case "centro", "center" -> "centro";

      case "derecha", "right" -> "derecha";

      default -> "izquierda";

    };

  }



  private static String ubicacionClave(Object v, String fallback) {

    String s = str(v, fallback).toLowerCase();

    return "caja_factura".equals(s) || "caja".equals(s) ? "caja_factura" : "bajo_encabezado";

  }



  private static String color(Object v, String fallback) {

    String s = str(v, fallback);

    if (!s.matches("^#[0-9A-Fa-f]{6}$")) {

      return fallback;

    }

    return s.toLowerCase();

  }



  private static String densidad(Object v, String fallback) {

    String s = str(v, fallback);

    return "compact".equalsIgnoreCase(s) ? "compact" : "normal";

  }



  private static String detalleAdicionalModo(Object v, String fallback) {

    String s = str(v, fallback).toLowerCase();

    return "columna".equals(s) || "columna_separada".equals(s) ? "columna" : "en_descripcion";

  }



  private static boolean bool(Object v, boolean fallback) {

    if (v instanceof Boolean b) {

      return b;

    }

    if (v != null) {

      return Boolean.parseBoolean(String.valueOf(v));

    }

    return fallback;

  }



  private static String str(Object v, String fallback) {

    if (v == null) {

      return fallback;

    }

    String s = String.valueOf(v).trim();

    return s.isEmpty() ? fallback : s;

  }

}

