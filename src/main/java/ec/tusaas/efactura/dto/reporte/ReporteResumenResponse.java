package ec.tusaas.efactura.dto.reporte;

import java.math.BigDecimal;

public record ReporteResumenResponse(
    long totalDocumentos,
    BigDecimal subtotal,
    BigDecimal descuentos,
    BigDecimal iva,
    BigDecimal total,
    BigDecimal totalFacturas,
    BigDecimal totalNotasCredito,
    BigDecimal totalNotasDebito,
    BigDecimal totalNeto) {}
