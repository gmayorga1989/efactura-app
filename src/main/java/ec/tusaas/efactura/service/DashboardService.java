package ec.tusaas.efactura.service;

import ec.tusaas.efactura.config.CacheConfig;
import ec.tusaas.efactura.dto.dashboard.DashboardComprobanteRecienteResponse;
import ec.tusaas.efactura.dto.dashboard.DashboardEmpresaEstadoResponse;
import ec.tusaas.efactura.dto.dashboard.DashboardEstadoResponse;
import ec.tusaas.efactura.dto.dashboard.DashboardHomeResponse;
import ec.tusaas.efactura.dto.dashboard.DashboardKpiResponse;
import ec.tusaas.efactura.dto.dashboard.DashboardMaestrosResponse;
import ec.tusaas.efactura.dto.dashboard.DashboardTipoEstadoResponse;
import ec.tusaas.efactura.entity.Comprobante;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.repository.CertificadoRepository;
import ec.tusaas.efactura.repository.ClienteRepository;
import ec.tusaas.efactura.repository.ComprobanteRepository;
import ec.tusaas.efactura.repository.EmpresaRepository;
import ec.tusaas.efactura.repository.EstablecimientoRepository;
import ec.tusaas.efactura.repository.ProductoRepository;
import ec.tusaas.efactura.repository.PuntoEmisionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class DashboardService {

  private static final String TIPO_FACTURA = "FACTURA";

  private final EmpresaRepository empresaRepository;
  private final ComprobanteRepository comprobanteRepository;
  private final ClienteRepository clienteRepository;
  private final ProductoRepository productoRepository;
  private final EstablecimientoRepository establecimientoRepository;
  private final PuntoEmisionRepository puntoEmisionRepository;
  private final CertificadoRepository certificadoRepository;

  @Transactional(readOnly = true)
  @Cacheable(value = CacheConfig.DASHBOARD_CACHE, key = "#empresaId")
  public DashboardHomeResponse home(UUID empresaId) {
    LocalDate hasta = LocalDate.now();
    LocalDate desde = hasta.withDayOfMonth(1);
    Empresa empresa =
        empresaRepository
            .findById(empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));

    BigDecimal ventasMes = safe(comprobanteRepository.sumarTotalPorTipo(empresaId, desde, hasta, TIPO_FACTURA));
    long documentosMes =
        comprobanteRepository.countByEmpresa_IdAndEstadoNotAndFechaEmisionBetween(
            empresaId, "ELIMINADO", desde, hasta);
    long autorizadosMes =
        comprobanteRepository.countByEmpresa_IdAndEstadoNotAndFechaEmisionBetweenAndEstadoSri(
            empresaId, "ELIMINADO", desde, hasta, "AUTORIZADO");
    BigDecimal autorizacionPct =
        documentosMes == 0
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(autorizadosMes * 100.0 / documentosMes).setScale(2, RoundingMode.HALF_UP);

    DashboardMaestrosResponse maestros = maestros(empresaId);
    boolean certificadoActivo = certificadoRepository.findFirstByEmpresa_IdAndActivoParaFirmaIsTrue(empresaId).isPresent();
    long comprobantesMes =
        comprobanteRepository.countByEmpresa_IdAndFechaEmisionBetween(empresaId, desde, hasta);

    return new DashboardHomeResponse(
        empresaId,
        desde,
        hasta,
        Instant.now(),
        new DashboardEmpresaEstadoResponse(
            empresa.getId(),
            empresa.getRazonSocial(),
            empresa.getNombreComercial(),
            empresa.getRuc(),
            empresa.getEstado(),
            empresa.getAmbienteSri(),
            empresa.getLogoUrl(),
            empresa.getPlanCodigo(),
            empresa.getPlanLimiteMes(),
            comprobantesMes,
            certificadoActivo,
            configuracionBasicaCompleta(empresa, certificadoActivo, maestros)),
        List.of(
            new DashboardKpiResponse(
                "ventasMes", "dashboard.kpi.monthSales", "Ventas del mes", ventasMes, "currency", "neutral", "trending-up"),
            new DashboardKpiResponse(
                "documentosMes",
                "dashboard.kpi.monthDocuments",
                "Documentos del mes",
                BigDecimal.valueOf(documentosMes),
                "count",
                "neutral",
                "file-text"),
            new DashboardKpiResponse(
                "autorizacionPct",
                "dashboard.kpi.authorizationRate",
                "Tasa de autorizacion",
                autorizacionPct,
                "percent",
                autorizacionPct.compareTo(BigDecimal.valueOf(90)) >= 0 ? "success" : "warning",
                "badge-check"),
            new DashboardKpiResponse(
                "planUsoMes",
                "dashboard.kpi.planUsage",
                "Uso del plan",
                empresa.getPlanLimiteMes() == null
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(comprobantesMes * 100.0 / Math.max(empresa.getPlanLimiteMes(), 1))
                        .setScale(2, RoundingMode.HALF_UP),
                empresa.getPlanLimiteMes() == null ? "unlimited" : "percent",
                "neutral",
                "gauge")),
        comprobanteRepository.contarPorEstadoSri(empresaId, desde, hasta).stream()
            .map(it -> new DashboardEstadoResponse(it.estadoSri(), it.total()))
            .toList(),
        comprobanteRepository.contarPorTipoYEstado(empresaId, desde, hasta, "").stream()
            .map(it -> new DashboardTipoEstadoResponse(it.tipoComprobante(), it.estadoSri(), it.total()))
            .toList(),
        comprobanteRepository.ventasPorDia(empresaId, desde, hasta, TIPO_FACTURA),
        comprobanteRepository
            .findTop5ByEmpresa_IdAndEstadoNotOrderByFechaEmisionDescFechaCreacionDesc(empresaId, "ELIMINADO")
            .stream()
            .map(DashboardService::reciente)
            .toList(),
        maestros,
        true);
  }

  private DashboardMaestrosResponse maestros(UUID empresaId) {
    long productos = productoRepository.countByEmpresa_IdAndEstadoNotAndTipoIgnoreCase(empresaId, "ELIMINADO", "PRODUCTO");
    long servicios = productoRepository.countByEmpresa_IdAndEstadoNotAndTipoIgnoreCase(empresaId, "ELIMINADO", "SERVICIO");
    long totalProductos = productoRepository.countByEmpresa_IdAndEstadoNot(empresaId, "ELIMINADO");
    return new DashboardMaestrosResponse(
        clienteRepository.countByEmpresaAndTipoTerceroIn(empresaId, List.of("CLIENTE", "AMBOS")),
        clienteRepository.countByEmpresaAndTipoTerceroIn(empresaId, List.of("PROVEEDOR", "AMBOS")),
        productos == 0 && servicios == 0 ? totalProductos : productos,
        servicios,
        establecimientoRepository.countByEmpresa_IdAndEstado(empresaId, "ACTIVO"),
        puntoEmisionRepository.countByEmpresa_IdAndEstado(empresaId, "ACTIVO"));
  }

  private static DashboardComprobanteRecienteResponse reciente(Comprobante c) {
    return new DashboardComprobanteRecienteResponse(
        c.getId(),
        c.getTipo(),
        c.getEstablecimientoCodigo() + "-" + c.getPuntoEmisionCodigo() + "-" + c.getSecuencial(),
        c.getFechaEmision(),
        c.getRazonSocialReceptor(),
        c.getIdentificacionReceptor(),
        c.getValorTotal(),
        c.getEstadoSri());
  }

  private static boolean configuracionBasicaCompleta(
      Empresa empresa, boolean certificadoActivo, DashboardMaestrosResponse maestros) {
    return hasText(empresa.getRuc())
        && hasText(empresa.getRazonSocial())
        && hasText(empresa.getDireccionMatriz())
        && certificadoActivo
        && maestros.establecimientos() > 0
        && maestros.puntosEmision() > 0;
  }

  private static BigDecimal safe(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
