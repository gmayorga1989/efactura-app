package ec.tusaas.efactura.web;

import ec.tusaas.efactura.dto.reporte.EstadoSriConteo;
import ec.tusaas.efactura.dto.reporte.ReporteDocumentosResponse;
import ec.tusaas.efactura.dto.reporte.ReporteGuiasResponse;
import ec.tusaas.efactura.dto.reporte.ReporteRetencionesResponse;
import ec.tusaas.efactura.repository.ComprobanteRepository;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.service.ReporteService;
import ec.tusaas.efactura.web.support.EmpresaContextoResolver;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/web/v1/reportes")
@RequiredArgsConstructor
public class ReportesController {

  private final EmpresaContextoResolver empresaContextoResolver;
  private final ComprobanteRepository comprobanteRepository;
  private final ReporteService reporteService;

  @GetMapping("/comprobantes-por-estado")
  @PreAuthorize(
      "hasAuthority('REPORTE_VER') or hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN')")
  public List<EstadoSriConteo> comprobantesPorEstado(
      @RequestParam(required = false) UUID empresaId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return comprobanteRepository.contarPorEstadoSri(eid, desde, hasta);
  }

  @GetMapping("/ventas")
  @PreAuthorize(
      "hasAuthority('REPORTE_VER') or hasAuthority('VENTAS_GESTIONAR') or hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN')")
  public ReporteDocumentosResponse ventas(
      @RequestParam(required = false) UUID empresaId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
      @RequestParam(required = false) String estadoSri,
      @RequestParam(required = false) String identificacion,
      @RequestParam(required = false) String establecimiento,
      @RequestParam(required = false) String puntoEmision,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return reporteService.ventas(
        eid, desde, hasta, estadoSri, identificacion, establecimiento, puntoEmision, pageable(page, size));
  }

  @GetMapping("/liquidaciones")
  @PreAuthorize(
      "hasAuthority('REPORTE_VER') or hasAuthority('PROVEEDOR_GESTIONAR') or hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN')")
  public ReporteDocumentosResponse liquidaciones(
      @RequestParam(required = false) UUID empresaId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
      @RequestParam(required = false) String estadoSri,
      @RequestParam(required = false) String identificacion,
      @RequestParam(required = false) String establecimiento,
      @RequestParam(required = false) String puntoEmision,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return reporteService.liquidaciones(
        eid, desde, hasta, estadoSri, identificacion, establecimiento, puntoEmision, pageable(page, size));
  }

  @GetMapping("/guias")
  @PreAuthorize(
      "hasAuthority('REPORTE_VER') or hasAuthority('VENTAS_GESTIONAR') or hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN')")
  public ReporteGuiasResponse guias(
      @RequestParam(required = false) UUID empresaId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
      @RequestParam(required = false) String estadoSri,
      @RequestParam(required = false) String identificacion,
      @RequestParam(required = false) String establecimiento,
      @RequestParam(required = false) String puntoEmision,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return reporteService.guias(
        eid, desde, hasta, estadoSri, identificacion, establecimiento, puntoEmision, pageable(page, size));
  }

  @GetMapping("/retenciones")
  @PreAuthorize(
      "hasAuthority('REPORTE_VER') or hasAuthority('PROVEEDOR_GESTIONAR') or hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN')")
  public ReporteRetencionesResponse retenciones(
      @RequestParam(required = false) UUID empresaId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
      @RequestParam(required = false) String estadoSri,
      @RequestParam(required = false) String identificacion,
      @RequestParam(required = false) String establecimiento,
      @RequestParam(required = false) String puntoEmision,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return reporteService.retenciones(
        eid, desde, hasta, estadoSri, identificacion, establecimiento, puntoEmision, pageable(page, size));
  }

  private static Pageable pageable(int page, int size) {
    return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200));
  }
}
