package ec.tusaas.efactura.dto.reporte;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;

public record ReporteGuiasResponse(
    UUID empresaId,
    LocalDate desde,
    LocalDate hasta,
    long totalGuias,
    List<EstadoSriConteo> estados,
    Page<ReporteGuiaResponse> guias) {}
