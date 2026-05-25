package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.rol.RolCreateRequest;
import ec.tusaas.efactura.dto.rol.RolResponse;
import ec.tusaas.efactura.dto.rol.RolUpdateRequest;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.entity.Permiso;
import ec.tusaas.efactura.entity.Rol;
import ec.tusaas.efactura.repository.EmpresaRepository;
import ec.tusaas.efactura.repository.MembresiaEmpresaRepository;
import ec.tusaas.efactura.repository.PermisoRepository;
import ec.tusaas.efactura.repository.RolRepository;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RolService {

  private final RolRepository rolRepository;
  private final EmpresaRepository empresaRepository;
  private final PermisoRepository permisoRepository;
  private final MembresiaEmpresaRepository membresiaEmpresaRepository;

  @Transactional(readOnly = true)
  public List<RolResponse> listar(UUID empresaId, UsuarioPrincipal principal) {
    return listar(empresaId, null, principal);
  }

  @Transactional(readOnly = true)
  public List<RolResponse> listar(UUID empresaId, String estado, UsuarioPrincipal principal) {
    assertAccesoEmpresa(empresaId, principal);
    String estadoFiltro = estado == null || estado.isBlank() ? null : estado.trim().toUpperCase();
    return rolRepository.findAllByEmpresaIdAndOptionalEstadoWithPermisos(empresaId, estadoFiltro).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public RolResponse obtener(UUID empresaId, UUID rolId, UsuarioPrincipal principal) {
    assertAccesoEmpresa(empresaId, principal);
    Rol r =
        rolRepository
            .findByIdAndEmpresaIdWithPermisos(rolId, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado"));
    return toResponse(r);
  }

  @Transactional
  public RolResponse crear(UUID empresaId, RolCreateRequest req, UsuarioPrincipal principal) {
    assertAdminEmpresa(empresaId, principal);
    Empresa empresa =
        empresaRepository.findById(empresaId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    String codigo = req.codigo().trim().toUpperCase();
    if (rolRepository.findByEmpresaAndCodigo(empresa, codigo).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un rol con ese código");
    }
    Rol r = new Rol();
    r.setEmpresa(empresa);
    r.setCodigo(codigo);
    r.setNombre(req.nombre().trim());
    r.setSistema(false);
    r.setEstado("ACTIVO");
    r.setUsuarioCreacion(principal.getEmail());
    aplicarPermisos(r, req.permisosCodigos());
    r = rolRepository.save(r);
    return toResponse(rolRepository.findByIdWithPermisos(r.getId()).orElse(r));
  }

  @Transactional
  public RolResponse actualizar(UUID empresaId, UUID rolId, RolUpdateRequest req, UsuarioPrincipal principal) {
    assertAdminEmpresa(empresaId, principal);
    Rol r =
        rolRepository
            .findByIdWithPermisos(rolId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado"));
    if (r.getEmpresa() == null || !r.getEmpresa().getId().equals(empresaId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado");
    }
    if (r.isSistema()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rol de sistema no editable");
    }
    if (req.nombre() != null) {
      if (req.nombre().isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre requerido");
      }
      r.setNombre(req.nombre().trim());
    }
    if (req.permisosCodigos() != null) {
      aplicarPermisos(r, req.permisosCodigos());
    }
    r.setFechaModificacion(Instant.now());
    r.setUsuarioModificacion(principal.getEmail());
    return toResponse(rolRepository.save(r));
  }

  @Transactional
  public RolResponse cambiarEstado(UUID empresaId, UUID rolId, String estado, UsuarioPrincipal principal) {
    assertAdminEmpresa(empresaId, principal);
    Rol r =
        rolRepository
            .findByIdAndEmpresaIdWithPermisos(rolId, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado"));
    if (r.isSistema()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rol de sistema no modificable");
    }
    r.setEstado(estado);
    r.setFechaModificacion(Instant.now());
    r.setUsuarioModificacion(principal.getEmail());
    return toResponse(rolRepository.save(r));
  }

  @Transactional
  public void eliminar(UUID empresaId, UUID rolId, UsuarioPrincipal principal) {
    assertAdminEmpresa(empresaId, principal);
    Rol r =
        rolRepository
            .findById(rolId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado"));
    if (r.getEmpresa() == null || !r.getEmpresa().getId().equals(empresaId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado");
    }
    if (r.isSistema()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rol de sistema no eliminable");
    }
    if (membresiaEmpresaRepository.countByRolesId(rolId) > 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El rol está asignado a uno o más miembros");
    }
    rolRepository.delete(r);
  }

  private void aplicarPermisos(Rol r, List<String> codigos) {
    r.getPermisos().clear();
    if (codigos == null || codigos.isEmpty()) {
      return;
    }
    List<String> missing = new ArrayList<>();
    for (String raw : codigos) {
      String c = raw == null ? "" : raw.trim();
      if (c.isEmpty()) {
        continue;
      }
      permisoRepository
          .findByCodigo(c)
          .ifPresentOrElse(p -> r.getPermisos().add(p), () -> missing.add(c));
    }
    if (!missing.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Permisos desconocidos: " + String.join(", ", missing));
    }
  }

  private RolResponse toResponse(Rol r) {
    List<String> permisos =
        r.getPermisos().stream().map(Permiso::getCodigo).sorted().distinct().toList();
    long usuariosAsignados = membresiaEmpresaRepository.countByRolesId(r.getId());
    return new RolResponse(
        r.getId(), r.getCodigo(), r.getNombre(), r.isSistema(), r.getEstado(), usuariosAsignados, permisos);
  }

  private static void assertAccesoEmpresa(UUID empresaId, UsuarioPrincipal principal) {
    boolean platform =
        principal.getAuthorities().stream().anyMatch(a -> "PLATFORM_ADMIN".equals(a.getAuthority()));
    if (platform) {
      return;
    }
    if (principal.getEmpresaId() == null || !principal.getEmpresaId().equals(empresaId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sin acceso a esta empresa");
    }
  }

  private static void assertAdminEmpresa(UUID empresaId, UsuarioPrincipal principal) {
    assertAccesoEmpresa(empresaId, principal);
    if (!principal.getAuthorities().stream()
        .anyMatch(a -> "EMPRESA_ADMIN".equals(a.getAuthority()) || "PLATFORM_ADMIN".equals(a.getAuthority()))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Se requiere EMPRESA_ADMIN");
    }
  }
}
