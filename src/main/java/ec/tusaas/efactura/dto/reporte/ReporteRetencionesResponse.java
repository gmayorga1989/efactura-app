package ec.tusaas.efactura.dto.reporte;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;

public record ReporteRetencionesResponse(
    UUID empresaId,
    LocalDate desde,
    LocalDate hasta,
    long totalRetenciones,
    BigDecimal baseImponible,
    BigDecimal valorRetenido,
    List<EstadoSriConteo> estados,
    Page<ReporteRetencionResponse> retenciones) {}
