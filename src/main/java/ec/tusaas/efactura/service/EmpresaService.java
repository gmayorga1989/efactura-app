package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.empresa.EmpresaCreateRequest;
import ec.tusaas.efactura.dto.empresa.EmpresaResponse;
import ec.tusaas.efactura.dto.empresa.EmpresaUpdateRequest;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.entity.Plan;
import ec.tusaas.efactura.entity.Suscripcion;
import ec.tusaas.efactura.mapper.EmpresaMapper;
import ec.tusaas.efactura.repository.EmpresaRepository;
import ec.tusaas.efactura.repository.PlanRepository;
import ec.tusaas.efactura.repository.SuscripcionRepository;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class EmpresaService {

  private final EmpresaRepository empresaRepository;
  private final PlanRepository planRepository;
  private final SuscripcionRepository suscripcionRepository;
  private final EmpresaMapper empresaMapper;
  private final TenantDatasourceConfigService tenantDatasourceConfigService;
  private final DashboardCacheService dashboardCacheService;

  @Transactional(readOnly = true)
  public EmpresaResponse obtener(UUID id, UsuarioPrincipal principal) {
    Empresa e = empresaRepository.findById(id).orElseThrow(notFound());
    assertCanReadEmpresa(principal, e);
    return empresaMapper.toResponse(e);
  }

  @Transactional
  public EmpresaResponse crear(EmpresaCreateRequest req) {
    if (empresaRepository.existsByRuc(req.ruc())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "RUC ya registrado");
    }
    Empresa e = new Empresa();
    e.setRuc(req.ruc());
    e.setRazonSocial(req.razonSocial());
    e.setNombreComercial(req.nombreComercial());
    e.setObligadoContabilidad(req.obligadoContabilidad());
    e.setContribuyenteEspecial(req.contribuyenteEspecial());
    e.setExportadorHabitual(req.exportadorHabitual());
    e.setCalificacionArtesanal(req.calificacionArtesanal());
    e.setCodigoArtesano(normalizeOptional(req.codigoArtesano()));
    e.setAgenteRetencion(req.agenteRetencion());
    e.setDireccionMatriz(req.direccionMatriz());
    if (req.timezone() != null && !req.timezone().isBlank()) {
      e.setTimezone(req.timezone());
    }
    validarCalificacionArtesanal(e);
    e = empresaRepository.save(e);
    Plan starter =
        planRepository
            .findByCodigo("STARTER")
            .orElseThrow(() -> new IllegalStateException("Plan STARTER no encontrado en BD"));
    Suscripcion s = new Suscripcion();
    s.setEmpresa(e);
    s.setPlan(starter);
    s.setFechaInicio(LocalDate.now());
    s.setEstado("ACTIVA");
    suscripcionRepository.save(s);
    tenantDatasourceConfigService.asegurarShared(e, "system");
    return empresaMapper.toResponse(e);
  }

  @Transactional
  public EmpresaResponse actualizar(UUID id, EmpresaUpdateRequest req, UsuarioPrincipal principal) {
    Empresa e = empresaRepository.findById(id).orElseThrow(notFound());
    assertCanManageEmpresa(principal, e);

    if (req.ruc() != null && !req.ruc().equals(e.getRuc())) {
      if (empresaRepository.existsByRuc(req.ruc())) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "RUC ya registrado");
      }
      e.setRuc(req.ruc());
    }
    if (req.razonSocial() != null) {
      if (req.razonSocial().isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Razon social requerida");
      }
      e.setRazonSocial(req.razonSocial());
    }
    if (req.nombreComercial() != null) {
      e.setNombreComercial(normalizeOptional(req.nombreComercial()));
    }
    if (req.obligadoContabilidad() != null) {
      e.setObligadoContabilidad(req.obligadoContabilidad());
    }
    if (req.contribuyenteEspecial() != null) {
      e.setContribuyenteEspecial(normalizeOptional(req.contribuyenteEspecial()));
    }
    if (req.exportadorHabitual() != null) {
      e.setExportadorHabitual(req.exportadorHabitual());
    }
    if (req.calificacionArtesanal() != null) {
      e.setCalificacionArtesanal(req.calificacionArtesanal());
    }
    if (req.codigoArtesano() != null) {
      e.setCodigoArtesano(normalizeOptional(req.codigoArtesano()));
    }
    if (req.agenteRetencion() != null) {
      e.setAgenteRetencion(req.agenteRetencion());
    }
    if (!e.isCalificacionArtesanal()) {
      e.setCodigoArtesano(null);
    }
    if (req.direccionMatriz() != null) {
      e.setDireccionMatriz(normalizeOptional(req.direccionMatriz()));
    }
    if (req.timezone() != null && !req.timezone().isBlank()) {
      e.setTimezone(req.timezone());
    }
    if (req.paisIso() != null && !req.paisIso().isBlank()) {
      e.setPaisIso(req.paisIso().trim().toUpperCase(java.util.Locale.ROOT));
    }

    validarCalificacionArtesanal(e);
    e.setFechaModificacion(java.time.Instant.now());
    e.setUsuarioModificacion(principal.getEmail());
    Empresa saved = empresaRepository.save(e);
    dashboardCacheService.evictEmpresa(id);
    return empresaMapper.toResponse(saved);
  }

  private void assertCanReadEmpresa(UsuarioPrincipal principal, Empresa e) {
    if (principal.getEmpresaId() == null) {
      if (!principal.getAuthorities().stream()
          .anyMatch(a -> "PLATFORM_ADMIN".equals(a.getAuthority()))) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sin permiso");
      }
      return;
    }
    if (!principal.getEmpresaId().equals(e.getId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puede consultar su empresa");
    }
  }

  private void assertCanManageEmpresa(UsuarioPrincipal principal, Empresa e) {
    if (principal.getAuthorities().stream().anyMatch(a -> "PLATFORM_ADMIN".equals(a.getAuthority()))) {
      return;
    }
    if (principal.getEmpresaId() != null
        && principal.getEmpresaId().equals(e.getId())
        && principal.getAuthorities().stream().anyMatch(a -> "EMPRESA_ADMIN".equals(a.getAuthority()))) {
      return;
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Se requiere EMPRESA_ADMIN o PLATFORM_ADMIN");
  }

  private void validarCalificacionArtesanal(Empresa e) {
    if (e.isCalificacionArtesanal() && (e.getCodigoArtesano() == null || e.getCodigoArtesano().isBlank())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Codigo de artesano requerido para calificacion artesanal");
    }
  }

  private static String normalizeOptional(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private static java.util.function.Supplier<ResponseStatusException> notFound() {
    return () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada");
  }
}
