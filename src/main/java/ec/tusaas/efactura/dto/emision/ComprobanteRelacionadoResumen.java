package ec.tusaas.efactura.dto.emision;

import java.time.Instant;
import java.util.UUID;

/** Comprobante emitido a partir de una factura (NC, ND, guía, etc.). */
public record ComprobanteRelacionadoResumen(
    UUID id,
    String tipo,
    String numeroComprobante,
    String estadoSri,
    Instant fechaAutorizacion) {}
