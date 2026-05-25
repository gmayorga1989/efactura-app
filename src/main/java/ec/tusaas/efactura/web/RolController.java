package ec.tusaas.efactura.web;

import ec.tusaas.efactura.dto.rol.RolCreateRequest;
import ec.tusaas.efactura.dto.rol.RolEstadoRequest;
import ec.tusaas.efactura.dto.rol.RolResponse;
import ec.tusaas.efactura.dto.rol.RolUpdateRequest;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.service.RolService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/web/v1")
@RequiredArgsConstructor
public class RolController {

  private final RolService rolService;

  @GetMapping("/roles")
  @PreAuthorize("hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN')")
  @SecurityRequirement(name = "bearer-jwt")
  public List<RolResponse> listarMiEmpresa(
      @RequestParam(required = false) String estado,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    if (principal.getEmpresaId() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Use GET /empresas/{empresaId}/roles como plataforma");
    }
    return rolService.listar(principal.getEmpresaId(), estado, principal);
  }

  @GetMapping("/roles/{rolId}")
  @PreAuthorize("hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN')")
  @SecurityRequirement(name = "bearer-jwt")
  public RolResponse obtenerMiEmpresa(
      @PathVariable UUID rolId, @AuthenticationPrincipal UsuarioPrincipal principal) {
    if (principal.getEmpresaId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use GET /empresas/{empresaId}/roles/{rolId}");
    }
    return rolService.obtener(principal.getEmpresaId(), rolId, principal);
  }

  @PostMapping("/roles")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('EMPRESA_ADMIN')")
  @SecurityRequirement(name = "bearer-jwt")
  public RolResponse crearEnMiEmpresa(
      @AuthenticationPrincipal UsuarioPrincipal principal, @Valid @RequestBody RolCreateRequest request) {
    if (principal.getEmpresaId() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Use POST /empresas/{empresaId}/roles como plataforma");
    }
    return rolService.crear(principal.getEmpresaId(), request, principal);
  }

  @PutMapping("/roles/{rolId}")
  @PreAuthorize("hasAuthority('EMPRESA_ADMIN')")
  @SecurityRequirement(name = "bearer-jwt")
  public RolResponse actualizarEnMiEmpresa(
      @PathVariable UUID rolId,
      @AuthenticationPrincipal UsuarioPrincipal principal,
      @Valid @RequestBody RolUpdateRequest request) {
    if (principal.getEmpresaId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use ruta de plataforma");
    }
    return rolService.actualizar(principal.getEmpresaId(), rolId, request, principal);
  }

  @PutMapping("/roles/{rolId}/estado")
  @PreAuthorize("hasAuthority('EMPRESA_ADMIN')")
  @SecurityRequirement(name = "bearer-jwt")
  public RolResponse cambiarEstadoEnMiEmpresa(
      @PathVariable UUID rolId,
      @AuthenticationPrincipal UsuarioPrincipal principal,
      @Valid @RequestBody RolEstadoRequest request) {
    if (principal.getEmpresaId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use ruta de plataforma");
    }
    return rolService.cambiarEstado(principal.getEmpresaId(), rolId, request.estado(), principal);
  }

  @DeleteMapping("/roles/{rolId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAuthority('EMPRESA_ADMIN')")
  @SecurityRequirement(name = "bearer-jwt")
  public void eliminarEnMiEmpresa(
      @PathVariable UUID rolId, @AuthenticationPrincipal UsuarioPrincipal principal) {
    if (principal.getEmpresaId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use ruta de plataforma");
    }
    rolService.eliminar(principal.getEmpresaId(), rolId, principal);
  }

  @GetMapping("/empresas/{empresaId}/roles")
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  @SecurityRequirement(name = "bearer-jwt")
  public List<RolResponse> listarPlataforma(
      @PathVariable UUID empresaId,
      @RequestParam(required = false) String estado,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    return rolService.listar(empresaId, estado, principal);
  }

  @GetMapping("/empresas/{empresaId}/roles/{rolId}")
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  @SecurityRequirement(name = "bearer-jwt")
  public RolResponse obtenerPlataforma(
      @PathVariable UUID empresaId,
      @PathVariable UUID rolId,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    return rolService.obtener(empresaId, rolId, principal);
  }

  @PostMapping("/empresas/{empresaId}/roles")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  @SecurityRequirement(name = "bearer-jwt")
  public RolResponse crearPlataforma(
      @PathVariable UUID empresaId,
      @AuthenticationPrincipal UsuarioPrincipal principal,
      @Valid @RequestBody RolCreateRequest request) {
    return rolService.crear(empresaId, request, principal);
  }

  @PutMapping("/empresas/{empresaId}/roles/{rolId}")
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  @SecurityRequirement(name = "bearer-jwt")
  public RolResponse actualizarPlataforma(
      @PathVariable UUID empresaId,
      @PathVariable UUID rolId,
      @AuthenticationPrincipal UsuarioPrincipal principal,
      @Valid @RequestBody RolUpdateRequest request) {
    return rolService.actualizar(empresaId, rolId, request, principal);
  }

  @PutMapping("/empresas/{empresaId}/roles/{rolId}/estado")
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  @SecurityRequirement(name = "bearer-jwt")
  public RolResponse cambiarEstadoPlataforma(
      @PathVariable UUID empresaId,
      @PathVariable UUID rolId,
      @AuthenticationPrincipal UsuarioPrincipal principal,
      @Valid @RequestBody RolEstadoRequest request) {
    return rolService.cambiarEstado(empresaId, rolId, request.estado(), principal);
  }

  @DeleteMapping("/empresas/{empresaId}/roles/{rolId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  @SecurityRequirement(name = "bearer-jwt")
  public void eliminarPlataforma(
      @PathVariable UUID empresaId,
      @PathVariable UUID rolId,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    rolService.eliminar(empresaId, rolId, principal);
  }
}
