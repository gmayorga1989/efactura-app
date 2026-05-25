package ec.tusaas.efactura.dto.reporte;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;

public record ReporteDocumentosResponse(
    UUID empresaId,
    LocalDate desde,
    LocalDate hasta,
    ReporteResumenResponse resumen,
    List<EstadoSriConteo> estados,
    List<ComprobanteTipoEstadoConteo> tiposEstado,
    Page<ReporteDocumentoResponse> documentos) {}
