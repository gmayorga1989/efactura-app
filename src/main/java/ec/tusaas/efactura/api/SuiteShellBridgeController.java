package ec.tusaas.efactura.api;

import ec.tusaas.efactura.dto.auth.SuiteShellBridgeConsumeResponse;
import ec.tusaas.efactura.dto.auth.SuiteShellBridgeCreateRequest;
import ec.tusaas.efactura.dto.auth.SuiteShellBridgeCreateResponse;
import ec.tusaas.efactura.security.SuiteIdentityJwtService;
import ec.tusaas.efactura.service.SuiteShellBridgeService;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Intercambio opaco para que el Suite Shell recupere tokens Identity sin llevar JWT en la URL (nueva
 * pestaña sin sessionStorage compartido con eFactura).
 */
@RestController
@RequestMapping("/api/public/v1/auth")
@RequiredArgsConstructor
public class SuiteShellBridgeController {

  private final SuiteShellBridgeService bridgeService;
  private final SuiteIdentityJwtService suiteIdentityJwtService;

  @PostMapping("/suite-shell-bridge")
  public SuiteShellBridgeCreateResponse create(@Valid @RequestBody SuiteShellBridgeCreateRequest body) {
    if (!suiteIdentityJwtService.isConfigured()) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Integración Suite Identity no configurada en eFactura");
    }
    try {
      suiteIdentityJwtService.parseAccessToken(body.identityAccess());
    } catch (JwtException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Identity inválido");
    }
    String refresh = body.identityRefresh() == null ? null : body.identityRefresh().trim();
    if (refresh != null && refresh.isEmpty()) {
      refresh = null;
    }
    String id = bridgeService.put(body.identityAccess().trim(), refresh);
    return new SuiteShellBridgeCreateResponse(id);
  }

  @GetMapping("/suite-shell-bridge/{id}")
  public SuiteShellBridgeConsumeResponse consume(@PathVariable String id) {
    return bridgeService
        .consume(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enlace caducado o ya utilizado"));
  }
}
