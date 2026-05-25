package ec.tusaas.efactura.dto.reporte;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReporteDocumentoResponse(
    UUID id,
    String tipo,
    String tipoCodigo,
    String numero,
    String claveAcceso,
    LocalDate fechaEmision,
    String identificacionReceptor,
    String razonSocialReceptor,
    BigDecimal subtotalSinImpuestos,
    BigDecimal descuentoTotal,
    BigDecimal ivaTotal,
    BigDecimal valorTotal,
    String estadoSri,
    String numeroAutorizacion,
    Instant fechaAutorizacion,
    String origen) {}
