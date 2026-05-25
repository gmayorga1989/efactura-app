package ec.tusaas.efactura.emision.ride;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

final class RideFechaFormat {

  private static final ZoneId ECUADOR = ZoneId.of("America/Guayaquil");
  private static final DateTimeFormatter DD_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final DateTimeFormatter DD_MM_YYYY_HH_MM =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ECUADOR);

  private RideFechaFormat() {}

  static String fecha(LocalDate fecha) {
    if (fecha == null) {
      return "-";
    }
    return DD_MM_YYYY.format(fecha);
  }

  static String fechaHora(Instant instant) {
    if (instant == null) {
      return "-";
    }
    return DD_MM_YYYY_HH_MM.format(instant);
  }
}
