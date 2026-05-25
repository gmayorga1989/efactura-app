package ec.tusaas.efactura.dto.sridescarga;

import java.math.BigDecimal;

public record SriComprobanteResumenMensualResponse(
    int anio, int mes, long totalComprobantes, BigDecimal valorTotal) {}
