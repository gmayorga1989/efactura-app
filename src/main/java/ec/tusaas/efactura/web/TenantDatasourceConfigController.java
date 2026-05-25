package ec.tusaas.efactura.web;

import ec.tusaas.efactura.dto.tenant.TenantDatasourceConfigRequest;
import ec.tusaas.efactura.dto.tenant.TenantDatasourceConfigResponse;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.service.TenantDatasourceConfigService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/web/v1/tenant-datasources")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
public class TenantDatasourceConfigController {

  private final TenantDatasourceConfigService tenantDatasourceConfigService;

  @GetMapping("/empresas/{empresaId}")
  public TenantDatasourceConfigResponse obtener(@PathVariable UUID empresaId) {
    return tenantDatasourceConfigService.obtener(empresaId);
  }

  @PatchMapping("/empresas/{empresaId}")
  public TenantDatasourceConfigResponse actualizar(
      @PathVariable UUID empresaId,
      @Valid @RequestBody TenantDatasourceConfigRequest request,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    return tenantDatasourceConfigService.actualizar(empresaId, request, principal);
  }

  @GetMapping
  public List<TenantDatasourceConfigResponse> listarPorDatasource(@RequestParam String datasourceKey) {
    return tenantDatasourceConfigService.listarPorDatasource(datasourceKey);
  }
}
