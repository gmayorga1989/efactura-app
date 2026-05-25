package ec.tusaas.efactura.web;

import ec.tusaas.efactura.dto.invitacion.InvitarUsuarioRequest;
import ec.tusaas.efactura.dto.invitacion.InvitacionCreadaResponse;
import ec.tusaas.efactura.dto.invitacion.InvitacionPendienteResponse;
import ec.tusaas.efactura.dto.invitacion.InvitacionResponse;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.service.InvitacionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/web/v1")
@RequiredArgsConstructor
public class InvitacionesController {

  private final InvitacionService invitacionService;

  @GetMapping("/invitaciones")
  @PreAuthorize("hasAuthority('EMPRESA_ADMIN')")
  @SecurityRequirement(name = "bearer-jwt")
  public List<InvitacionResponse> listarEnMiEmpresa(
      @RequestParam(required = false) String estado,
      @RequestParam(defaultValue = "true") boolean incluirExpiradas,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    if (principal.getEmpresaId() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Use GET /empresas/{empresaId}/invitaciones");
    }
    return invitacionService.listar(principal.getEmpresaId(), estado, incluirExpiradas, principal);
  }

  @GetMapping("/invitaciones/pendientes")
  @PreAuthorize("hasAuthority('EMPRESA_ADMIN')")
  @SecurityRequirement(name = "bearer-jwt")
  public List<InvitacionPendienteResponse> pendientesEnMiEmpresa(
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    if (principal.getEmpresaId() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Use GET /empresas/{empresaId}/invitaciones/pendientes");
    }
    return invitacionService.listarPendientes(principal.getEmpresaId(), principal);
  }

  @PostMapping("/invitaciones/{invitacionId}/cancelacion")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAuthority('EMPRESA_ADMIN')")
  @SecurityRequirement(name = "bearer-jwt")
  public void cancelarEnMiEmpresa(
      @PathVariable UUID invitacionId, @AuthenticationPrincipal UsuarioPrincipal principal) {
    if (principal.getEmpresaId() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Use POST /empresas/{empresaId}/invitaciones/{id}/cancelacion");
    }
    invitacionService.cancelar(principal.getEmpresaId(), invitacionId, principal);
  }

  @PostMapping("/invitaciones/{invitacionId}/reenvio")
  @PreAuthorize("hasAuthority('EMPRESA_ADMIN')")
  @SecurityRequirement(name = "bearer-jwt")
  public InvitacionCreadaResponse reenviarEnMiEmpresa(
      @PathVariable UUID invitacionId, @AuthenticationPrincipal UsuarioPrincipal principal) {
    if (principal.getEmpresaId() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Use POST /empresas/{empresaId}/invitaciones/{id}/reenvio");
    }
    return invitacionService.reenviar(principal.getEmpresaId(), invitacionId, principal);
  }

  @GetMapping("/empresas/{empresaId}/invitaciones/pendientes")
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  @SecurityRequirement(name = "bearer-jwt")
  public List<InvitacionPendienteResponse> pendientesPlataforma(
      @PathVariable UUID empresaId, @AuthenticationPrincipal UsuarioPrincipal principal) {
    return invitacionService.listarPendientes(empresaId, principal);
  }

  @GetMapping("/empresas/{empresaId}/invitaciones")
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  @SecurityRequirement(name = "bearer-jwt")
  public List<InvitacionResponse> listarPlataforma(
      @PathVariable UUID empresaId,
      @RequestParam(required = false) String estado,
      @RequestParam(defaultValue = "true") boolean incluirExpiradas,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    return invitacionService.listar(empresaId, estado, incluirExpiradas, principal);
  }

  @PostMapping("/empresas/{empresaId}/invitaciones/{invitacionId}/cancelacion")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  @SecurityRequirement(name = "bearer-jwt")
  public void cancelarPlataforma(
      @PathVariable UUID empresaId,
      @PathVariable UUID invitacionId,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    invitacionService.cancelar(empresaId, invitacionId, principal);
  }

  @PostMapping("/empresas/{empresaId}/invitaciones/{invitacionId}/reenvio")
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  @SecurityRequirement(name = "bearer-jwt")
  public InvitacionCreadaResponse reenviarPlataforma(
      @PathVariable UUID empresaId,
      @PathVariable UUID invitacionId,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    return invitacionService.reenviar(empresaId, invitacionId, principal);
  }

  @PostMapping("/invitaciones")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('EMPRESA_ADMIN')")
  @SecurityRequirement(name = "bearer-jwt")
  public InvitacionCreadaResponse invitarEnMiEmpresa(
      @AuthenticationPrincipal UsuarioPrincipal principal, @Valid @RequestBody InvitarUsuarioRequest request) {
    if (principal.getEmpresaId() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Use POST /empresas/{empresaId}/invitaciones como administrador de plataforma");
    }
    return invitacionService.invitar(principal.getEmpresaId(), request, principal);
  }

  @PostMapping("/empresas/{empresaId}/invitaciones")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  @SecurityRequirement(name = "bearer-jwt")
  public InvitacionCreadaResponse invitarComoPlataforma(
      @PathVariable UUID empresaId,
      @AuthenticationPrincipal UsuarioPrincipal principal,
      @Valid @RequestBody InvitarUsuarioRequest request) {
    return invitacionService.invitar(empresaId, request, principal);
  }
}
