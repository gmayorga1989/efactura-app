package ec.tusaas.efactura.web;

import ec.tusaas.efactura.dto.dashboard.DashboardHomeResponse;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.service.DashboardCacheService;
import ec.tusaas.efactura.service.DashboardService;
import ec.tusaas.efactura.web.support.EmpresaContextoResolver;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/web/v1/dashboard")
@RequiredArgsConstructor
@PreAuthorize(
    "hasAuthority('EMPRESA_ADMIN') or hasAuthority('FACTURA_EMITIR') or hasAuthority('VENTAS_GESTIONAR') "
        + "or hasAuthority('PROVEEDOR_GESTIONAR') or hasAuthority('REPORTE_VER') or hasAuthority('PLATFORM_ADMIN')")
public class DashboardController {

  private final EmpresaContextoResolver empresaContextoResolver;
  private final DashboardService dashboardService;
  private final DashboardCacheService dashboardCacheService;

  @GetMapping("/home")
  public DashboardHomeResponse home(
      @RequestParam(required = false) UUID empresaId,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return dashboardService.home(eid);
  }

  @PostMapping("/home/refresh")
  public DashboardHomeResponse refreshHome(
      @RequestParam(required = false) UUID empresaId,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    dashboardCacheService.evictEmpresa(eid);
    return dashboardService.home(eid);
  }
}
