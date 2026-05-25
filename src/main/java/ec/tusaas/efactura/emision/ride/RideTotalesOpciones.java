package ec.tusaas.efactura.emision.ride;

import ec.tusaas.efactura.dto.ride.RidePlantillaDto;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Claves configurables de filas en el bloque de totales del RIDE. */
public final class RideTotalesOpciones {

  public static final String SUBTOTAL_SIN_IMPUESTOS = "SUBTOTAL_SIN_IMPUESTOS";
  public static final String DESCUENTO = "DESCUENTO";
  public static final String SUBTOTAL_0 = "SUBTOTAL_0";
  public static final String SUBTOTAL_GRAVADO = "SUBTOTAL_GRAVADO";
  public static final String SUBTOTAL_EXENTO = "SUBTOTAL_EXENTO";
  public static final String SUBTOTAL_NO_OBJETO = "SUBTOTAL_NO_OBJETO";
  public static final String SUBTOTAL_TARIFA_5 = "SUBTOTAL_TARIFA_5";
  public static final String SUBTOTAL_TARIFA_15 = "SUBTOTAL_TARIFA_15";
  public static final String ICE = "ICE";
  public static final String IRBPNR = "IRBPNR";
  public static final String IVA = "IVA";
  public static final String IVA_DESGLOSE = "IVA_DESGLOSE";
  public static final String PROPINA = "PROPINA";
  public static final String VALOR_TOTAL = "VALOR_TOTAL";

  public static final List<String> TODAS =
      List.of(
          SUBTOTAL_SIN_IMPUESTOS,
          DESCUENTO,
          SUBTOTAL_0,
          SUBTOTAL_GRAVADO,
          SUBTOTAL_EXENTO,
          SUBTOTAL_NO_OBJETO,
          SUBTOTAL_TARIFA_5,
          SUBTOTAL_TARIFA_15,
          ICE,
          IRBPNR,
          IVA,
          IVA_DESGLOSE,
          PROPINA,
          VALOR_TOTAL);

  private RideTotalesOpciones() {}

  public static List<String> filasActivas(RidePlantillaDto plantilla) {
    if (plantilla == null || plantilla.filasTotales() == null || plantilla.filasTotales().isEmpty()) {
      return porDefecto(plantilla);
    }
    Set<String> orden = new LinkedHashSet<>();
    for (Object raw : plantilla.filasTotales()) {
      if (raw == null) {
        continue;
      }
      String key = String.valueOf(raw).trim().toUpperCase();
      if (TODAS.contains(key)) {
        orden.add(key);
      }
    }
    if (!orden.contains(VALOR_TOTAL)) {
      orden.add(VALOR_TOTAL);
    }
    return List.copyOf(orden);
  }

  private static List<String> porDefecto(RidePlantillaDto plantilla) {
    List<String> out = new ArrayList<>();
    out.add(SUBTOTAL_SIN_IMPUESTOS);
    out.add(DESCUENTO);
    if (plantilla != null && plantilla.mostrarDesgloseIva()) {
      out.add(IVA_DESGLOSE);
    } else {
      out.add(IVA);
    }
    out.add(VALOR_TOTAL);
    return out;
  }

  @SuppressWarnings("unchecked")
  public static List<Map<String, Object>> leerDesgloseImpuestosMap(ec.tusaas.efactura.entity.Comprobante c) {
    Map<String, Object> cd = c.getCustomData();
    if (cd == null) {
      return List.of();
    }
    Object raw = cd.get("desgloseImpuestos");
    if (!(raw instanceof List<?> list)) {
      return List.of();
    }
    List<Map<String, Object>> out = new ArrayList<>();
    for (Object item : list) {
      if (item instanceof Map<?, ?> map) {
        out.add((Map<String, Object>) map);
      }
    }
    return out;
  }
}
