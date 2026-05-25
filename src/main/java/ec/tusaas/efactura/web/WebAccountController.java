package ec.tusaas.efactura.web;

import ec.tusaas.efactura.dto.me.AvatarResponse;
import ec.tusaas.efactura.dto.me.MeFeatures;
import ec.tusaas.efactura.dto.me.MeResponse;
import ec.tusaas.efactura.dto.me.MiEmpresaResumenResponse;
import ec.tusaas.efactura.dto.me.PasswordChangeRequest;
import ec.tusaas.efactura.dto.me.PerfilResponse;
import ec.tusaas.efactura.dto.me.PerfilUpdateRequest;
import ec.tusaas.efactura.dto.me.PresenceResponse;
import ec.tusaas.efactura.dto.me.TwoFactorConfirmRequest;
import ec.tusaas.efactura.dto.me.TwoFactorDisableRequest;
import ec.tusaas.efactura.dto.me.TwoFactorSetupResponse;
import ec.tusaas.efactura.dto.usuario.UsuarioCreateRequest;
import ec.tusaas.efactura.dto.usuario.UsuarioResponse;
import ec.tusaas.efactura.dto.usuario.UsuarioTemporalPasswordResponse;
import ec.tusaas.efactura.dto.usuario.UsuarioUpdateRequest;
import ec.tusaas.efactura.entity.MembresiaEmpresa;
import ec.tusaas.efactura.mapper.EmpresaMapper;
import ec.tusaas.efactura.repository.MembresiaEmpresaRepository;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.service.UsuarioService;
import ec.tusaas.efactura.util.MembresiaPermisos;
import jakarta.validation.Valid;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/web/v1")
@RequiredArgsConstructor
public class WebAccountController {

  private final MembresiaEmpresaRepository membresiaEmpresaRepository;
  private final EmpresaMapper empresaMapper;
  private final UsuarioService usuarioService;

  @GetMapping("/me")
  public MeResponse me(@AuthenticationPrincipal UsuarioPrincipal principal) {
    MembresiaEmpresa m = resolveMembresiaActual(principal);
    var permisos = MembresiaPermisos.codigos(m);
    var roles = MembresiaPermisos.rolesCodigos(m);
    boolean esPlataforma = principal.getEmpresaId() == null;
    var features = MeFeatures.fromPermisos(new HashSet<>(permisos), esPlataforma);
    var empresaDto =
        m.getEmpresa() == null ? null : empresaMapper.toResponse(m.getEmpresa());
    return new MeResponse(
        principal.getId(),
        m.getId(),
        principal.getEmail(),
        m.getIdentidad().getNombre(),
        principal.getEmpresaId(),
        empresaDto,
        permisos,
        roles,
        features,
        usuarioService.obtenerPerfil(principal),
        List.of());
  }

  @GetMapping("/mis-empresas")
  public List<MiEmpresaResumenResponse> misEmpresas(@AuthenticationPrincipal UsuarioPrincipal principal) {
    return membresiaEmpresaRepository.findAllByIdentidad_Id(principal.getId()).stream()
        .map(m -> toResumen(m, principal))
        .toList();
  }

  @PatchMapping("/me/perfil")
  public PerfilResponse actualizarPerfil(
      @AuthenticationPrincipal UsuarioPrincipal principal, @Valid @RequestBody PerfilUpdateRequest request) {
    return usuarioService.actualizarPerfil(request, principal);
  }

  @GetMapping("/me/perfil")
  public PerfilResponse obtenerPerfil(@AuthenticationPrincipal UsuarioPrincipal principal) {
    return usuarioService.obtenerPerfil(principal);
  }

  @PostMapping("/me/password")
  public void cambiarPassword(
      @AuthenticationPrincipal UsuarioPrincipal principal, @Valid @RequestBody PasswordChangeRequest request) {
    usuarioService.cambiarPassword(request, principal);
  }

  @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public AvatarResponse subirAvatar(
      @AuthenticationPrincipal UsuarioPrincipal principal, @RequestPart("archivo") MultipartFile archivo)
      throws Exception {
    return usuarioService.subirAvatar(archivo, principal);
  }

  @DeleteMapping("/me/avatar")
  public PerfilResponse eliminarAvatar(@AuthenticationPrincipal UsuarioPrincipal principal) {
    return usuarioService.eliminarAvatar(principal);
  }

  @PostMapping("/me/presence/ping")
  public PresenceResponse registrarPing(@AuthenticationPrincipal UsuarioPrincipal principal) {
    return usuarioService.registrarPing(principal);
  }

  @PostMapping("/me/2fa/setup")
  public TwoFactorSetupResponse iniciarTwoFactor(@AuthenticationPrincipal UsuarioPrincipal principal) {
    return usuarioService.iniciarTwoFactor(principal);
  }

  @PostMapping("/me/2fa/confirm")
  public PerfilResponse confirmarTwoFactor(
      @AuthenticationPrincipal UsuarioPrincipal principal, @Valid @RequestBody TwoFactorConfirmRequest request) {
    return usuarioService.confirmarTwoFactor(request, principal);
  }

