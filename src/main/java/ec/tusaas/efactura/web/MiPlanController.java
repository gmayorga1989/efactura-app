package ec.tusaas.efactura.web;

import ec.tusaas.efactura.dto.plan.MiPlanResponse;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.service.MiPlanService;
import ec.tusaas.efactura.web.support.EmpresaContextoResolver;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/web/v1")
@RequiredArgsConstructor
public class MiPlanController {

  private final EmpresaContextoResolver empresaContextoResolver;
  private final MiPlanService miPlanService;

  @GetMapping("/mi-plan")
  @PreAuthorize("isAuthenticated()")
  public MiPlanResponse miPlan(
      @RequestParam(required = false) UUID empresaId, @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return miPlanService.resumen(eid);
  }
}
