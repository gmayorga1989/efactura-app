package ec.tusaas.efactura.emision;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Utilidades para datos del receptor en customData de comprobantes. */
public final class ComprobanteEmailUtil {

  private ComprobanteEmailUtil() {}

  /** Copia correo y dirección del receptor desde el comprobante origen si faltan. */
  public static void copiarDatosReceptorSiAusente(Map<String, Object> destino, Map<String, Object> origen) {
    copiarEmailReceptorSiAusente(destino, origen);
    copiarDireccionReceptorSiAusente(destino, origen);
  }

  /** Copia emailReceptor / emailsReceptor del origen si el destino no los tiene. */
  public static void copiarEmailReceptorSiAusente(Map<String, Object> destino, Map<String, Object> origen) {
    if (destino == null || origen == null || tieneEmail(destino)) {
      return;
    }
    Object email = origen.get("emailReceptor");
    if (email != null && !String.valueOf(email).isBlank()) {
      destino.put("emailReceptor", String.valueOf(email).trim());
    }
    Object emails = origen.get("emailsReceptor");
    if (emails instanceof List<?> list && !list.isEmpty()) {
      List<String> normalizados = new ArrayList<>();
      for (Object item : list) {
        if (item != null && !String.valueOf(item).isBlank()) {
          normalizados.add(String.valueOf(item).trim());
        }
      }
      if (!normalizados.isEmpty()) {
        destino.put("emailsReceptor", normalizados);
        if (!destino.containsKey("emailReceptor")) {
          destino.put("emailReceptor", normalizados.get(0));
        }
      }
    }
  }

  /** Actualiza emailReceptor (y lista emailsReceptor si hay varios separados por ; o ,). */
  public static void aplicarEmailReceptor(Map<String, Object> cd, String raw) {
    if (cd == null || raw == null || raw.isBlank()) {
      return;
    }
    List<String> correos = parseListaCorreos(raw);
    if (correos.isEmpty()) {
      cd.put("emailReceptor", raw.trim());
      return;
    }
    cd.put("emailReceptor", correos.get(0));
    if (correos.size() > 1) {
      cd.put("emailsReceptor", correos);
    }
  }

  /** Copia direccionReceptor / direccionComprador del origen si el destino no las tiene. */
  public static void copiarDireccionReceptorSiAusente(Map<String, Object> destino, Map<String, Object> origen) {
    if (destino == null || origen == null || tieneDireccion(destino)) {
      return;
    }
    Object dir = origen.get("direccionReceptor");
    if (dir == null) {
      dir = origen.get("direccionComprador");
    }
    if (dir != null && !String.valueOf(dir).isBlank()) {
      destino.put("direccionReceptor", String.valueOf(dir).trim());
    }
  }

  public static Map<String, Object> customDataConEmail(Map<String, Object> base, String raw) {
    Map<String, Object> cd = new HashMap<>(base != null ? base : Map.of());
    aplicarEmailReceptor(cd, raw);
    return cd;
  }

  private static boolean tieneDireccion(Map<String, Object> cd) {
    Object dir = cd.get("direccionReceptor");
    if (dir != null && !String.valueOf(dir).isBlank()) {
      return true;
    }
    Object alt = cd.get("direccionComprador");
    return alt != null && !String.valueOf(alt).isBlank();
  }

  private static boolean tieneEmail(Map<String, Object> cd) {
    Object email = cd.get("emailReceptor");
    if (email != null && !String.valueOf(email).isBlank()) {
      return true;
    }
    Object emails = cd.get("emailsReceptor");
    return emails instanceof List<?> list && !list.isEmpty();
  }

  private static List<String> parseListaCorreos(String raw) {
    List<String> out = new ArrayList<>();
    if (raw == null || raw.isBlank()) {
      return out;
    }
    for (String part : raw.split("[;,]")) {
      String mail = part.trim();
      if (!mail.isEmpty() && mail.contains("@")) {
        out.add(mail);
      }
    }
    return out;
  }
}
