package ec.tusaas.efactura.dto.comprobante;

/** Mensaje estructurado devuelto por el SRI en recepción o autorización. */
public record SriMensajeHistorialDto(
    String identificador, String tipo, String mensaje, String informacionAdicional) {}
