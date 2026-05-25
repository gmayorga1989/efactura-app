package ec.tusaas.efactura.dto.emision;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record ComprobanteDetalleResponse(
    UUID id,
    int linea,
    String codigoPrincipal,
    String codigoAuxiliar,
    String descripcion,
    BigDecimal cantidad,
    BigDecimal precioUnitario,
    BigDecimal descuento,
    BigDecimal precioTotalSinImpuesto,
    Map<String, Object> customData) {}
