package ec.tusaas.efactura.dto.reporte;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReporteRetencionLineaResponse(
    String codigo,
    String codigoRetencion,
    BigDecimal baseImponible,
    BigDecimal porcentaje,
    BigDecimal valor,
    String documentoSustentoTipo,
    String documentoSustentoNumero,
    LocalDate documentoSustentoFecha) {}