  @PostMapping("/me/2fa/disable")
  public PerfilResponse deshabilitarTwoFactor(
      @AuthenticationPrincipal UsuarioPrincipal principal, @Valid @RequestBody TwoFactorDisableRequest request) {
    return usuarioService.deshabilitarTwoFactor(request, principal);
  }

  @GetMapping("/usuarios")
  public Page<UsuarioResponse> listarMisUsuarios(
      @AuthenticationPrincipal UsuarioPrincipal principal, @PageableDefault(size = 20) Pageable pageable) {
    return usuarioService.listarMiEmpresa(principal, pageable);
  }

  @PostMapping("/usuarios")
  @PreAuthorize("hasAuthority('EMPRESA_ADMIN')")
  public UsuarioResponse crearUsuarioTenant(
      @AuthenticationPrincipal UsuarioPrincipal principal,
      @Valid @RequestBody UsuarioCreateRequest request) {
    return usuarioService.crearEnMiEmpresa(request, principal);
  }

  @PatchMapping("/usuarios/{membresiaId}")
  @PreAuthorize("hasAuthority('EMPRESA_ADMIN')")
  public UsuarioResponse actualizarUsuarioTenant(
      @PathVariable UUID membresiaId,
      @AuthenticationPrincipal UsuarioPrincipal principal,
      @Valid @RequestBody UsuarioUpdateRequest request) {
    return usuarioService.actualizarEnMiEmpresa(membresiaId, request, principal);
  }

  @PostMapping("/usuarios/{membresiaId}/reenviar-temporal")
  @PreAuthorize("hasAuthority('EMPRESA_ADMIN')")
  public UsuarioTemporalPasswordResponse reenviarTemporalTenant(
      @PathVariable UUID membresiaId,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    return usuarioService.reenviarTemporalEnMiEmpresa(membresiaId, principal);
  }

  @GetMapping("/empresas/{empresaId}/usuarios")
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  public Page<UsuarioResponse> listarUsuariosPlataforma(
      @PathVariable UUID empresaId,
      @AuthenticationPrincipal UsuarioPrincipal principal,
      @PageableDefault(size = 20) Pageable pageable) {
    return usuarioService.listarPorEmpresa(empresaId, principal, pageable);
  }

  @PostMapping("/empresas/{empresaId}/usuarios")
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  public UsuarioResponse crearUsuarioPlataforma(
      @PathVariable UUID empresaId,
      @AuthenticationPrincipal UsuarioPrincipal principal,
      @Valid @RequestBody UsuarioCreateRequest request) {
    return usuarioService.crearEnEmpresa(empresaId, request, principal);
  }

  @PatchMapping("/empresas/{empresaId}/usuarios/{membresiaId}")
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  public UsuarioResponse actualizarUsuarioPlataforma(
      @PathVariable UUID empresaId,
      @PathVariable UUID membresiaId,
      @AuthenticationPrincipal UsuarioPrincipal principal,
      @Valid @RequestBody UsuarioUpdateRequest request) {
    return usuarioService.actualizarEnEmpresa(empresaId, membresiaId, request, principal);
  }

  @PostMapping("/empresas/{empresaId}/usuarios/{membresiaId}/reenviar-temporal")
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  public UsuarioTemporalPasswordResponse reenviarTemporalPlataforma(
      @PathVariable UUID empresaId,
      @PathVariable UUID membresiaId,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    return usuarioService.reenviarTemporalEnEmpresa(empresaId, membresiaId, principal);
  }

  private MembresiaEmpresa resolveMembresiaActual(UsuarioPrincipal principal) {
    if (principal.getEmpresaId() == null) {
      return membresiaEmpresaRepository
          .findPlataformaByIdentidad(principal.getId())
          .orElseThrow(
              () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membresía de plataforma no encontrada"));
    }
    return membresiaEmpresaRepository
        .findTenantByIdentidadAndEmpresa(principal.getId(), principal.getEmpresaId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membresía no encontrada"));
  }

  private MiEmpresaResumenResponse toResumen(MembresiaEmpresa m, UsuarioPrincipal principal) {
    boolean esActual =
        (principal.getEmpresaId() == null && m.getEmpresa() == null)
            || (principal.getEmpresaId() != null
                && m.getEmpresa() != null
                && principal.getEmpresaId().equals(m.getEmpresa().getId()));
    if (m.getEmpresa() == null) {
      return new MiEmpresaResumenResponse(
          m.getId(),
          null,
          true,
          "Plataforma",
          null,
          null,
          null,
          m.getEstado(),
          true,
          esActual);
    }
    var e = m.getEmpresa();
    return new MiEmpresaResumenResponse(
        m.getId(),
        e.getId(),
        false,
        e.getRazonSocial(),
        e.getNombreComercial(),
        e.getRuc(),
        e.getSlug(),
        m.getEstado(),
        "ACTIVO".equalsIgnoreCase(e.getEstado()),
        esActual);
  }
}
