package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.maestro.ProductoCategoriaCreateRequest;
import ec.tusaas.efactura.dto.maestro.ProductoCategoriaResponse;
import ec.tusaas.efactura.dto.maestro.ProductoCategoriaUpdateRequest;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.entity.ProductoCategoria;
import ec.tusaas.efactura.repository.ProductoCategoriaRepository;
import ec.tusaas.efactura.repository.ProductoRepository;
import ec.tusaas.efactura.repository.EmpresaRepository;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProductoCategoriaService {

  private final ProductoCategoriaRepository productoCategoriaRepository;
  private final ProductoRepository productoRepository;
  private final EmpresaRepository empresaRepository;

  @Transactional(readOnly = true)
  public List<ProductoCategoriaResponse> listarActivas(UUID empresaId) {
    List<ProductoCategoria> rows =
        productoCategoriaRepository.findByEmpresa_IdAndEstadoOrderByOrdenAscNombreAsc(empresaId, "ACTIVO");
    Map<UUID, ProductoCategoria> byId = new HashMap<>();
    for (ProductoCategoria c : rows) {
      byId.put(c.getId(), c);
    }
    List<ProductoCategoriaResponse> out = new ArrayList<>();
    for (ProductoCategoria c : rows) {
      int nivel = nivelDe(c, byId);
      String ruta = rutaDe(c, byId);
      UUID parentId = c.getParent() == null ? null : c.getParent().getId();
      out.add(new ProductoCategoriaResponse(c.getId(), parentId, c.getCodigo(), c.getNombre(), nivel, ruta, c.getOrden()));
    }
    out.sort(Comparator.comparing(ProductoCategoriaResponse::ruta, String.CASE_INSENSITIVE_ORDER));
    return out;
  }

  @Transactional
  public ProductoCategoriaResponse crear(UUID empresaId, ProductoCategoriaCreateRequest req, UsuarioPrincipal principal) {
    String codigo = req.codigo().trim().toUpperCase(Locale.ROOT);
    if (productoCategoriaRepository.existsByEmpresa_IdAndCodigoIgnoreCase(empresaId, codigo)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Codigo de categoria ya existe");
    }
    Empresa empresa = empresaRepository.getReferenceById(empresaId);
    ProductoCategoria c = new ProductoCategoria();
    c.setEmpresa(empresa);
    c.setCodigo(codigo);
    c.setNombre(req.nombre().trim());
    c.setOrden(req.orden() != null ? req.orden() : 0);
    c.setEstado("ACTIVO");
    c.setUsuarioCreacion(principal.getEmail());
    if (req.parentId() != null) {
      ProductoCategoria parent =
          productoCategoriaRepository
              .findByIdAndEmpresa_Id(req.parentId(), empresaId)
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoria padre no encontrada"));
      c.setParent(parent);
    }
    ProductoCategoria saved = productoCategoriaRepository.save(c);
    return toResponse(saved, empresaId);
  }

  @Transactional
  public ProductoCategoriaResponse actualizar(
      UUID empresaId, UUID id, ProductoCategoriaUpdateRequest req, UsuarioPrincipal principal) {
    ProductoCategoria c =
        productoCategoriaRepository
            .findByIdAndEmpresa_Id(id, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria no encontrada"));
    if (req.nombre() != null && !req.nombre().isBlank()) {
      c.setNombre(req.nombre().trim());
    }
    if (req.orden() != null) {
      c.setOrden(req.orden());
    }
    if (req.parentId() != null) {
      if (req.parentId().equals(id)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoria no puede ser padre de si misma");
      }
      ProductoCategoria parent =
          productoCategoriaRepository
              .findByIdAndEmpresa_Id(req.parentId(), empresaId)
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoria padre no encontrada"));
      if (esDescendienteDe(parent, c.getId())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ciclo en jerarquia de categorias");
      }
      c.setParent(parent);
    }
    c.setFechaModificacion(Instant.now());
    c.setUsuarioModificacion(principal.getEmail());
    return toResponse(productoCategoriaRepository.save(c), empresaId);
  }

  @Transactional
  public void eliminar(UUID empresaId, UUID id, UsuarioPrincipal principal) {
    ProductoCategoria c =
        productoCategoriaRepository
            .findByIdAndEmpresa_Id(id, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria no encontrada"));
    if (productoRepository.countByCategoria_IdAndEstadoNot(id, "ELIMINADO") > 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Existen productos asignados a esta categoria");
    }
    if (productoCategoriaRepository.countByParent_IdAndEstadoNot(id, "ELIMINADO") > 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Existen subcategorias activas");
    }
    c.setEstado("ELIMINADO");
    c.setFechaModificacion(Instant.now());
    c.setUsuarioModificacion(principal.getEmail());
    productoCategoriaRepository.save(c);
  }

  private ProductoCategoriaResponse toResponse(ProductoCategoria c, UUID empresaId) {
    List<ProductoCategoria> rows =
        productoCategoriaRepository.findByEmpresa_IdAndEstadoOrderByOrdenAscNombreAsc(empresaId, "ACTIVO");
    Map<UUID, ProductoCategoria> byId = new HashMap<>();
    for (ProductoCategoria x : rows) {
      byId.put(x.getId(), x);
    }
    int nivel = nivelDe(c, byId);
    String ruta = rutaDe(c, byId);
    UUID parentId = c.getParent() == null ? null : c.getParent().getId();
    return new ProductoCategoriaResponse(c.getId(), parentId, c.getCodigo(), c.getNombre(), nivel, ruta, c.getOrden());
  }

  private static int nivelDe(ProductoCategoria c, Map<UUID, ProductoCategoria> byId) {
    int depth = 0;
    ProductoCategoria cur = c;
    while (cur != null && depth < 64) {
      depth++;
      UUID pid = cur.getParent() == null ? null : cur.getParent().getId();
      cur = pid == null ? null : byId.get(pid);
    }
    return depth;
  }

  private static String rutaDe(ProductoCategoria c, Map<UUID, ProductoCategoria> byId) {
    LinkedList<String> parts = new LinkedList<>();
    ProductoCategoria cur = c;
    for (int i = 0; i < 64 && cur != null; i++) {
      parts.addFirst(cur.getNombre());
      UUID pid = cur.getParent() == null ? null : cur.getParent().getId();
      cur = pid == null ? null : byId.get(pid);
    }
    return String.join(" > ", parts);
  }

  private boolean esDescendienteDe(ProductoCategoria candidateParent, UUID childId) {
    ProductoCategoria cur = candidateParent;
    for (int i = 0; i < 64 && cur != null; i++) {
      if (cur.getId().equals(childId)) {
        return true;
      }
      cur = cur.getParent();
    }
    return false;
  }

  @Transactional(readOnly = true)
  public String rutaEtiquetas(UUID empresaId, UUID categoriaId) {
    if (categoriaId == null) {
      return null;
    }
    ProductoCategoria c =
        productoCategoriaRepository
            .findByIdAndEmpresa_Id(categoriaId, empresaId)
            .orElse(null);
    if (c == null || !"ACTIVO".equals(c.getEstado())) {
      return null;
    }
    List<ProductoCategoria> rows =
        productoCategoriaRepository.findByEmpresa_IdAndEstadoOrderByOrdenAscNombreAsc(empresaId, "ACTIVO");
    Map<UUID, ProductoCategoria> byId = new HashMap<>();
    for (ProductoCategoria x : rows) {
      byId.put(x.getId(), x);
    }
    return rutaDe(c, byId);
  }
}
