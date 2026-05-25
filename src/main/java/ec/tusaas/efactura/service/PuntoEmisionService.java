package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.tributario.PuntoEmisionRequest;
import ec.tusaas.efactura.dto.tributario.PuntoEmisionResponse;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.entity.Establecimiento;
import ec.tusaas.efactura.entity.PuntoEmision;
import ec.tusaas.efactura.entity.Secuencial;
import ec.tusaas.efactura.repository.ComprobanteRepository;
import ec.tusaas.efactura.repository.EmpresaRepository;
import ec.tusaas.efactura.repository.EstablecimientoRepository;
import ec.tusaas.efactura.repository.PuntoEmisionRepository;
import ec.tusaas.efactura.repository.SecuencialRepository;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.tributario.TiposComprobanteSri;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PuntoEmisionService {

  private final PuntoEmisionRepository puntoEmisionRepository;
  private final EstablecimientoRepository establecimientoRepository;
  private final EmpresaRepository empresaRepository;
  private final SecuencialRepository secuencialRepository;
  private final ComprobanteRepository comprobanteRepository;
  private final EmpresaTributarioService empresaTributarioService;
  private final DashboardCacheService dashboardCacheService;

  @Transactional(readOnly = true)
  public List<PuntoEmisionResponse> listar(UUID empresaId, UUID establecimientoId, UsuarioPrincipal principal) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    establecimientoRepository
        .findByIdAndEmpresa_Id(establecimientoId, empresaId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    return puntoEmisionRepository.findByEstablecimiento_IdOrderByCodigoAsc(establecimientoId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public PuntoEmisionResponse crear(
      UUID empresaId, UUID establecimientoId, PuntoEmisionRequest req, UsuarioPrincipal principal) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    Empresa empresa =
        empresaRepository.findById(empresaId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    Establecimiento est =
        establecimientoRepository
            .findByIdAndEmpresa_Id(establecimientoId, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    puntoEmisionRepository
        .findByEstablecimiento_IdAndCodigo(establecimientoId, req.codigo())
        .ifPresent(
            x -> {
              throw new ResponseStatusException(HttpStatus.CONFLICT, "Código de punto de emisión ya existe");
            });
    PuntoEmision pe = new PuntoEmision();
    pe.setEmpresa(empresa);
    pe.setEstablecimiento(est);
    pe.setCodigo(req.codigo());
    pe.setNombre(req.nombre());
    pe.setUsuarioCreacion(principal.getEmail());
    pe = puntoEmisionRepository.saveAndFlush(pe);
    crearSecuencialesIniciales(empresa, pe);
    dashboardCacheService.evictEmpresa(empresaId);
    return toResponse(pe);
  }

  @Transactional
  public PuntoEmisionResponse actualizar(
      UUID empresaId, UUID establecimientoId, UUID id, PuntoEmisionRequest req, UsuarioPrincipal principal) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    establecimientoRepository
        .findByIdAndEmpresa_Id(establecimientoId, empresaId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    PuntoEmision pe =
        puntoEmisionRepository
            .findByIdAndEmpresa_Id(id, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!pe.getEstablecimiento().getId().equals(establecimientoId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El punto no pertenece al establecimiento");
    }
    boolean tieneComprobantes = tieneComprobantes(empresaId, pe);
    if (tieneComprobantes && !pe.getCodigo().equals(req.codigo())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "El punto de emision ya tiene documentos generados; solo se puede cambiar el nombre");
    }
    if (!pe.getCodigo().equals(req.codigo())) {
      puntoEmisionRepository
          .findByEstablecimiento_IdAndCodigo(establecimientoId, req.codigo())
          .ifPresent(
              otro -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Código de punto de emisión ya existe");
              });
    }
    pe.setCodigo(req.codigo());
    pe.setNombre(req.nombre());
    pe.setFechaModificacion(Instant.now());
    pe.setUsuarioModificacion(principal.getEmail());
    PuntoEmision saved = puntoEmisionRepository.save(pe);
    dashboardCacheService.evictEmpresa(empresaId);
    return toResponse(saved);
  }

  @Transactional
  public PuntoEmisionResponse cambiarEstado(
      UUID empresaId, UUID establecimientoId, UUID id, String estado, UsuarioPrincipal principal) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    establecimientoRepository
        .findByIdAndEmpresa_Id(establecimientoId, empresaId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    PuntoEmision pe =
        puntoEmisionRepository
            .findByIdAndEmpresa_Id(id, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!pe.getEstablecimiento().getId().equals(establecimientoId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El punto no pertenece al establecimiento");
    }
    pe.setEstado(estado);
    pe.setFechaModificacion(Instant.now());
    pe.setUsuarioModificacion(principal.getEmail());
    PuntoEmision saved = puntoEmisionRepository.save(pe);
    dashboardCacheService.evictEmpresa(empresaId);
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public PuntoEmisionResponse obtener(UUID empresaId, UUID establecimientoId, UUID id, UsuarioPrincipal principal) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    establecimientoRepository
        .findByIdAndEmpresa_Id(establecimientoId, empresaId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    PuntoEmision pe =
        puntoEmisionRepository
            .findByIdAndEmpresa_Id(id, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!pe.getEstablecimiento().getId().equals(establecimientoId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El punto no pertenece al establecimiento");
    }
    return toResponse(pe);
  }

  private void crearSecuencialesIniciales(Empresa empresa, PuntoEmision pe) {
    for (String tipo : TiposComprobanteSri.TODOS_ORDENADOS) {
      if (secuencialRepository.findByPuntoEmision_IdAndTipoComprobante(pe.getId(), tipo).isEmpty()) {
        Secuencial s = new Secuencial();
        s.setEmpresa(empresa);
        s.setPuntoEmision(pe);
        s.setTipoComprobante(tipo);
        s.setValorActual(0);
        secuencialRepository.save(s);
      }
    }
  }

  private boolean tieneComprobantes(UUID empresaId, PuntoEmision pe) {
    return comprobanteRepository.countByEmpresa_IdAndEstablecimientoCodigoAndPuntoEmisionCodigo(
            empresaId, pe.getEstablecimiento().getCodigo(), pe.getCodigo())
        > 0;
  }

  private PuntoEmisionResponse toResponse(PuntoEmision pe) {
    return new PuntoEmisionResponse(
        pe.getId(),
        pe.getEmpresa().getId(),
        pe.getEstablecimiento().getId(),
        pe.getCodigo(),
        pe.getNombre(),
        pe.getEstablecimiento().getCodigo(),
        pe.getEstado());
  }
}
