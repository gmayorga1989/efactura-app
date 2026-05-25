package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.tributario.EstablecimientoRequest;
import ec.tusaas.efactura.dto.tributario.EstablecimientoResponse;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.entity.Establecimiento;
import ec.tusaas.efactura.repository.ComprobanteRepository;
import ec.tusaas.efactura.repository.EmpresaRepository;
import ec.tusaas.efactura.repository.EstablecimientoRepository;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class EstablecimientoService {

  private final EstablecimientoRepository establecimientoRepository;
  private final EmpresaRepository empresaRepository;
  private final ComprobanteRepository comprobanteRepository;
  private final EmpresaTributarioService empresaTributarioService;
  private final DashboardCacheService dashboardCacheService;

  @Transactional(readOnly = true)
  public List<EstablecimientoResponse> listar(UUID empresaId, UsuarioPrincipal principal) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal); // reutiliza validación de acceso
    return establecimientoRepository.findByEmpresa_IdOrderByCodigoAsc(empresaId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public EstablecimientoResponse crear(UUID empresaId, EstablecimientoRequest req, UsuarioPrincipal principal) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    Empresa empresa =
        empresaRepository.findById(empresaId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    establecimientoRepository
        .findByEmpresa_IdAndCodigo(empresaId, req.codigo())
        .ifPresent(
            x -> {
              throw new ResponseStatusException(HttpStatus.CONFLICT, "Código de establecimiento ya existe");
            });
    Establecimiento e = new Establecimiento();
    e.setEmpresa(empresa);
    e.setCodigo(req.codigo());
    e.setNombre(req.nombre());
    e.setDireccion(req.direccion());
    e.setUsuarioCreacion(principal.getEmail());
    Establecimiento saved = establecimientoRepository.save(e);
    dashboardCacheService.evictEmpresa(empresaId);
    return toResponse(saved);
  }

  @Transactional
  public EstablecimientoResponse actualizar(
      UUID empresaId, UUID id, EstablecimientoRequest req, UsuarioPrincipal principal) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    Establecimiento e =
        establecimientoRepository
            .findByIdAndEmpresa_Id(id, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    boolean tieneComprobantes = tieneComprobantes(empresaId, e.getCodigo());
    if (tieneComprobantes) {
      if (!Objects.equals(e.getCodigo(), req.codigo()) || !Objects.equals(e.getDireccion(), req.direccion())) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "El establecimiento ya tiene documentos generados; solo se puede cambiar el nombre");
      }
    }
    if (!e.getCodigo().equals(req.codigo())) {
      establecimientoRepository
          .findByEmpresa_IdAndCodigo(empresaId, req.codigo())
          .ifPresent(
              otro -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Código de establecimiento ya existe");
              });
    }
    e.setCodigo(req.codigo());
    e.setNombre(req.nombre());
    e.setDireccion(req.direccion());
    e.setFechaModificacion(Instant.now());
    e.setUsuarioModificacion(principal.getEmail());
    Establecimiento saved = establecimientoRepository.save(e);
    dashboardCacheService.evictEmpresa(empresaId);
    return toResponse(saved);
  }

  @Transactional
  public EstablecimientoResponse cambiarEstado(
      UUID empresaId, UUID id, String estado, UsuarioPrincipal principal) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    Establecimiento e =
        establecimientoRepository
            .findByIdAndEmpresa_Id(id, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    e.setEstado(estado);
    e.setFechaModificacion(Instant.now());
    e.setUsuarioModificacion(principal.getEmail());
    Establecimiento saved = establecimientoRepository.save(e);
    dashboardCacheService.evictEmpresa(empresaId);
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public EstablecimientoResponse obtener(UUID empresaId, UUID id, UsuarioPrincipal principal) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    Establecimiento e =
        establecimientoRepository
            .findByIdAndEmpresa_Id(id, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    return toResponse(e);
  }

  private EstablecimientoResponse toResponse(Establecimiento e) {
    return new EstablecimientoResponse(
        e.getId(),
        e.getEmpresa().getId(),
        e.getCodigo(),
        e.getNombre(),
        e.getDireccion(),
        e.getEstado());
  }

  private boolean tieneComprobantes(UUID empresaId, String establecimientoCodigo) {
    return comprobanteRepository.countByEmpresa_IdAndEstablecimientoCodigo(empresaId, establecimientoCodigo) > 0;
  }
}
