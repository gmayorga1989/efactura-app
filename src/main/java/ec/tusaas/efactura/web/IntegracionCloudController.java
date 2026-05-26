package ec.tusaas.efactura.web;

import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.service.CloudDriveIntegrationService;
import ec.tusaas.efactura.web.support.EmpresaContextoResolver;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/web/v1/integraciones/cloud")
@RequiredArgsConstructor
public class IntegracionCloudController {

  private final EmpresaContextoResolver empresaContextoResolver;
  private final CloudDriveIntegrationService cloudDriveIntegrationService;

  private static final String VENTAS =
      "hasAuthority('VENTAS_GESTIONAR') or hasAuthority('FACTURA_EMITIR') or hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN')";

  @GetMapping("/config")
  @PreAuthorize(VENTAS)
  public Map<String, Object> config(
      @RequestParam(required = false) UUID empresaId, @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return cloudDriveIntegrationService.configPublico(eid);
  }

  @GetMapping("/google/auth-url")
  @PreAuthorize(VENTAS)
  public Map<String, String> googleAuthUrl(
      @RequestParam(required = false) UUID empresaId, @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return Map.of("url", cloudDriveIntegrationService.googleAuthUrl(eid));
  }

  @GetMapping(value = "/google/callback", produces = MediaType.TEXT_HTML_VALUE)
  public String googleCallback(@RequestParam String code, @RequestParam String state) {
    return cloudDriveIntegrationService.googleCallback(code, state);
  }

  @GetMapping("/google/access-token")
  @PreAuthorize(VENTAS)
  public Map<String, String> googleAccessToken(
      @RequestParam(required = false) UUID empresaId, @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return Map.of("accessToken", cloudDriveIntegrationService.googleAccessToken(eid));
  }

  @GetMapping("/microsoft/auth-url")
  @PreAuthorize(VENTAS)
  public Map<String, String> microsoftAuthUrl(
      @RequestParam(required = false) UUID empresaId, @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return Map.of("url", cloudDriveIntegrationService.microsoftAuthUrl(eid));
  }

  @GetMapping("/microsoft/access-token")
  @PreAuthorize(VENTAS)
  public Map<String, String> microsoftAccessToken(
      @RequestParam(required = false) UUID empresaId, @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return Map.of("accessToken", cloudDriveIntegrationService.microsoftAccessToken(eid));
  }

  @GetMapping(value = "/microsoft/callback", produces = MediaType.TEXT_HTML_VALUE)
  public String microsoftCallback(@RequestParam String code, @RequestParam String state) {
    return cloudDriveIntegrationService.microsoftCallback(code, state);
  }
}
