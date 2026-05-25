package ec.tusaas.efactura.dto.reporte;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ReporteRetencionResponse(
    UUID comprobanteId,
    String numero,
    String claveAcceso,
    LocalDate fechaEmision,
    String estadoSri,
    String sujetoRetenido,
    String identificacionSujetoRetenido,
    BigDecimal baseImponible,
    BigDecimal valorRetenido,
    List<ReporteRetencionLineaResponse> lineas) {}
