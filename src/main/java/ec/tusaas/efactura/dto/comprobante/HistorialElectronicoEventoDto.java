package ec.tusaas.efactura.dto.comprobante;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Evento de trazabilidad del proceso electrónico de un comprobante. */
public record HistorialElectronicoEventoDto(
    UUID id,
    Instant fecha,
    String categoria,
    String operacion,
    String titulo,
    String mensaje,
    String resultado,
    Integer httpStatus,
    UUID comprobanteId,
    String numeroComprobante,
    String tipoComprobante,
    boolean expandible,
    String detalleSolicitud,
    String detalleRespuesta,
    List<SriMensajeHistorialDto> mensajesSri) {

  public HistorialElectronicoEventoDto(
      UUID id,
      Instant fecha,
      String categoria,
      String operacion,
      String titulo,
      String mensaje,
      String resultado,
      Integer httpStatus,
      UUID comprobanteId,
      String numeroComprobante,
      String tipoComprobante) {
    this(
        id,
        fecha,
        categoria,
        operacion,
        titulo,
        mensaje,
        resultado,
        httpStatus,
        comprobanteId,
        numeroComprobante,
        tipoComprobante,
        false,
        null,
        null,
        List.of());
  }
}
