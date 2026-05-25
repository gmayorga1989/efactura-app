package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.me.AvatarResponse;
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
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.entity.Identidad;
import ec.tusaas.efactura.entity.MembresiaEmpresa;
import ec.tusaas.efactura.entity.Rol;
import ec.tusaas.efactura.repository.EmpresaRepository;
import ec.tusaas.efactura.repository.IdentidadRepository;
import ec.tusaas.efactura.repository.MembresiaEmpresaRepository;
import ec.tusaas.efactura.repository.RolRepository;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.security.TokenHasher;
import ec.tusaas.efactura.security.TotpService;
import ec.tusaas.efactura.storage.ObjectStorageService;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UsuarioService {

  private final MembresiaEmpresaRepository membresiaEmpresaRepository;
  private final IdentidadRepository identidadRepository;
  private final EmpresaRepository empresaRepository;
  private final RolRepository rolRepository;
  private final PasswordEncoder passwordEncoder;
  private final EmailNotificationService emailNotificationService;
  private final TotpService totpService;
  private final ObjectStorageService objectStorageService;

  private static final List<String> AVATAR_CONTENT_TYPES = List.of("image/png", "image/jpeg", "image/webp");
  private static final long ONLINE_WINDOW_SECONDS = 120;

  @Transactional(readOnly = true)
  public Page<UsuarioResponse> listarPorEmpresa(UUID empresaId, UsuarioPrincipal principal, Pageable pageable) {
    assertPlatformOrSameEmpresa(principal, empresaId);
    return membresiaEmpresaRepository.findAllByEmpresa_Id(empresaId, pageable).map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public Page<UsuarioResponse> listarMiEmpresa(UsuarioPrincipal principal, Pageable pageable) {
    if (principal.getEmpresaId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuario de plataforma: use /empresas/{id}/usuarios");
    }
    return listarPorEmpresa(principal.getEmpresaId(), principal, pageable);
  }

  @Transactional
  public UsuarioResponse crearEnEmpresa(UUID empresaId, UsuarioCreateRequest req, UsuarioPrincipal principal) {
    assertPlatformOrSameEmpresa(principal, empresaId);
    if (!principal.getAuthorities().stream()
        .anyMatch(a -> "EMPRESA_ADMIN".equals(a.getAuthority()) || "PLATFORM_ADMIN".equals(a.getAuthority()))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Se requiere EMPRESA_ADMIN o PLATFORM_ADMIN");
    }
    Empresa empresa =
        empresaRepository.findById(empresaId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    String email = req.email().trim().toLowerCase();
    Optional<Identidad> existente = identidadRepository.findByEmailIgnoreCase(email);
    if (existente.isPresent()
        && membresiaEmpresaRepository.existsByIdentidadIdAndEmpresaId(existente.get().getId(), empresaId)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email ya registrado en la empresa");
    }

    Rol rol =
        rolRepository
            .findByEmpresaAndCodigo(empresa, req.rolCodigo())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol no encontrado"));
    assertRolPerteneceEmpresa(rol, empresa);

    IdentidadResolved resolved = resolveOrCreateIdentidad(req, email);
    Identidad identidad = resolved.identidad();
    MembresiaEmpresa m = new MembresiaEmpresa();
    m.setIdentidad(identidad);
    m.setEmpresa(empresa);
    m.setEstado(resolved.nueva() ? "PENDIENTE_CONFIRMACION" : "ACTIVO");
    m.getRoles().add(rol);
    m = membresiaEmpresaRepository.save(m);
    emailNotificationService.enviarUsuarioCreado(
        empresaId,
        empresa,
        identidad.getEmail(),
        identidad.getNombre(),
        rol.getCodigo(),
        resolved.nueva() ? req.password() : null,
        nombreUsuario(principal));
    return toResponse(
        membresiaEmpresaRepository
            .findWithPermisosById(m.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)));
  }

  private IdentidadResolved resolveOrCreateIdentidad(UsuarioCreateRequest req, String email) {
    Optional<Identidad> existente = identidadRepository.findByEmailIgnoreCase(email);
    if (existente.isPresent()) {
      return new IdentidadResolved(existente.get(), false);
    }
    Identidad id = new Identidad();
    id.setEmail(email);
    id.setPasswordHash(passwordEncoder.encode(req.password()));
    id.setNombre(req.nombre());
    return new IdentidadResolved(identidadRepository.save(id), true);
  }

  @Transactional
  public UsuarioResponse crearEnMiEmpresa(UsuarioCreateRequest req, UsuarioPrincipal principal) {
    if (principal.getEmpresaId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use POST /empresas/{id}/usuarios");
    }
    return crearEnEmpresa(principal.getEmpresaId(), req, principal);
  }

  @Transactional
  public UsuarioResponse actualizarEnMiEmpresa(
      UUID membresiaId, UsuarioUpdateRequest req, UsuarioPrincipal principal) {
    if (principal.getEmpresaId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use PATCH /empresas/{id}/usuarios/{membresiaId}");
    }
    return actualizarEnEmpresa(principal.getEmpresaId(), membresiaId, req, principal);
  }

  @Transactional
  public UsuarioResponse actualizarEnEmpresa(
      UUID empresaId, UUID membresiaId, UsuarioUpdateRequest req, UsuarioPrincipal principal) {
    assertAdminGestionUsuarios(principal, empresaId);
    MembresiaEmpresa m =
        membresiaEmpresaRepository
            .findWithPermisosById(membresiaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    if (m.getEmpresa() == null || !m.getEmpresa().getId().equals(empresaId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario de otra empresa");
    }
    if (req.nombre() != null) {
      if (req.nombre().isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre requerido");
      }
      m.getIdentidad().setNombre(req.nombre().trim());
      m.getIdentidad().setFechaModificacion(Instant.now());
      m.getIdentidad().setUsuarioModificacion(principal.getEmail());
      identidadRepository.save(m.getIdentidad());
    }
    if (req.estado() != null) {
      if (principal.getId().equals(m.getIdentidad().getId()) && "INACTIVO".equals(req.estado())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puede inactivar su propio usuario");
      }
      m.setEstado(req.estado());
    }
    if (req.roles() != null) {
      if (req.roles().isEmpty()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe asignar al menos un rol");
      }
      List<String> codigos = req.roles().stream().map(String::trim).filter(s -> !s.isBlank()).distinct().toList();
      List<Rol> roles = rolRepository.findAllByEmpresaAndCodigoIn(m.getEmpresa(), codigos);
      if (roles.size() != codigos.size()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uno o mas roles no existen en la empresa");
      }
      m.setRoles(new HashSet<>(roles));
    }
    m.setFechaModificacion(Instant.now());
    m.setUsuarioModificacion(principal.getEmail());
    return toResponse(membresiaEmpresaRepository.save(m));
  }

  @Transactional
  public UsuarioTemporalPasswordResponse reenviarTemporalEnMiEmpresa(
      UUID membresiaId, UsuarioPrincipal principal) {
    if (principal.getEmpresaId() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Use POST /empresas/{id}/usuarios/{membresiaId}/reenviar-temporal");
    }
    return reenviarTemporalEnEmpresa(principal.getEmpresaId(), membresiaId, principal);
  }

  @Transactional
  public UsuarioTemporalPasswordResponse reenviarTemporalEnEmpresa(
      UUID empresaId, UUID membresiaId, UsuarioPrincipal principal) {
    assertAdminGestionUsuarios(principal, empresaId);
    MembresiaEmpresa m =
        membresiaEmpresaRepository
            .findWithPermisosById(membresiaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    if (m.getEmpresa() == null || !m.getEmpresa().getId().equals(empresaId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario de otra empresa");
    }
    if (!"PENDIENTE_CONFIRMACION".equals(m.getEstado())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Solo se puede reenviar a usuarios pendientes de confirmacion");
    }

    String temporal = TokenHasher.randomRefreshTokenPlain().substring(0, 12);
    Identidad identidad = m.getIdentidad();
    identidad.setPasswordHash(passwordEncoder.encode(temporal));
    identidad.setFechaModificacion(Instant.now());
    identidad.setUsuarioModificacion(principal.getEmail());
    identidadRepository.save(identidad);

    String rolCodigo = m.getRoles().stream().findFirst().map(Rol::getCodigo).orElse("");
    boolean emailEnviado =
        emailNotificationService.enviarUsuarioCreado(
            empresaId,
            m.getEmpresa(),
            identidad.getEmail(),
            identidad.getNombre(),
            rolCodigo,
            temporal,
            nombreUsuario(principal));
    return new UsuarioTemporalPasswordResponse(m.getId(), m.getEstado(), emailEnviado);
  }

  @Transactional(readOnly = true)
  public PerfilResponse obtenerPerfil(UsuarioPrincipal principal) {
    Identidad identidad =
        identidadRepository
            .findById(principal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado"));
    return toPerfilResponse(identidad);
  }

  @Transactional
  public PerfilResponse actualizarPerfil(PerfilUpdateRequest req, UsuarioPrincipal principal) {
    Identidad identidad =
        identidadRepository
            .findById(principal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado"));
    aplicarPerfil(identidad, req);
    identidad.setFechaModificacion(Instant.now());
    identidad.setUsuarioModificacion(principal.getEmail());
    return toPerfilResponse(identidadRepository.save(identidad));
  }

  @Transactional
  public void cambiarPassword(PasswordChangeRequest req, UsuarioPrincipal principal) {
    Identidad identidad =
        identidadRepository
            .findById(principal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado"));
    if (!passwordEncoder.matches(req.passwordActual(), identidad.getPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Contrasena actual incorrecta");
    }
    identidad.setPasswordHash(passwordEncoder.encode(req.passwordNuevo()));
    identidad.setFechaModificacion(Instant.now());
    identidad.setUsuarioModificacion(principal.getEmail());
    identidadRepository.save(identidad);
  }

  @Transactional
  public AvatarResponse subirAvatar(MultipartFile archivo, UsuarioPrincipal principal) throws Exception {
    validarAvatar(archivo);
    Identidad identidad =
        identidadRepository
            .findById(principal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado"));
    String extension = extension(archivo.getOriginalFilename(), archivo.getContentType());
    String key = "avatars/" + identidad.getId() + "/avatar-" + UUID.randomUUID() + extension;
    objectStorageService.guardarPublico(key, archivo.getBytes(), archivo.getContentType());
    identidad.setAvatarStorageKey(key);
    identidad.setAvatarUrl(objectStorageService.publicUrl(key));
    identidad.setFechaModificacion(Instant.now());
    identidad.setUsuarioModificacion(principal.getEmail());
    identidadRepository.save(identidad);
    return new AvatarResponse(identidad.getId(), identidad.getAvatarUrl(), key);
  }

  @Transactional
  public PerfilResponse eliminarAvatar(UsuarioPrincipal principal) {
    Identidad identidad =
        identidadRepository
            .findById(principal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado"));
    identidad.setAvatarUrl(null);
    identidad.setAvatarStorageKey(null);
    identidad.setFechaModificacion(Instant.now());
    identidad.setUsuarioModificacion(principal.getEmail());
    return toPerfilResponse(identidadRepository.save(identidad));
  }

  @Transactional
  public PresenceResponse registrarPing(UsuarioPrincipal principal) {
    Identidad identidad =
        identidadRepository
            .findById(principal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado"));
    identidad.setUltimoPing(Instant.now());
    identidadRepository.save(identidad);
    return toPresenceResponse(identidad);
  }

  @Transactional
  public TwoFactorSetupResponse iniciarTwoFactor(UsuarioPrincipal principal) {
    Identidad identidad =
        identidadRepository
            .findById(principal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado"));
    if (identidad.isMfaHabilitado()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "2FA ya esta habilitado");
    }
    String secret = totpService.generateSecret();
    identidad.setMfaSecret(secret);
    identidad.setFechaModificacion(Instant.now());
    identidad.setUsuarioModificacion(principal.getEmail());
    identidadRepository.save(identidad);
    return new TwoFactorSetupResponse(
        secret, totpService.otpauthUri("eFactura", identidad.getEmail(), secret), false);
  }

  @Transactional
  public PerfilResponse confirmarTwoFactor(TwoFactorConfirmRequest req, UsuarioPrincipal principal) {
    Identidad identidad =
        identidadRepository
            .findById(principal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado"));
    if (identidad.getMfaSecret() == null || identidad.getMfaSecret().isBlank()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Debe iniciar configuracion 2FA primero");
    }
    if (!totpService.verify(identidad.getMfaSecret(), req.codigo())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Codigo 2FA invalido");
    }
    identidad.setMfaHabilitado(true);
    identidad.setFechaModificacion(Instant.now());
    identidad.setUsuarioModificacion(principal.getEmail());
    return toPerfilResponse(identidadRepository.save(identidad));
  }

  @Transactional
  public PerfilResponse deshabilitarTwoFactor(TwoFactorDisableRequest req, UsuarioPrincipal principal) {
    Identidad identidad =
        identidadRepository
            .findById(principal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado"));
    if (!passwordEncoder.matches(req.passwordActual(), identidad.getPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Contrasena actual incorrecta");
    }
    if (!identidad.isMfaHabilitado() || !totpService.verify(identidad.getMfaSecret(), req.codigo())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Codigo 2FA invalido");
    }
    identidad.setMfaHabilitado(false);
    identidad.setMfaSecret(null);
    identidad.setFechaModificacion(Instant.now());
    identidad.setUsuarioModificacion(principal.getEmail());
    return toPerfilResponse(identidadRepository.save(identidad));
  }

  private void assertPlatformOrSameEmpresa(UsuarioPrincipal principal, UUID empresaId) {
    boolean platform =
        principal.getAuthorities().stream().anyMatch(a -> "PLATFORM_ADMIN".equals(a.getAuthority()));
    if (platform) {
      return;
    }
    if (principal.getEmpresaId() == null || !principal.getEmpresaId().equals(empresaId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sin acceso a esta empresa");
    }
  }

  private void assertAdminGestionUsuarios(UsuarioPrincipal principal, UUID empresaId) {
    assertPlatformOrSameEmpresa(principal, empresaId);
    if (!principal.getAuthorities().stream()
        .anyMatch(a -> "EMPRESA_ADMIN".equals(a.getAuthority()) || "PLATFORM_ADMIN".equals(a.getAuthority()))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Se requiere EMPRESA_ADMIN o PLATFORM_ADMIN");
    }
  }

  private MembresiaEmpresa membresiaActual(UsuarioPrincipal principal) {
    if (principal.getEmpresaId() == null) {
      return membresiaEmpresaRepository
          .findPlataformaByIdentidad(principal.getId())
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membresia no encontrada"));
    }
    return membresiaEmpresaRepository
        .findTenantByIdentidadAndEmpresa(principal.getId(), principal.getEmpresaId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membresia no encontrada"));
  }

  private String nombreUsuario(UsuarioPrincipal principal) {
    return identidadRepository
        .findById(principal.getId())
        .map(Identidad::getNombre)
        .filter(nombre -> !nombre.isBlank())
        .orElse(principal.getEmail());
  }

  private static void assertRolPerteneceEmpresa(Rol rol, Empresa empresa) {
    if (rol.getEmpresa() == null || !Objects.equals(rol.getEmpresa().getId(), empresa.getId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol no pertenece a esta empresa");
    }
  }

  private UsuarioResponse toResponse(MembresiaEmpresa m) {
    Identidad i = m.getIdentidad();
    return new UsuarioResponse(
        m.getId(),
        i.getId(),
        m.getEmpresa() == null ? null : m.getEmpresa().getId(),
        i.getEmail(),
        i.getNombre(),
        i.getAvatarUrl(),
        m.getEstado(),
        isOnline(i),
        i.getUltimoPing(),
        m.getRoles().stream().map(Rol::getCodigo).toList());
  }

  private static void aplicarPerfil(Identidad identidad, PerfilUpdateRequest req) {
    if (req.nombre() != null) {
      if (req.nombre().isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre requerido");
      }
      identidad.setNombre(req.nombre().trim());
    }
    identidad.setGenero(trimUpper(req.genero()));
    identidad.setFechaNacimiento(req.fechaNacimiento());
    identidad.setPais(trimUpper(req.pais()));
    identidad.setProvincia(trim(req.provincia()));
    identidad.setCanton(trim(req.canton()));
    identidad.setCiudad(trim(req.ciudad()));
    identidad.setParroquia(trim(req.parroquia()));
    if (req.idioma() != null) {
      identidad.setIdioma(normalizeIdioma(req.idioma()));
    }
    if (req.moneda() != null) {
      identidad.setMoneda(req.moneda().trim().toUpperCase(Locale.ROOT));
    }
    if (req.zonaHoraria() != null) {
      String zona = req.zonaHoraria().trim();
      try {
        ZoneId.of(zona);
      } catch (Exception e) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zona horaria invalida");
      }
      identidad.setZonaHoraria(zona);
    }
  }

  private static PerfilResponse toPerfilResponse(Identidad i) {
    return new PerfilResponse(
        i.getId(),
        i.getEmail(),
        i.getNombre(),
        i.getGenero(),
        i.getFechaNacimiento(),
        i.getPais(),
        i.getProvincia(),
        i.getCanton(),
        i.getCiudad(),
        i.getParroquia(),
        i.getIdioma(),
        i.getMoneda(),
        i.getZonaHoraria(),
        i.getAvatarUrl(),
        i.getUltimoPing(),
        isOnline(i),
        i.isMfaHabilitado());
  }

  private static PresenceResponse toPresenceResponse(Identidad i) {
    return new PresenceResponse(i.getId(), isOnline(i), i.getUltimoPing());
  }

  private static boolean isOnline(Identidad i) {
    return i.getUltimoPing() != null
        && i.getUltimoPing().isAfter(Instant.now().minus(ONLINE_WINDOW_SECONDS, ChronoUnit.SECONDS));
  }

  private static void validarAvatar(MultipartFile archivo) {
    if (archivo == null || archivo.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo vacio");
    }
    if (archivo.getSize() > 2 * 1024 * 1024) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Avatar maximo 2MB");
    }
    String contentType = archivo.getContentType();
    if (contentType == null || !AVATAR_CONTENT_TYPES.contains(contentType)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de avatar no permitido");
    }
  }

  private static String extension(String nombre, String contentType) {
    if (nombre != null) {
      String lower = nombre.toLowerCase(Locale.ROOT);
      for (String ext : Arrays.asList(".png", ".jpg", ".jpeg", ".webp")) {
        if (lower.endsWith(ext)) {
          return ext;
        }
      }
    }
    return switch (contentType) {
      case "image/png" -> ".png";
      case "image/jpeg" -> ".jpg";
      case "image/webp" -> ".webp";
      default -> ".bin";
    };
  }

  private static String normalizeIdioma(String value) {
    String idioma = value.trim().toLowerCase(Locale.ROOT);
    if (!idioma.matches("^[a-z]{2}(-[a-z]{2})?$")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idioma invalido");
    }
    return idioma;
  }

  private static String trim(String value) {
    return value == null ? null : value.trim();
  }

  private static String trimUpper(String value) {
    return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
  }

  private record IdentidadResolved(Identidad identidad, boolean nueva) {}
}
