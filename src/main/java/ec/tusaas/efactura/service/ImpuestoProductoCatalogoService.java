package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.maestro.ImpuestoProductoCatalogoCreateRequest;
import ec.tusaas.efactura.dto.maestro.ImpuestoProductoCatalogoPatchRequest;
import ec.tusaas.efactura.dto.maestro.ImpuestoProductoCatalogoResponse;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.entity.ImpuestoProductoCatalogo;
import ec.tusaas.efactura.repository.EmpresaRepository;
import ec.tusaas.efactura.repository.ImpuestoProductoCatalogoRepository;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ImpuestoProductoCatalogoService {

  private final ImpuestoProductoCatalogoRepository impuestoProductoCatalogoRepository;
  private final EmpresaRepository empresaRepository;

  @Transactional(readOnly = true)
  public List<ImpuestoProductoCatalogoResponse> listarPorEmpresa(UUID empresaId) {
    Empresa empresa =
        empresaRepository
            .findById(empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));
    String pais = paisIsoEmpresa(empresa);
    return impuestoProductoCatalogoRepository.listarVisiblesEmpresaPais(empresaId, pais).stream()
        .map(ImpuestoProductoCatalogoService::toDto)
        .toList();
  }

  @Transactional
  public ImpuestoProductoCatalogoResponse crear(UUID empresaId, ImpuestoProductoCatalogoCreateRequest req) {
    Empresa empresa =
        empresaRepository
            .findById(empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));
    String pais = paisIsoEmpresa(empresa);
    String tipo = req.tipo().trim().toUpperCase(Locale.ROOT);
    String codigo = req.codigo().trim().toUpperCase(Locale.ROOT);
    if (tipo.isEmpty() || codigo.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo y codigo son obligatorios");
    }
    if (impuestoProductoCatalogoRepository.existsByEmpresaIdAndTipoIgnoreCaseAndCodigoIgnoreCase(
        empresaId, tipo, codigo)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un item con ese tipo y codigo");
    }
    ImpuestoProductoCatalogo row = new ImpuestoProductoCatalogo();
    row.setEmpresaId(empresaId);
    row.setPaisIso(pais);
    row.setTipo(tipo);
    row.setCodigo(codigo);
    row.setNombre(req.nombre().trim());
    row.setPorcentajeDefault(req.porcentajeDefault());
    row.setOrden(req.orden() != null ? req.orden() : 0);
    row.setActivo(true);
    ImpuestoProductoCatalogo saved = impuestoProductoCatalogoRepository.save(row);
    return toDto(saved);
  }

  @Transactional
  public ImpuestoProductoCatalogoResponse actualizar(
      UUID empresaId, UUID id, ImpuestoProductoCatalogoPatchRequest req) {
    ImpuestoProductoCatalogo row = buscarEditable(empresaId, id);
    if (req.nombre() != null && !req.nombre().isBlank()) {
      row.setNombre(req.nombre().trim());
    }
    if (req.porcentajeDefault() != null) {
      row.setPorcentajeDefault(req.porcentajeDefault());
    }
    if (req.orden() != null) {
      row.setOrden(req.orden());
    }
    if (req.activo() != null) {
      row.setActivo(req.activo());
    }
    return toDto(impuestoProductoCatalogoRepository.save(row));
  }

  @Transactional
  public void desactivar(UUID empresaId, UUID id) {
    ImpuestoProductoCatalogo row = buscarEditable(empresaId, id);
    row.setActivo(false);
    impuestoProductoCatalogoRepository.save(row);
  }

  private ImpuestoProductoCatalogo buscarEditable(UUID empresaId, UUID id) {
    ImpuestoProductoCatalogo row =
        impuestoProductoCatalogoRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item de catalogo no encontrado"));
    if (row.getEmpresaId() == null || !row.getEmpresaId().equals(empresaId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo se pueden editar items del catalogo de la empresa");
    }
    return row;
  }

  @Transactional(readOnly = true)
  public ImpuestoProductoCatalogo buscarActivo(UUID catalogoId, String paisIso, UUID empresaId) {
    ImpuestoProductoCatalogo row =
        impuestoProductoCatalogoRepository
            .findById(catalogoId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item de catalogo no encontrado"));
    if (!row.isActivo()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item de catalogo inactivo");
    }
    if (!paisIso.equalsIgnoreCase(row.getPaisIso())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item de catalogo no aplica al pais de la empresa");
    }
    if (row.getEmpresaId() != null && !row.getEmpresaId().equals(empresaId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item de catalogo no pertenece a la empresa");
    }
    return row;
  }

  @Transactional(readOnly = true)
  public Optional<ImpuestoProductoCatalogo> findActivoEnPais(UUID catalogoId, String paisIso, UUID empresaId) {
    return impuestoProductoCatalogoRepository
        .findById(catalogoId)
        .filter(
            c ->
                c.isActivo()
                    && c.getPaisIso().equalsIgnoreCase(paisIso)
                    && (c.getEmpresaId() == null || c.getEmpresaId().equals(empresaId)));
  }

  private static String paisIsoEmpresa(Empresa empresa) {
    String pais = empresa.getPaisIso();
    if (pais == null || pais.isBlank()) {
      return "EC";
    }
    return pais.trim().toUpperCase(Locale.ROOT);
  }

  private static ImpuestoProductoCatalogoResponse toDto(ImpuestoProductoCatalogo e) {
    return new ImpuestoProductoCatalogoResponse(
        e.getId(),
        e.getEmpresaId(),
        e.getPaisIso(),
        e.getTipo(),
        e.getCodigo(),
        e.getNombre(),
        e.getPorcentajeDefault(),
        e.getOrden(),
        e.isActivo());
  }
}
