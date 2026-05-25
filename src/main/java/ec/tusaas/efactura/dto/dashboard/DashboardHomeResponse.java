package ec.tusaas.efactura.dto.dashboard;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DashboardHomeResponse(
    UUID empresaId,
    LocalDate desde,
    LocalDate hasta,
    Instant generadoEn,
    DashboardEmpresaEstadoResponse empresa,
    List<DashboardKpiResponse> kpis,
    List<DashboardEstadoResponse> comprobantesPorEstado,
    List<DashboardTipoEstadoResponse> comprobantesPorTipoEstado,
    List<DashboardSerieDiaResponse> ventasPorDia,
    List<DashboardComprobanteRecienteResponse> comprobantesRecientes,
    DashboardMaestrosResponse maestros,
    boolean cacheable) {}
