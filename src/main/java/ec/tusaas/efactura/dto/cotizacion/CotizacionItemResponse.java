package ec.tusaas.efactura.dto.cotizacion;

import java.math.BigDecimal;
import java.util.UUID;

public record CotizacionItemResponse(
    UUID id,
    int linea,
    UUID productoId,
    String codigoPrincipal,
    String codigoAuxiliar,
    String descripcion,
    BigDecimal cantidad,
    BigDecimal precioUnitario,
    BigDecimal descuento,
    BigDecimal ivaPorcentaje,
    BigDecimal precioTotalSinImpuesto) {}
