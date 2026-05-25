package ec.tusaas.efactura.service;

import ec.tusaas.efactura.config.props.JwtProperties;
import ec.tusaas.efactura.config.props.SuiteIdentityProperties;
import ec.tusaas.efactura.dto.auth.EmpresaLoginOptionDto;
import ec.tusaas.efactura.dto.auth.ActivateTemporaryPasswordRequest;
import ec.tusaas.efactura.dto.auth.LoginRequest;
import ec.tusaas.efactura.dto.auth.LoginResult;
import ec.tusaas.efactura.dto.auth.LogoutRequest;
import ec.tusaas.efactura.dto.auth.RefreshRequest;
import ec.tusaas.efactura.dto.auth.SelectEmpresaRequest;
import ec.tusaas.efactura.dto.auth.SwitchEmpresaRequest;
import ec.tusaas.efactura.dto.auth.TokenResponse;
import ec.tusaas.efactura.dto.invitacion.AcceptInviteRequest;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.entity.Identidad;
import ec.tusaas.efactura.entity.MembresiaEmpresa;
import ec.tusaas.efactura.entity.RefreshToken;
import ec.tusaas.efactura.repository.IdentidadRepository;
import ec.tusaas.efactura.repository.MembresiaEmpresaRepository;
import ec.tusaas.efactura.repository.RefreshTokenRepository;
import ec.tusaas.efactura.security.JwtService;
import ec.tusaas.efactura.security.SuiteIdentityJwtService;
import ec.tusaas.efactura.security.SuiteIdentityJwtService.SuiteIdentityAccessClaims;
import ec.tusaas.efactura.security.TokenHasher;
import ec.tusaas.efactura.security.TotpService;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.util.MembresiaPermisos;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

  public static final String AUD_AUTH_LOGIN = "AUTH_LOGIN";
  public static final String AUD_AUTH_SELECT_EMPRESA = "AUTH_SELECT_EMPRESA";
  public static final String AUD_AUTH_SWITCH_EMPRESA = "AUTH_SWITCH_EMPRESA";
  public static final String AUD_AUTH_REFRESH = "AUTH_REFRESH";
  public static final String AUD_AUTH_ACCEPT_INVITE = "AUTH_ACCEPT_INVITE";
  public static final String AUD_AUTH_ACTIVATE_TEMPORARY_PASSWORD = "AUTH_ACTIVATE_TEMPORARY_PASSWORD";

  public static final String AUD_AUTH_SUITE_EXCHANGE = "AUTH_SUITE_EXCHANGE";

  private final IdentidadRepository identidadRepository;
  private final MembresiaEmpresaRepository membresiaEmpresaRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final JwtProperties jwtProperties;
  private final SuiteIdentityJwtService suiteIdentityJwtService;
  private final SuiteIdentityProperties suiteIdentityProperties;
  private final InvitacionService invitacionService;
  private final AuditoriaService auditoriaService;
  private final TotpService totpService;

  @Transactional
  public LoginResult login(LoginRequest req) {
    String email = req.email().trim().toLowerCase();
    Identidad identidad =
        identidadRepository
            .findByEmailIgnoreCase(email)
            .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
    assertIdentidadCanLogin(identidad);
    if (!passwordEncoder.matches(req.password(), identidad.getPasswordHash())) {
      throw new BadCredentialsException("Credenciales inválidas");
    }
    if (identidad.isMfaHabilitado()) {
      if (req.mfaCode() == null || req.mfaCode().isBlank()) {
        return LoginResult.requireMfa();
      }
      if (!totpService.verify(identidad.getMfaSecret(), req.mfaCode().trim())) {
        throw new BadCredentialsException("Codigo MFA invalido");
      }
    }
    identidad.setUltimoLogin(Instant.now());

    List<MembresiaEmpresa> activas = membresiaEmpresaRepository.findActivasWithEmpresa(identidad.getId());
    List<EmpresaLoginOptionDto> opciones = activas.stream().map(this::toLoginOption).toList();
    List<EmpresaLoginOptionDto> seleccionables = opciones.stream().filter(EmpresaLoginOptionDto::seleccionable).toList();

    String ruc = req.ruc() == null || req.ruc().isBlank() ? null : req.ruc().trim();

    if (ruc != null) {
      EmpresaLoginOptionDto match =
          seleccionables.stream()
              .filter(o -> !o.esPlataforma() && ruc.equals(o.ruc()))
              .findFirst()
              .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
      MembresiaEmpresa m = resolveMembresiaForOption(identidad.getId(), match);
      return LoginResult.complete(emitTokens(m, AUD_AUTH_LOGIN));
    }

    if (seleccionables.isEmpty()) {
      if (opciones.isEmpty()) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN, "No tiene empresas asignadas con membresía activa");
      }
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "Ninguna empresa está disponible para acceso en este momento (revisar estado de empresa o membresía)");
    }

    if (seleccionables.size() == 1) {
      MembresiaEmpresa m = resolveMembresiaForOption(identidad.getId(), seleccionables.get(0));
      return LoginResult.complete(emitTokens(m, AUD_AUTH_LOGIN));
    }

    String ticket = jwtService.createSelectEmpresaTicket(identidad.getId());
    return LoginResult.selectEmpresa(ticket, opciones);
  }

  @Transactional
  public LoginResult selectEmpresa(SelectEmpresaRequest req) {
    UUID identidadId;
    try {
      identidadId = jwtService.parseSelectEmpresaTicket(req.sessionTicket().trim());
    } catch (JwtException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Ticket inválido o expirado");
    }
    Identidad identidad =
        identidadRepository
            .findById(identidadId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identidad no encontrada"));
    assertIdentidadCanLogin(identidad);

    MembresiaEmpresa m;
    if (req.empresaId() == null) {
      m =
          membresiaEmpresaRepository
              .findPlataformaByIdentidad(identidadId)
              .filter(x -> "ACTIVO".equalsIgnoreCase(x.getEstado()))
              .orElseThrow(
                  () ->
                      new ResponseStatusException(
                          HttpStatus.BAD_REQUEST, "No tiene membresía activa de plataforma"));
    } else {
      m =
          membresiaEmpresaRepository
              .findTenantByIdentidadAndEmpresa(identidadId, req.empresaId())
              .filter(x -> "ACTIVO".equalsIgnoreCase(x.getEstado()))
              .orElseThrow(
                  () -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Sin membresía activa en esa empresa"));
    }
    EmpresaLoginOptionDto opt = toLoginOption(m);
    if (!opt.seleccionable()) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          opt.motivoNoSeleccion() == null ? "Empresa no disponible" : opt.motivoNoSeleccion());
    }
    m =
        membresiaEmpresaRepository
            .findWithPermisosById(m.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Membresía"));
    return LoginResult.complete(emitTokens(m, AUD_AUTH_SELECT_EMPRESA));
  }

  @Transactional
  public TokenResponse switchEmpresa(String authorizationHeader, SwitchEmpresaRequest req) {
    String raw = bearerValue(authorizationHeader);
    Claims claims;
    try {
      claims = jwtService.parseAndVerify(raw);
    } catch (JwtException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido");
    }
    if (!JwtService.TYP_ACCESS.equals(claims.get(JwtService.CLAIM_TYP, String.class))) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token no es de acceso");
    }
    Date issuedAt = claims.getIssuedAt();
    if (issuedAt != null && jwtProperties.getSwitchMaxTokenAgeMinutes() > 0) {
      Instant limite = issuedAt.toInstant().plus(jwtProperties.getSwitchMaxTokenAgeMinutes(), ChronoUnit.MINUTES);
      if (limite.isBefore(Instant.now())) {
        throw new ResponseStatusException(
            HttpStatus.UNAUTHORIZED, "El token es demasiado antiguo para cambiar de empresa; vuelva a autenticarse");
      }
    }
    UUID identidadId = UUID.fromString(claims.getSubject());
    Identidad identidad =
        identidadRepository
            .findById(identidadId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identidad no encontrada"));
    if (jwtProperties.isSwitchRequireMfa() && identidad.isMfaHabilitado()) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED,
          "Cambio de empresa requiere reautenticación con MFA habilitado (configuración del servidor)");
    }

    MembresiaEmpresa m;
    if (req.empresaId() == null) {
      m =
          membresiaEmpresaRepository
              .findPlataformaByIdentidad(identidadId)
              .filter(x -> "ACTIVO".equalsIgnoreCase(x.getEstado()))
              .orElseThrow(
                  () -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Sin membresía de plataforma activa"));
    } else {
      m =
          membresiaEmpresaRepository
              .findTenantByIdentidadAndEmpresa(identidadId, req.empresaId())
              .filter(x -> "ACTIVO".equalsIgnoreCase(x.getEstado()))
              .orElseThrow(
                  () -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Sin membresía activa en esa empresa"));
    }
    EmpresaLoginOptionDto opt = toLoginOption(m);
    if (!opt.seleccionable()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Empresa no disponible para este contexto");
    }
    m =
        membresiaEmpresaRepository
            .findWithPermisosById(m.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Membresía"));
    return emitTokens(m, AUD_AUTH_SWITCH_EMPRESA);
  }

  @Transactional
  public TokenResponse refresh(RefreshRequest req) {
    String hash = TokenHasher.sha256Hex(req.refreshToken().trim());
    RefreshToken rt =
        refreshTokenRepository
            .findValidByHash(hash, Instant.now())
            .orElseThrow(() -> new BadCredentialsException("Refresh token inválido o expirado"));
    Identidad identidad = rt.getIdentidad();
    assertIdentidadCanLogin(identidad);
    rt.setRevoked(true);
    refreshTokenRepository.save(rt);

    UUID empresaId = rt.getEmpresa() == null ? null : rt.getEmpresa().getId();
    MembresiaEmpresa m =
        resolveMembresiaSession(identidad.getId(), empresaId)
            .orElseThrow(() -> new BadCredentialsException("Sesión de refresh inconsistente"));
    m =
        membresiaEmpresaRepository
            .findWithPermisosById(m.getId())
            .orElseThrow(() -> new BadCredentialsException("Membresía no encontrada"));

    return emitTokens(m, AUD_AUTH_REFRESH);
  }

  @Transactional
  public TokenResponse acceptInvite(AcceptInviteRequest req) {
    UUID membresiaId = invitacionService.aceptarInvitacion(req);
    MembresiaEmpresa m =
        membresiaEmpresaRepository
            .findWithPermisosById(membresiaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Membresía"));
    return emitTokens(m, AUD_AUTH_ACCEPT_INVITE);
  }

  @Transactional
  public TokenResponse activateTemporaryPassword(ActivateTemporaryPasswordRequest req) {
    String email = req.email().trim().toLowerCase();
    Identidad identidad =
        identidadRepository
            .findByEmailIgnoreCase(email)
            .orElseThrow(() -> new BadCredentialsException("Credenciales invalidas"));
    assertIdentidadCanLogin(identidad);
    if (!passwordEncoder.matches(req.passwordTemporal(), identidad.getPasswordHash())) {
      throw new BadCredentialsException("Credenciales invalidas");
    }
    List<MembresiaEmpresa> pendientes =
        membresiaEmpresaRepository.findAllByIdentidadEstadoAndOptionalEmpresa(
            identidad.getId(), "PENDIENTE_CONFIRMACION", req.empresaId());
    if (pendientes.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "No existe usuario pendiente de confirmacion");
    }
    if (req.empresaId() == null && pendientes.size() > 1) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Debe especificar empresaId para activar una membresia pendiente");
    }
    MembresiaEmpresa m = pendientes.get(0);
    identidad.setPasswordHash(passwordEncoder.encode(req.passwordNuevo()));
    identidad.setFechaModificacion(Instant.now());
    identidad.setUsuarioModificacion(email);
    identidadRepository.save(identidad);

    m.setEstado("ACTIVO");
    m.setFechaAceptacion(Instant.now());
    m.setFechaModificacion(Instant.now());
    m.setUsuarioModificacion(email);
    membresiaEmpresaRepository.save(m);

    m =
        membresiaEmpresaRepository
            .findWithPermisosById(m.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Membresia"));
    return emitTokens(m, AUD_AUTH_ACTIVATE_TEMPORARY_PASSWORD);
  }

  @Transactional
  public TokenResponse exchangeSuiteIdentityToken(String authorizationHeader) {
    if (!suiteIdentityProperties.isEnabled() || !suiteIdentityJwtService.isConfigured()) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Canje Identity Suite deshabilitado o no configurado en el servidor");
    }
    String raw = bearerValue(authorizationHeader);
    final SuiteIdentityAccessClaims claims;
    try {
      claims = suiteIdentityJwtService.parseAccessToken(raw);
    } catch (JwtException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Suite inválido o expirado");
    }
    if (!claims.canAccessEfactura()) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Token sin alcance efactura.access o suite.admin");
    }
    if (claims.email() == null || claims.email().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token sin email");
    }
    String email = claims.email().trim().toLowerCase();
    Identidad identidad =
        identidadRepository
            .findByEmailIgnoreCase(email)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Identidad no registrada en eFactura para este correo"));
    assertIdentidadCanLogin(identidad);
    if (identidad.isMfaHabilitado()) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Usuarios con MFA deben usar el login tradicional de eFactura");
    }
    MembresiaEmpresa m =
        membresiaEmpresaRepository
            .findActiveTenantBySuiteTenantId(identidad.getId(), claims.companyId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Sin membresía activa para el tenant del token; vincule empresa.id o empresa.suite_company_id al UUID de la compañía en Identity (claim company_id)"));
    m =
        membresiaEmpresaRepository
            .findWithPermisosById(m.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Membresía"));
    return emitTokens(m, AUD_AUTH_SUITE_EXCHANGE);
  }

  @Transactional
  public void logout(UsuarioPrincipal principal, LogoutRequest req) {
    if (req != null && req.refreshToken() != null && !req.refreshToken().isBlank()) {
      String hash = TokenHasher.sha256Hex(req.refreshToken().trim());
      refreshTokenRepository
          .findValidByHash(hash, Instant.now())
          .ifPresent(
              rt -> {
                if (!rt.getIdentidad().getId().equals(principal.getId())) {
                  throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Token no pertenece al usuario");
                }
                rt.setRevoked(true);
                refreshTokenRepository.save(rt);
              });
    }
  }

  private TokenResponse emitTokens(MembresiaEmpresa membresiaConPermisos, String accionAuditoria) {
    Identidad identidad = membresiaConPermisos.getIdentidad();
    List<String> authorities = MembresiaPermisos.codigos(membresiaConPermisos);
    UUID empresaId =
        membresiaConPermisos.getEmpresa() == null ? null : membresiaConPermisos.getEmpresa().getId();
    String access =
        jwtService.createAccessToken(identidad.getId(), empresaId, identidad.getEmail(), authorities);
    String refreshPlain = TokenHasher.randomRefreshTokenPlain();
    String hash = TokenHasher.sha256Hex(refreshPlain);
    RefreshToken rt = new RefreshToken();
    rt.setIdentidad(identidad);
    rt.setEmpresa(membresiaConPermisos.getEmpresa());
    rt.setTokenHash(hash);
    rt.setExpiresAt(Instant.now().plusSeconds(jwtProperties.getRefreshTokenDays() * 86400L));
    refreshTokenRepository.save(rt);

    if (accionAuditoria != null) {
      UUID empAudit = empresaId;
      Map<String, Object> detalle = new HashMap<>();
      detalle.put("identidadId", identidad.getId().toString());
      detalle.put("membresiaId", membresiaConPermisos.getId().toString());
      auditoriaService.registrar(
          accionAuditoria, empAudit, identidad.getEmail(), "Sesion", membresiaConPermisos.getId(), detalle);
    }

    return toTokenResponse(access, refreshPlain);
  }

  private Optional<MembresiaEmpresa> resolveMembresiaSession(UUID identidadId, UUID empresaId) {
    if (empresaId == null) {
      return membresiaEmpresaRepository.findPlataformaByIdentidad(identidadId);
    }
    return membresiaEmpresaRepository.findTenantByIdentidadAndEmpresa(identidadId, empresaId);
  }

  private MembresiaEmpresa resolveMembresiaForOption(UUID identidadId, EmpresaLoginOptionDto opt) {
    MembresiaEmpresa m;
    if (opt.esPlataforma()) {
      m =
          membresiaEmpresaRepository
              .findPlataformaByIdentidad(identidadId)
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Membresía plataforma"));
    } else {
      m =
          membresiaEmpresaRepository
              .findTenantByIdentidadAndEmpresa(identidadId, opt.empresaId())
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Membresía tenant"));
    }
    return membresiaEmpresaRepository
        .findWithPermisosById(m.getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Permisos"));
  }

  private EmpresaLoginOptionDto toLoginOption(MembresiaEmpresa m) {
    if (m.getEmpresa() == null) {
      boolean ok = "ACTIVO".equalsIgnoreCase(m.getEstado());
      return new EmpresaLoginOptionDto(
          null,
          true,
          "Administración plataforma",
          null,
          null,
          null,
          ok,
          ok ? null : "Membresía no activa");
    }
    Empresa e = m.getEmpresa();
    String motivo = null;
    boolean sel = "ACTIVO".equalsIgnoreCase(m.getEstado()) && "ACTIVO".equalsIgnoreCase(e.getEstado());
    if (!"ACTIVO".equalsIgnoreCase(m.getEstado())) {
      motivo = "Membresía no activa";
    } else if (!"ACTIVO".equalsIgnoreCase(e.getEstado())) {
      motivo = "Empresa inactiva";
    }
    return new EmpresaLoginOptionDto(
        e.getId(),
        false,
        e.getRazonSocial(),
        e.getNombreComercial(),
        e.getRuc(),
        e.getSlug(),
        sel,
        sel ? null : motivo);
  }

  private void assertIdentidadCanLogin(Identidad identidad) {
    if (!"ACTIVO".equalsIgnoreCase(identidad.getEstado())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario inactivo");
    }
    if (identidad.getBloqueadoHasta() != null && identidad.getBloqueadoHasta().isAfter(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario temporalmente bloqueado");
    }
  }

  private TokenResponse toTokenResponse(String access, String refresh) {
    return new TokenResponse(
        "Bearer",
        access,
        refresh,
        jwtProperties.getAccessTokenMinutes() * 60L);
  }

  private static String bearerValue(String authorizationHeader) {
    if (authorizationHeader == null || !authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization Bearer requerido");
    }
    return authorizationHeader.substring(7).trim();
  }
}
