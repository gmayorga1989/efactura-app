package ec.tusaas.efactura.dto.emision;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ComprobanteResponse(
    UUID id,
    UUID empresaId,
    String tipo,
    String tipoCodigo,
    String numeroComprobante,
    String claveAcceso,
    LocalDate fechaEmision,
    String razonSocialReceptor,
    String identificacionReceptor,
    BigDecimal subtotalSinImpuestos,
    BigDecimal descuentoTotal,
    BigDecimal ivaTotal,
    BigDecimal valorTotal,
    String estadoSri,
    String numeroAutorizacion,
    Instant fechaAutorizacion,
    /** Último mensaje del SRI (recepción/autorización), útil en pendiente o error. */
    String ultimoMensajeSri,
    UUID vendedorId,
    String vendedorNombre,
    Map<String, Object> customData,
    List<ComprobanteDetalleResponse> detalles) {}
