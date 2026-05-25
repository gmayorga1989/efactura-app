package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.maestro.ProductoImpuestoAdicionalResponse;
import ec.tusaas.efactura.dto.maestro.ProductoImpuestoCatalogoRequest;
import ec.tusaas.efactura.dto.maestro.ProductoImpuestoManualRequest;
import ec.tusaas.efactura.dto.maestro.ProductoListaPrecioResponse;
import ec.tusaas.efactura.dto.maestro.ProductoRequest;
import ec.tusaas.efactura.dto.maestro.ProductoResponse;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.entity.ImpuestoProductoCatalogo;
import ec.tusaas.efactura.entity.ListaPrecio;
import ec.tusaas.efactura.entity.Producto;
import ec.tusaas.efactura.entity.ProductoCategoria;
import ec.tusaas.efactura.entity.ProductoPrecio;
import ec.tusaas.efactura.repository.EmpresaRepository;
import ec.tusaas.efactura.repository.ListaPrecioRepository;
import ec.tusaas.efactura.repository.ProductoCategoriaRepository;
import ec.tusaas.efactura.repository.ProductoPrecioRepository;
import ec.tusaas.efactura.repository.ProductoRepository;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProductoService {

  private static final String CD_IMPUESTOS_ADICIONALES = "impuestosAdicionales";

  private final ProductoRepository productoRepository;
  private final EmpresaRepository empresaRepository;
  private final ListaPrecioRepository listaPrecioRepository;
  private final ProductoPrecioRepository productoPrecioRepository;
  private final ListaPrecioService listaPrecioService;
  private final ProductoCategoriaRepository productoCategoriaRepository;
  private final ImpuestoProductoCatalogoService impuestoProductoCatalogoService;
  private final ProductoCategoriaService productoCategoriaService;
  private final DashboardCacheService dashboardCacheService;

  @Transactional(readOnly = true)
  public Page<ProductoResponse> listar(UUID empresaId, Pageable pageable) {
    return productoRepository.findByEmpresa_IdAndEstadoNotOrderByDescripcionAsc(empresaId, "ELIMINADO", pageable)
        .map(p -> toResponse(p, empresaId));
  }

  @Transactional(readOnly = true)
  public ProductoResponse obtener(UUID empresaId, UUID id) {
    return toResponse(buscar(empresaId, id), empresaId);
  }

  @Transactional
  public ProductoResponse crear(UUID empresaId, ProductoRequest req, UsuarioPrincipal principal) {
    productoRepository.findByEmpresa_IdAndCodigoPrincipal(empresaId, req.codigoPrincipal()).ifPresent(p -> {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Producto/servicio ya existe");
    });
    Empresa empresa =
        empresaRepository.findById(empresaId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    Producto p = new Producto();
    p.setEmpresa(empresa);
    aplicar(p, empresaId, req);
    p.setUsuarioCreacion(principal.getEmail());
    Producto saved = productoRepository.save(p);
    syncPrecios(saved.getId(), empresaId, req);
    saved.setPrecioUnitario(resolveBasePrecio(empresaId, saved.getId()).orElse(req.precioUnitario()));
    saved = productoRepository.save(saved);
    dashboardCacheService.evictEmpresa(empresaId);
    return toResponse(saved, empresaId);
  }

  @Transactional
  public ProductoResponse actualizar(UUID empresaId, UUID id, ProductoRequest req, UsuarioPrincipal principal) {
    Producto p = buscar(empresaId, id);
    if (!p.getCodigoPrincipal().equals(req.codigoPrincipal())) {
      productoRepository.findByEmpresa_IdAndCodigoPrincipal(empresaId, req.codigoPrincipal()).ifPresent(otro -> {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Producto/servicio ya existe");
      });
    }
    aplicar(p, empresaId, req);
    p.setFechaModificacion(Instant.now());
    p.setUsuarioModificacion(principal.getEmail());
    Producto saved = productoRepository.save(p);
    if (req.preciosListas() != null || req.precioUnitario() != null) {
      syncPrecios(saved.getId(), empresaId, req);
      saved.setPrecioUnitario(resolveBasePrecio(empresaId, saved.getId()).orElse(req.precioUnitario()));
      saved = productoRepository.save(saved);
    }
    dashboardCacheService.evictEmpresa(empresaId);
    return toResponse(saved, empresaId);
  }

  @Transactional
  public void eliminar(UUID empresaId, UUID id, UsuarioPrincipal principal) {
    Producto p = buscar(empresaId, id);
    p.setEstado("ELIMINADO");
    p.setFechaModificacion(Instant.now());
    p.setUsuarioModificacion(principal.getEmail());
    productoRepository.save(p);
    dashboardCacheService.evictEmpresa(empresaId);
  }

  private Producto buscar(UUID empresaId, UUID id) {
    return productoRepository
        .findByIdAndEmpresa_Id(id, empresaId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto/servicio no encontrado"));
  }

  private void aplicar(Producto p, UUID empresaId, ProductoRequest req) {
    p.setCodigoPrincipal(req.codigoPrincipal());
    p.setCodigoAuxiliar(req.codigoAuxiliar());
    p.setDescripcion(req.descripcion());
    p.setTipo(req.tipo());
    p.setPrecioUnitario(req.precioUnitario());
    p.setIvaCodigo(req.ivaCodigo());
    p.setIceCodigo(req.iceCodigo());
    p.setIrbpnrCodigo(req.irbpnrCodigo());
    aplicarCategoria(p, empresaId, req.categoriaId());
    p.setCustomData(mergeCustomData(p, req, empresaId));
    p.setEstado("ACTIVO");
  }

  private void aplicarCategoria(Producto p, UUID empresaId, UUID categoriaId) {
    if (categoriaId == null) {
      p.setCategoria(null);
      return;
    }
    ProductoCategoria cat =
        productoCategoriaRepository
            .findByIdAndEmpresa_Id(categoriaId, empresaId)
            .filter(c -> "ACTIVO".equals(c.getEstado()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoria no encontrada"));
    p.setCategoria(cat);
  }

  private Map<String, Object> mergeCustomData(Producto p, ProductoRequest req, UUID empresaId) {
    Map<String, Object> m = new HashMap<>();
    if (p.getCustomData() != null) {
      m.putAll(p.getCustomData());
    }
    m.putAll(req.safeCustomData());
    String pais = paisIsoEmpresa(empresaId);
    boolean catalogoPresent = req.impuestosCatalogo() != null;
    boolean manualPresent = req.impuestosManuales() != null;
    if (catalogoPresent || manualPresent) {
      List<Map<String, Object>> lines = new ArrayList<>();
      if (catalogoPresent && !req.safeImpuestosCatalogo().isEmpty()) {
        lines.addAll(toCatalogStoredList(req.safeImpuestosCatalogo(), pais, empresaId));
      }
      if (manualPresent) {
        for (ProductoImpuestoManualRequest man : req.safeImpuestosManuales()) {
          if (man == null) {
            continue;
          }
          Map<String, Object> row = new LinkedHashMap<>();
          row.put("tipo", man.nombre().trim());
          row.put("porcentaje", man.porcentaje());
          lines.add(row);
        }
      }
      if (lines.isEmpty()) {
        m.remove(CD_IMPUESTOS_ADICIONALES);
      } else {
        m.put(CD_IMPUESTOS_ADICIONALES, lines);
      }
    }
    return m;
  }

  private String paisIsoEmpresa(UUID empresaId) {
    return empresaRepository
        .findById(empresaId)
        .map(Empresa::getPaisIso)
        .map(s -> s == null || s.isBlank() ? "EC" : s.trim().toUpperCase(Locale.ROOT))
        .orElse("EC");
  }

  private List<Map<String, Object>> toCatalogStoredList(
      List<ProductoImpuestoCatalogoRequest> list, String paisIso, UUID empresaId) {
    List<Map<String, Object>> out = new ArrayList<>();
    for (ProductoImpuestoCatalogoRequest line : list) {
      ImpuestoProductoCatalogo def = impuestoProductoCatalogoService.buscarActivo(line.catalogoItemId(), paisIso, empresaId);
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("catalogoItemId", def.getId().toString());
      BigDecimal pct = line.porcentaje();
      if (pct == null) {
        pct = def.getPorcentajeDefault();
      }
      if (pct != null) {
        row.put("porcentaje", pct);
      }
      out.add(row);
    }
    return out;
  }

  private void syncPrecios(UUID productoId, UUID empresaId, ProductoRequest req) {
    ListaPrecio base = listaPrecioService.asegurarListaBase(empresaId);
    Producto ref = productoRepository.getReferenceById(productoId);
    if (req.preciosListas() != null) {
      productoPrecioRepository.deleteByProducto_Id(productoId);
      if (!req.safePreciosListas().isEmpty()) {
        for (var item : req.safePreciosListas()) {
          String cod = normalizeListaCodigo(item.listaCodigo());
          ListaPrecio lista =
              listaPrecioRepository.findByEmpresa_IdAndCodigo(empresaId, cod).orElse(base);
          upsertPrecio(ref, lista.getId(), item.precio());
        }
      } else if (req.precioUnitario() != null) {
        upsertPrecio(ref, base.getId(), req.precioUnitario());
      }
      return;
    }
    if (req.precioUnitario() != null) {
      upsertPrecio(ref, base.getId(), req.precioUnitario());
    }
  }

  private static String normalizeListaCodigo(String codigo) {
    return codigo == null ? "" : codigo.trim().toUpperCase(Locale.ROOT);
  }

  private void upsertPrecio(Producto producto, UUID listaId, BigDecimal precio) {
    UUID pid = producto.getId();
    var existing = productoPrecioRepository.findByProducto_IdAndListaPrecio_Id(pid, listaId);
    if (existing.isPresent()) {
      ProductoPrecio row = existing.get();
      row.setPrecio(precio);
      row.setFechaModificacion(Instant.now());
      productoPrecioRepository.save(row);
    } else {
      ListaPrecio lista = listaPrecioRepository.findById(listaId).orElseThrow();
      ProductoPrecio row = new ProductoPrecio();
      row.setProducto(producto);
      row.setListaPrecio(lista);
      row.setPrecio(precio);
      productoPrecioRepository.save(row);
    }
  }

  private Optional<BigDecimal> resolveBasePrecio(UUID empresaId, UUID productoId) {
    return listaPrecioRepository
        .findByEmpresa_IdAndCodigo(empresaId, "BASE")
        .flatMap(
            b -> productoPrecioRepository.findByProducto_IdAndListaPrecio_Id(productoId, b.getId()).map(ProductoPrecio::getPrecio));
  }

  private ProductoResponse toResponse(Producto p, UUID empresaId) {
    List<ProductoPrecio> rows = productoPrecioRepository.findByProductoIdWithLista(p.getId());
    List<ProductoListaPrecioResponse> precios =
        rows.stream()
            .map(
                pp ->
                    new ProductoListaPrecioResponse(
                        pp.getListaPrecio().getId(),
                        pp.getListaPrecio().getCodigo(),
                        pp.getListaPrecio().getNombre(),
                        pp.getPrecio()))
            .toList();
    BigDecimal basePrecio =
        precios.stream()
            .filter(r -> "BASE".equalsIgnoreCase(r.listaCodigo()))
            .map(ProductoListaPrecioResponse::precio)
            .findFirst()
            .orElse(p.getPrecioUnitario());
    UUID categoriaId = null;
    String categoriaCodigo = null;
    String categoriaNombre = null;
    String categoriaRuta = null;
    if (p.getCategoria() != null) {
      ProductoCategoria cat = p.getCategoria();
      categoriaId = cat.getId();
      categoriaCodigo = cat.getCodigo();
      categoriaNombre = cat.getNombre();
      categoriaRuta = productoCategoriaService.rutaEtiquetas(empresaId, cat.getId());
    }
    return new ProductoResponse(
        p.getId(),
        p.getEmpresa().getId(),
        p.getCodigoPrincipal(),
        p.getCodigoAuxiliar(),
        p.getDescripcion(),
        p.getTipo(),
        basePrecio,
        p.getIvaCodigo(),
        p.getIceCodigo(),
        p.getIrbpnrCodigo(),
        categoriaId,
        categoriaCodigo,
        categoriaNombre,
        categoriaRuta,
        p.getEstado(),
        p.getCustomData(),
        p.getImagenUrl(),
        precios,
        readImpuestos(p.getCustomData(), paisIsoEmpresa(empresaId), empresaId));
  }

  private List<ProductoImpuestoAdicionalResponse> readImpuestos(Map<String, Object> cd, String paisIso, UUID empresaId) {
    if (cd == null) {
      return List.of();
    }
    Object raw = cd.get(CD_IMPUESTOS_ADICIONALES);
    if (!(raw instanceof List<?> list)) {
      return List.of();
    }
    List<ProductoImpuestoAdicionalResponse> out = new ArrayList<>();
    for (Object o : list) {
      if (!(o instanceof Map<?, ?> m)) {
        continue;
      }
      Object cid = m.get("catalogoItemId");
      if (cid == null) {
        cid = m.get("catalogoId");
      }
      if (cid != null && !String.valueOf(cid).isBlank()) {
        UUID catId;
        try {
          catId = UUID.fromString(String.valueOf(cid).trim());
        } catch (IllegalArgumentException ex) {
          continue;
        }
        Optional<ImpuestoProductoCatalogo> defOpt =
            impuestoProductoCatalogoService.findActivoEnPais(catId, paisIso, empresaId);
        if (defOpt.isEmpty()) {
          continue;
        }
        ImpuestoProductoCatalogo def = defOpt.get();
        BigDecimal pct = null;
        if (m.get("porcentaje") instanceof Number n) {
          pct = BigDecimal.valueOf(n.doubleValue());
        }
        if (pct == null) {
          pct = def.getPorcentajeDefault();
        }
        out.add(
            new ProductoImpuestoAdicionalResponse(
                def.getId(), def.getNombre(), def.getTipo(), def.getCodigo(), pct));
        continue;
      }
      String tipo = String.valueOf(m.get("tipo") == null ? "" : m.get("tipo")).trim();
      if (tipo.isEmpty()) {
        continue;
      }
      String codigo = m.get("codigo") != null ? String.valueOf(m.get("codigo")).trim() : null;
      BigDecimal pct = null;
      if (m.get("porcentaje") instanceof Number n) {
        pct = BigDecimal.valueOf(n.doubleValue());
      }
      String nombreLegacy =
          codigo != null && !codigo.isBlank() ? tipo + " — " + codigo : tipo;
      out.add(new ProductoImpuestoAdicionalResponse(null, nombreLegacy, tipo, codigo, pct));
    }
    return out;
  }
}
