package ec.tusaas.efactura.service.suite;

import ec.tusaas.efactura.config.props.SuiteIdentityProperties;
import ec.tusaas.efactura.dto.auth.SuiteTenantBootstrapRequest;
import ec.tusaas.efactura.dto.auth.SuiteTenantBootstrapResultDto;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.entity.Identidad;
import ec.tusaas.efactura.entity.MembresiaEmpresa;
import ec.tusaas.efactura.entity.Plan;
import ec.tusaas.efactura.entity.Rol;
import ec.tusaas.efactura.entity.Suscripcion;
import ec.tusaas.efactura.repository.EmpresaRepository;
import ec.tusaas.efactura.repository.IdentidadRepository;
import ec.tusaas.efactura.repository.MembresiaEmpresaRepository;
import ec.tusaas.efactura.repository.PermisoRepository;
import ec.tusaas.efactura.repository.PlanRepository;
import ec.tusaas.efactura.repository.RolRepository;
import ec.tusaas.efactura.repository.SuscripcionRepository;
import ec.tusaas.efactura.security.SuiteIdentityJwtService;
import ec.tusaas.efactura.security.SuiteIdentityJwtService.SuiteIdentityAccessClaims;
import ec.tusaas.efactura.service.TenantDatasourceConfigService;
import io.jsonwebtoken.JwtException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SuiteTenantProvisioningService {

  private static final Pattern PASSWORD_HAS_LETTER = Pattern.compile(".*[A-Za-zÁÉÍÓÚÜÑáéíóúüñ].*");
  private static final Pattern PASSWORD_HAS_DIGIT = Pattern.compile(".*\\d.*");
  private static final Pattern RUC_NUMERIC = Pattern.compile("^\\d{13}$");

  private static final List<String> ADMIN_PERMISO_CODIGOS =
      List.of(
          "EMPRESA_ADMIN",
          "FACTURA_EMITIR",
          "COMPROBANTE_MONITOR",
          "REPORTE_VER",
          "PROVEEDOR_GESTIONAR",
          "VENTAS_GESTIONAR");

  private final SuiteIdentityProperties suiteIdentityProperties;
  private final SuiteIdentityJwtService suiteIdentityJwtService;
  private final IdentidadRepository identidadRepository;
  private final EmpresaRepository empresaRepository;
  private final MembresiaEmpresaRepository membresiaEmpresaRepository;
  private final RolRepository rolRepository;
  private final PermisoRepository permisoRepository;
  private final PlanRepository planRepository;
  private final SuscripcionRepository suscripcionRepository;
  private final TenantDatasourceConfigService tenantDatasourceConfigService;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public SuiteTenantBootstrapResultDto bootstrapFromSuiteToken(
      String authorizationHeader, SuiteTenantBootstrapRequest body) {
    if (!suiteIdentityProperties.isPublicBootstrapEnabled()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aprovisionamiento Suite desactivado");
    }
    if (!suiteIdentityProperties.isEnabled() || !suiteIdentityJwtService.isConfigured()) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Integración Suite Identity no configurada en eFactura");
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
    validatePassword(body.getPassword());

    String email = claims.email().trim().toLowerCase(Locale.ROOT);
    UUID suiteCompanyId = claims.companyId();

    AtomicBoolean empresaNueva = new AtomicBoolean(false);
    Empresa empresa =
        empresaRepository
            .findBySuiteCompanyId(suiteCompanyId)
            .orElseGet(
                () -> {
                  empresaNueva.set(true);
                  String ruc = resolveRucForNewEmpresa(body.getRuc(), suiteCompanyId);
                  String slug = resolveEmpresaSlug(body.getEmpresaSlug(), body.getRazonSocial(), suiteCompanyId);
                  Empresa e = new Empresa();
                  e.setRuc(ruc);
                  e.setSlug(slug);
                  e.setRazonSocial(body.getRazonSocial().trim());
                  e.setNombreComercial(body.getRazonSocial().trim());
                  e.setSuiteCompanyId(suiteCompanyId);
                  e.setUsuarioCreacion("suite-bootstrap");
                  Empresa saved = empresaRepository.save(e);
                  tenantDatasourceConfigService.asegurarShared(saved, "suite-bootstrap");
                  Plan starter =
                      planRepository
                          .findByCodigo("STARTER")
                          .orElseThrow(
                              () ->
                                  new ResponseStatusException(
                                      HttpStatus.INTERNAL_SERVER_ERROR, "Plan STARTER no encontrado"));
                  Suscripcion suscripcion = new Suscripcion();
                  suscripcion.setEmpresa(saved);
                  suscripcion.setPlan(starter);
                  suscripcion.setFechaInicio(LocalDate.now());
                  suscripcion.setEstado("ACTIVA");
                  suscripcionRepository.save(suscripcion);
                  return saved;
                });

    if (!empresaNueva.get() && (empresa.getSuiteCompanyId() == null || !suiteCompanyId.equals(empresa.getSuiteCompanyId()))) {
      empresa.setSuiteCompanyId(suiteCompanyId);
      empresaRepository.save(empresa);
    }

    AtomicBoolean identidadNueva = new AtomicBoolean(false);
    Identidad identidad =
        identidadRepository
            .findByEmailIgnoreCase(email)
            .orElseGet(
                () -> {
                  identidadNueva.set(true);
                  Identidad i = new Identidad();
                  i.setEmail(email);
                  i.setPasswordHash(passwordEncoder.encode(body.getPassword()));
                  String nombre = body.getRazonSocial().trim();
                  if (nombre.length() > 200) {
                    nombre = nombre.substring(0, 200);
                  }
                  i.setNombre(nombre);
                  i.setUsuarioCreacion("suite-bootstrap");
                  return identidadRepository.save(i);
                });

    Rol rolAdmin = buildOrFindAdminRol(empresa);

    AtomicBoolean membresiaNueva = new AtomicBoolean(false);
    MembresiaEmpresa membresia =
        resolveOrCreateMembresia(identidad, empresa, rolAdmin, suiteCompanyId, membresiaNueva);

    return new SuiteTenantBootstrapResultDto(
        empresaNueva.get(),
        identidadNueva.get(),
        membresiaNueva.get(),
        empresa.getId(),
        identidad.getId(),
        membresia.getId(),
        empresa.getRuc(),
        empresa.getSlug());
  }

  private MembresiaEmpresa resolveOrCreateMembresia(
      Identidad identidad,
      Empresa empresa,
      Rol rolAdmin,
      UUID suiteCompanyId,
      AtomicBoolean membresiaNueva) {
    Optional<MembresiaEmpresa> existing = membresiaEmpresaRepository.findByIdentidadAndEmpresa(identidad, empresa);
    if (existing.isPresent()) {
      MembresiaEmpresa m = existing.get();
      if (!"ACTIVO".equalsIgnoreCase(m.getEstado())) {
        m.setEstado("ACTIVO");
        m.setFechaAceptacion(Instant.now());
        m.setUsuarioModificacion("suite-bootstrap");
        membresiaEmpresaRepository.save(m);
      }
      if (m.getRoles().isEmpty()) {
        m.getRoles().add(rolAdmin);
        membresiaEmpresaRepository.save(m);
      }
      return m;
    }
    return membresiaEmpresaRepository
        .findActiveTenantBySuiteTenantId(identidad.getId(), suiteCompanyId)
        .orElseGet(
            () -> {
              membresiaNueva.set(true);
              MembresiaEmpresa m = new MembresiaEmpresa();
              m.setIdentidad(identidad);
              m.setEmpresa(empresa);
              m.setEstado("ACTIVO");
              m.setUsuarioCreacion("suite-bootstrap");
              m.getRoles().add(rolAdmin);
              return membresiaEmpresaRepository.save(m);
            });
  }

  private Rol buildOrFindAdminRol(Empresa empresa) {
    return rolRepository
        .findByEmpresaAndCodigo(empresa, "ADMIN")
        .orElseGet(
            () -> {
              Rol rolAdmin = new Rol();
              rolAdmin.setEmpresa(empresa);
              rolAdmin.setCodigo("ADMIN");
              rolAdmin.setNombre("Administrador empresa");
              rolAdmin.setSistema(true);
              for (String codigo : ADMIN_PERMISO_CODIGOS) {
                permisoRepository.findByCodigo(codigo).ifPresent(p -> rolAdmin.getPermisos().add(p));
              }
              return rolRepository.save(rolAdmin);
            });
  }

  private static void validatePassword(String password) {
    if (password == null || password.length() < 10) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "La contraseña debe tener al menos 10 caracteres");
    }
    if (!PASSWORD_HAS_LETTER.matcher(password).matches()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña debe incluir al menos una letra");
    }
    if (!PASSWORD_HAS_DIGIT.matcher(password).matches()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña debe incluir al menos un dígito");
    }
  }

  private String resolveRucForNewEmpresa(String rucInput, UUID suiteCompanyId) {
    if (rucInput != null && !rucInput.isBlank()) {
      String r = rucInput.trim();
      if (!RUC_NUMERIC.matcher(r).matches()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "RUC debe tener 13 dígitos");
      }
      if (empresaRepository.existsByRuc(r)) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "RUC ya registrado en eFactura");
      }
      return r;
    }
    for (int salt = 0; salt < 50; salt++) {
      long mix = suiteCompanyId.getMostSignificantBits() ^ suiteCompanyId.getLeastSignificantBits() ^ salt;
      long tenDigits = Math.floorMod(mix, 10_000_000_000L);
      String candidate = String.format("999%010d", tenDigits);
      if (!empresaRepository.existsByRuc(candidate)) {
        return candidate;
      }
    }
    throw new ResponseStatusException(
        HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar un RUC provisional único");
  }

  private String resolveEmpresaSlug(String preferred, String razonSocial, UUID suiteCompanyId) {
    String base =
        slugify(preferred != null && !preferred.isBlank() ? preferred : razonSocial + "-" + suiteCompanyId);
    if (base.length() < 2) {
      base = "empresa-" + suiteCompanyId.toString().substring(0, 8);
    }
    String candidate = base;
    for (int i = 0; i < 80; i++) {
      if (empresaRepository.findBySlugIgnoreCase(candidate).isEmpty()) {
        return candidate;
      }
      candidate = base + "-" + (i + 1);
    }
    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo asignar slug único");
  }

  private static String slugify(String raw) {
    String s = raw.trim().toLowerCase(Locale.ROOT);
    s = s.replaceAll("[^a-z0-9]+", "-");
    s = s.replaceAll("^-+", "").replaceAll("-+$", "");
    if (s.length() > 80) {
      s = s.substring(0, 80).replaceAll("-+$", "");
    }
    return s;
  }

  private static String bearerValue(String authorizationHeader) {
    if (authorizationHeader == null || !authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization Bearer requerido");
    }
    return authorizationHeader.substring(7).trim();
  }
}
