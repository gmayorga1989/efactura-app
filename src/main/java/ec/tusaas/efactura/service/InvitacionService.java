package ec.tusaas.efactura.service;

import ec.tusaas.efactura.config.props.InvitacionProperties;
import ec.tusaas.efactura.dto.invitacion.AcceptInviteRequest;
import ec.tusaas.efactura.dto.invitacion.InvitarUsuarioRequest;
import ec.tusaas.efactura.dto.invitacion.InvitacionCreadaResponse;
import ec.tusaas.efactura.dto.invitacion.InvitacionPendienteResponse;
import ec.tusaas.efactura.dto.invitacion.InvitacionResponse;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.entity.Identidad;
import ec.tusaas.efactura.entity.MembresiaEmpresa;
import ec.tusaas.efactura.entity.MembresiaInvitacion;
import ec.tusaas.efactura.entity.Rol;
import ec.tusaas.efactura.repository.EmpresaRepository;
import ec.tusaas.efactura.repository.IdentidadRepository;
import ec.tusaas.efactura.repository.MembresiaEmpresaRepository;
import ec.tusaas.efactura.repository.MembresiaInvitacionRepository;
import ec.tusaas.efactura.repository.RolRepository;
import ec.tusaas.efactura.security.TokenHasher;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvitacionService {

  private final MembresiaInvitacionRepository membresiaInvitacionRepository;
  private final IdentidadRepository identidadRepository;
  private final MembresiaEmpresaRepository membresiaEmpresaRepository;
  private final EmpresaRepository empresaRepository;
  private final RolRepository rolRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuditoriaService auditoriaService;
  private final InvitacionProperties invitacionProperties;
  private final EmailNotificationService emailNotificationService;

  @Transactional(readOnly = true)
  public List<InvitacionPendienteResponse> listarPendientes(UUID empresaId, UsuarioPrincipal principal) {
    assertGestionInvitaciones(empresaId, principal);
    return membresiaInvitacionRepository
        .findAllByEmpresa_IdAndEstadoOrderByFechaCreacionDesc(empresaId, "PENDIENTE")
        .stream()
        .map(
            i ->
                new InvitacionPendienteResponse(
                    i.getId(),
                    i.getEmail(),
                    i.getRol().getCodigo(),
                    i.getExpiresAt(),
                    i.getInvitadoPorEmail(),
                    i.getFechaCreacion()))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<InvitacionResponse> listar(
      UUID empresaId, String estado, boolean incluirExpiradas, UsuarioPrincipal principal) {
    assertGestionInvitaciones(empresaId, principal);
    String filtroEstado = estado == null || estado.isBlank() ? null : estado.trim().toUpperCase();
    return membresiaInvitacionRepository.findAllByEmpresa_IdOrderByFechaCreacionDesc(empresaId).stream()
        .map(this::toResponse)
        .filter(inv -> incluirExpiradas || !"EXPIRADA".equals(inv.estado()))
        .filter(inv -> filtroEstado == null || filtroEstado.equals(inv.estado()))
        .toList();
  }

  @Transactional
  public void cancelar(UUID empresaId, UUID invitacionId, UsuarioPrincipal principal) {
    assertGestionInvitaciones(empresaId, principal);
    MembresiaInvitacion inv =
        membresiaInvitacionRepository
            .findById(invitacionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitación no encontrada"));
    if (!inv.getEmpresa().getId().equals(empresaId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invitación de otra empresa");
    }
    if (!"PENDIENTE".equalsIgnoreCase(inv.getEstado())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "La invitación ya no está pendiente");
    }
    inv.setEstado("CANCELADA");
    membresiaInvitacionRepository.save(inv);
    auditoriaService.registrar(
        "USUARIO_INVITACION_CANCELADA",
        empresaId,
        principal.getEmail(),
        "MembresiaInvitacion",
        inv.getId(),
        Map.of("email", inv.getEmail()));
  }

  @Transactional
  public InvitacionCreadaResponse invitar(
      UUID empresaId, InvitarUsuarioRequest req, UsuarioPrincipal principal) {
    assertGestionInvitaciones(empresaId, principal);

    String email = req.email().trim().toLowerCase();
    Optional<Identidad> existente = identidadRepository.findByEmailIgnoreCase(email);
    if (existente.isPresent()
        && membresiaEmpresaRepository.existsByIdentidadIdAndEmpresaId(existente.get().getId(), empresaId)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario ya pertenece a esta empresa");
    }

    Empresa empresa =
        empresaRepository.findById(empresaId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    Rol rol =
        rolRepository
            .findByEmpresaAndCodigo(empresa, req.rolCodigo())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol no encontrado"));
    if (rol.getEmpresa() == null || !Objects.equals(rol.getEmpresa().getId(), empresa.getId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol no pertenece a esta empresa");
    }

    String plain = TokenHasher.randomRefreshTokenPlain();
    String hash = TokenHasher.sha256Hex(plain);
    MembresiaInvitacion row = new MembresiaInvitacion();
    row.setEmail(email);
    row.setEmpresa(empresa);
    row.setRol(rol);
    row.setTokenHash(hash);
    row.setExpiresAt(
        Instant.now().plus(invitacionProperties.getExpiracionDias(), ChronoUnit.DAYS));
    String invitadoPor = nombreUsuario(principal);
    row.setInvitadoPorEmail(principal.getEmail());
    try {
      row = membresiaInvitacionRepository.save(row);
    } catch (DataIntegrityViolationException e) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Ya existe una invitación pendiente para ese email en la empresa");
    }

    auditoriaService.registrar(
        "USUARIO_INVITACION_CREADA",
        empresaId,
        principal.getEmail(),
        "MembresiaInvitacion",
        row.getId(),
        Map.of("email", email, "rol", req.rolCodigo()));

    if (invitacionProperties.isLogTokenServidor()) {
      log.info(
          "[invitacion] creada id={} email={} empresa={} token={} expira={}",
          row.getId(),
          email,
          empresa.getRazonSocial(),
          plain,
          row.getExpiresAt());
    }

    boolean emailEnviado =
        emailNotificationService.enviarInvitacion(
            empresaId, empresa, email, req.rolCodigo(), plain, row.getExpiresAt(), invitadoPor);

    return new InvitacionCreadaResponse(
        row.getId(), plain, row.getExpiresAt(), emailNotificationService.invitationUrl(plain), emailEnviado);
  }

  @Transactional
  public InvitacionCreadaResponse reenviar(UUID empresaId, UUID invitacionId, UsuarioPrincipal principal) {
    assertGestionInvitaciones(empresaId, principal);
    MembresiaInvitacion inv =
        membresiaInvitacionRepository
            .findById(invitacionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "InvitaciÃ³n no encontrada"));
    if (!inv.getEmpresa().getId().equals(empresaId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "InvitaciÃ³n de otra empresa");
    }
    if (!"PENDIENTE".equalsIgnoreCase(inv.getEstado())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "La invitaciÃ³n ya no estÃ¡ pendiente");
    }

    String plain = TokenHasher.randomRefreshTokenPlain();
    inv.setTokenHash(TokenHasher.sha256Hex(plain));
    inv.setExpiresAt(Instant.now().plus(invitacionProperties.getExpiracionDias(), ChronoUnit.DAYS));
    inv = membresiaInvitacionRepository.save(inv);

    boolean emailEnviado =
        emailNotificationService.enviarInvitacion(
            empresaId,
            inv.getEmpresa(),
            inv.getEmail(),
            inv.getRol().getCodigo(),
            plain,
            inv.getExpiresAt(),
            nombreUsuario(principal));
    auditoriaService.registrar(
        "USUARIO_INVITACION_REENVIADA",
        empresaId,
        principal.getEmail(),
        "MembresiaInvitacion",
        inv.getId(),
        Map.of("email", inv.getEmail(), "rol", inv.getRol().getCodigo()));
    return new InvitacionCreadaResponse(
        inv.getId(), plain, inv.getExpiresAt(), emailNotificationService.invitationUrl(plain), emailEnviado);
  }

  @Transactional
  public UUID aceptarInvitacion(AcceptInviteRequest req) {
    String hash = TokenHasher.sha256Hex(req.token().trim());
    MembresiaInvitacion inv =
        membresiaInvitacionRepository
            .findValidByTokenHash(hash, "PENDIENTE", Instant.now())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitación inválida o expirada"));

    Empresa empresa = inv.getEmpresa();
    Rol rol = inv.getRol();
    String email = inv.getEmail().trim().toLowerCase();

    Optional<Identidad> idOpt = identidadRepository.findByEmailIgnoreCase(email);
    Identidad identidad;
    if (idOpt.isEmpty()) {
      if (req.nombre() == null || req.nombre().isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre requerido para nueva cuenta");
      }
      identidad = new Identidad();
      identidad.setEmail(email);
      identidad.setPasswordHash(passwordEncoder.encode(req.password()));
      identidad.setNombre(req.nombre().trim());
      identidad = identidadRepository.save(identidad);
    } else {
      identidad = idOpt.get();
      assertIdentidadCanLogin(identidad);
      if (!passwordEncoder.matches(req.password(), identidad.getPasswordHash())) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Contraseña incorrecta");
      }
      if (membresiaEmpresaRepository.existsByIdentidadIdAndEmpresaId(identidad.getId(), empresa.getId())) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya pertenece a esta empresa");
      }
      if (req.nombre() != null && !req.nombre().isBlank()) {
        identidad.setNombre(req.nombre().trim());
      }
      identidadRepository.save(identidad);
    }

    MembresiaEmpresa m = new MembresiaEmpresa();
    m.setIdentidad(identidad);
    m.setEmpresa(empresa);
    m.setEstado("ACTIVO");
    m.getRoles().add(rol);
    m = membresiaEmpresaRepository.save(m);

    inv.setEstado("ACEPTADA");
    membresiaInvitacionRepository.save(inv);

    return m.getId();
  }

  private void assertGestionInvitaciones(UUID empresaId, UsuarioPrincipal principal) {
    if (principal.getEmpresaId() == null || !principal.getEmpresaId().equals(empresaId)) {
      boolean platform =
          principal.getAuthorities().stream().anyMatch(a -> "PLATFORM_ADMIN".equals(a.getAuthority()));
      if (!platform) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo miembros de la empresa o plataforma");
      }
    }
    if (!principal.getAuthorities().stream()
        .anyMatch(a -> "EMPRESA_ADMIN".equals(a.getAuthority()) || "PLATFORM_ADMIN".equals(a.getAuthority()))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Se requiere EMPRESA_ADMIN");
    }
  }

  private void assertIdentidadCanLogin(Identidad identidad) {
    if (!"ACTIVO".equalsIgnoreCase(identidad.getEstado())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario inactivo");
    }
    if (identidad.getBloqueadoHasta() != null && identidad.getBloqueadoHasta().isAfter(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario temporalmente bloqueado");
    }
  }

  private String nombreUsuario(UsuarioPrincipal principal) {
    return identidadRepository
        .findById(principal.getId())
        .map(Identidad::getNombre)
        .filter(nombre -> !nombre.isBlank())
        .orElse(principal.getEmail());
  }

  private InvitacionResponse toResponse(MembresiaInvitacion inv) {
    String estado = estadoEfectivo(inv);
    return new InvitacionResponse(
        inv.getId(),
        inv.getEmail(),
        inv.getRol().getCodigo(),
        estado,
        inv.getExpiresAt(),
        inv.getInvitadoPorEmail(),
        inv.getFechaCreacion(),
        "EXPIRADA".equals(estado));
  }

  private String estadoEfectivo(MembresiaInvitacion inv) {
    String estado = inv.getEstado() == null ? "PENDIENTE" : inv.getEstado().trim().toUpperCase();
    if ("PENDIENTE".equals(estado) && inv.getExpiresAt() != null && inv.getExpiresAt().isBefore(Instant.now())) {
      return "EXPIRADA";
    }
    return estado;
  }
}
