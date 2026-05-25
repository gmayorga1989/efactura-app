package ec.tusaas.efactura.dto.email;

import java.util.HashMap;
import java.util.Map;

/** Plantilla HTML para correos de comprobantes al cliente. */
public record EmailPlantillaDto(
    String disenoBase,
    String colorPrimario,
    String colorAcento,
    String asuntoPlantilla,
    String textoIntro,
    String textoPie,
    boolean mostrarLogo,
    boolean mostrarResumen) {

  public static final String CONFIG_KEY = "emailPlantilla";

  public static EmailPlantillaDto porDefecto() {
    return new EmailPlantillaDto(
        "moderno",
        "#0f766e",
        "#1e5b96",
        "{{tipo}} electrónico Nº {{numero}}",
        "",
        "",
        true,
        true);
  }

  public static EmailPlantillaDto presetCorporativo() {
    return new EmailPlantillaDto(
        "corporativo",
        "#1e3a5f",
        "#475569",
        "{{tipo}} electrónico Nº {{numero}}",
        "",
        "",
        true,
        true);
  }

  @SuppressWarnings("unchecked")
  public static EmailPlantillaDto fromMap(Object raw) {
    if (!(raw instanceof Map<?, ?> map)) {
      return porDefecto();
    }
    EmailPlantillaDto d = porDefecto();
    return new EmailPlantillaDto(
        diseno(map.get("disenoBase"), d.disenoBase()),
        color(map.get("colorPrimario"), d.colorPrimario()),
        color(map.get("colorAcento"), d.colorAcento()),
        str(map.get("asuntoPlantilla"), d.asuntoPlantilla()),
        str(map.get("textoIntro"), d.textoIntro()),
        str(map.get("textoPie"), d.textoPie()),
        bool(map.get("mostrarLogo"), d.mostrarLogo()),
        bool(map.get("mostrarResumen"), d.mostrarResumen()));
  }

  public Map<String, Object> toMap() {
    Map<String, Object> m = new HashMap<>();
    m.put("disenoBase", disenoBase == null ? "moderno" : disenoBase);
    m.put("colorPrimario", colorPrimario);
    m.put("colorAcento", colorAcento);
    m.put("asuntoPlantilla", asuntoPlantilla == null ? "" : asuntoPlantilla);
    m.put("textoIntro", textoIntro == null ? "" : textoIntro);
    m.put("textoPie", textoPie == null ? "" : textoPie);
    m.put("mostrarLogo", mostrarLogo);
    m.put("mostrarResumen", mostrarResumen);
    return m;
  }

  private static String diseno(Object v, String fallback) {
    String s = str(v, fallback).toLowerCase();
    return "corporativo".equals(s) ? "corporativo" : "moderno";
  }

  private static String color(Object v, String fallback) {
    String s = str(v, fallback);
    return s.matches("^#[0-9A-Fa-f]{6}$") ? s.toLowerCase() : fallback;
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
