package ec.tusaas.efactura.web;

import ec.tusaas.efactura.dto.apikey.ApiKeyCreateRequest;
import ec.tusaas.efactura.dto.apikey.ApiKeyCreatedResponse;
import ec.tusaas.efactura.dto.apikey.ApiKeyResponse;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.service.ApiKeyService;
import ec.tusaas.efactura.web.support.EmpresaContextoResolver;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/web/v1/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

  private final EmpresaContextoResolver empresaContextoResolver;
  private final ApiKeyService apiKeyService;

  @GetMapping
  @PreAuthorize("hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN')")
  public List<ApiKeyResponse> listar(
      @RequestParam(required = false) UUID empresaId, @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return apiKeyService.listar(eid);
  }

  @PostMapping
  @PreAuthorize("hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN')")
  public ApiKeyCreatedResponse crear(
      @RequestParam(required = false) UUID empresaId,
      @AuthenticationPrincipal UsuarioPrincipal principal,
      @Valid @RequestBody ApiKeyCreateRequest request) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return apiKeyService.crear(eid, request, principal.getEmail());
  }

  @PostMapping("/{id}/revocar")
  @PreAuthorize("hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN')")
  public void revocar(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    apiKeyService.revocar(eid, id);
  }
}
