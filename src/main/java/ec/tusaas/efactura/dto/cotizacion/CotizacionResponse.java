package ec.tusaas.efactura.dto.cotizacion;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CotizacionResponse(
    UUID id,
    String numero,
    LocalDate fechaEmision,
    int validezDias,
    LocalDate fechaVencimiento,
    String estado,
    UUID clienteId,
    UUID vendedorId,
    String vendedorNombre,
    String tipoIdentificacionReceptor,
    String identificacionReceptor,
    String razonSocialReceptor,
    String emailReceptor,
    String moneda,
    BigDecimal subtotalSinImpuestos,
    BigDecimal descuentoTotal,
    BigDecimal ivaTotal,
    BigDecimal valorTotal,
    String introduccionHtml,
    String condicionesHtml,
    Map<String, Object> plantillaJson,
    UUID comprobanteId,
    Instant fechaEnvio,
    Instant fechaConversion,
    List<CotizacionItemResponse> items,
    List<CotizacionAdjuntoResponse> adjuntos) {}
