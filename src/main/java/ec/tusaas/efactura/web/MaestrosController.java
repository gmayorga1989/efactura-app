package ec.tusaas.efactura.web;

import ec.tusaas.efactura.dto.maestro.ImpuestoProductoCatalogoCreateRequest;
import ec.tusaas.efactura.dto.maestro.ImpuestoProductoCatalogoPatchRequest;
import ec.tusaas.efactura.dto.maestro.ImpuestoProductoCatalogoResponse;
import ec.tusaas.efactura.dto.maestro.ListaPrecioCreateRequest;
import ec.tusaas.efactura.dto.maestro.ListaPrecioResponse;
import ec.tusaas.efactura.dto.maestro.ProductoCategoriaCreateRequest;
import ec.tusaas.efactura.dto.maestro.ProductoCategoriaResponse;
import ec.tusaas.efactura.dto.maestro.ProductoCategoriaUpdateRequest;
import ec.tusaas.efactura.dto.maestro.ClienteRequest;
import ec.tusaas.efactura.dto.maestro.ClienteResponse;
import ec.tusaas.efactura.dto.maestro.ProductoImportResult;
import ec.tusaas.efactura.dto.maestro.ProductoRequest;
import ec.tusaas.efactura.dto.maestro.ProductoResponse;
import ec.tusaas.efactura.dto.maestro.RucConsultaResponse;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.service.ClienteService;
import ec.tusaas.efactura.service.ImpuestoProductoCatalogoService;
import ec.tusaas.efactura.service.ListaPrecioService;
import ec.tusaas.efactura.service.ProductoCategoriaService;
import ec.tusaas.efactura.service.ProductoImagenService;
import ec.tusaas.efactura.service.ProductoImportService;
import ec.tusaas.efactura.service.ProductoService;
import ec.tusaas.efactura.service.SriCatastroService;
import ec.tusaas.efactura.web.support.EmpresaContextoResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/web/v1")
@RequiredArgsConstructor
@PreAuthorize(
    "hasAuthority('EMPRESA_ADMIN') or hasAuthority('FACTURA_EMITIR') or hasAuthority('VENTAS_GESTIONAR') "
        + "or hasAuthority('PROVEEDOR_GESTIONAR') or hasAuthority('PLATFORM_ADMIN')")
public class MaestrosController {

  private final EmpresaContextoResolver empresaContextoResolver;
  private final ClienteService clienteService;
  private final ProductoService productoService;
  private final ListaPrecioService listaPrecioService;
  private final ProductoImagenService productoImagenService;
  private final ProductoCategoriaService productoCategoriaService;
  private final ImpuestoProductoCatalogoService impuestoProductoCatalogoService;
  private final SriCatastroService sriCatastroService;
  private final ProductoImportService productoImportService;

  @GetMapping({"/clientes", "/proveedores"})
  public Page<ClienteResponse> listarClientes(
      @RequestParam(required = false) UUID empresaId,
      @RequestParam(required = false) String tipoTercero,
      @RequestParam(required = false) String estado,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @AuthenticationPrincipal UsuarioPrincipal principal,
      HttpServletRequest servletRequest) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return clienteService.listar(eid, tipoTercero != null ? tipoTercero : tipoDesdeRuta(servletRequest), estado, q, pageable(page, size));
  }

  @PostMapping({"/clientes", "/proveedores"})
  public ClienteResponse crearCliente(
      @RequestParam(required = false) UUID empresaId,
      @Valid @RequestBody ClienteRequest body,
      @AuthenticationPrincipal UsuarioPrincipal principal,
      HttpServletRequest servletRequest) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return clienteService.crear(eid, body, tipoDesdeRuta(servletRequest), principal);
  }

  @GetMapping({"/clientes/consulta-ruc/{ruc}", "/proveedores/consulta-ruc/{ruc}"})
  public RucConsultaResponse consultarRuc(@PathVariable String ruc) {
    return sriCatastroService.consultarRuc(ruc);
  }

  @GetMapping({"/clientes/{id}", "/proveedores/{id}"})
  public ClienteResponse obtenerCliente(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return clienteService.obtener(eid, id);
  }

  @PatchMapping({"/clientes/{id}", "/proveedores/{id}"})
  public ClienteResponse actualizarCliente(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @Valid @RequestBody ClienteRequest body,
      @AuthenticationPrincipal UsuarioPrincipal principal,
      HttpServletRequest servletRequest) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return clienteService.actualizar(eid, id, body, tipoDesdeRuta(servletRequest), principal);
  }

  @PatchMapping({"/clientes/{id}/tipo-tercero", "/proveedores/{id}/tipo-tercero"})
  public ClienteResponse cambiarTipoTercero(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @RequestBody Map<String, String> body,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return clienteService.cambiarTipoTercero(eid, id, body.get("tipoTercero"), principal);
  }

  @DeleteMapping({"/clientes/{id}", "/proveedores/{id}"})
  public void eliminarCliente(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    clienteService.eliminar(eid, id, principal);
  }

  @GetMapping({"/productos/listas-precio", "/servicios/listas-precio"})
  public List<ListaPrecioResponse> listasPrecio(
      @RequestParam(required = false) UUID empresaId, @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    listaPrecioService.asegurarListaBase(eid);
    return listaPrecioService.listarActivas(eid);
  }

  @PostMapping({"/productos/listas-precio", "/servicios/listas-precio"})
  public ListaPrecioResponse crearListaPrecio(
      @RequestParam(required = false) UUID empresaId,
      @Valid @RequestBody ListaPrecioCreateRequest body,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return listaPrecioService.crear(eid, body);
  }

  @GetMapping({"/productos/impuestos-catalogo", "/servicios/impuestos-catalogo"})
  public List<ImpuestoProductoCatalogoResponse> impuestosCatalogoProducto(
      @RequestParam(required = false) UUID empresaId, @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return impuestoProductoCatalogoService.listarPorEmpresa(eid);
  }

  @PostMapping({"/productos/impuestos-catalogo", "/servicios/impuestos-catalogo"})
  public ImpuestoProductoCatalogoResponse crearImpuestoCatalogoProducto(
      @RequestParam(required = false) UUID empresaId,
      @Valid @RequestBody ImpuestoProductoCatalogoCreateRequest body,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return impuestoProductoCatalogoService.crear(eid, body);
  }

  @PatchMapping({"/productos/impuestos-catalogo/{id}", "/servicios/impuestos-catalogo/{id}"})
  public ImpuestoProductoCatalogoResponse actualizarImpuestoCatalogoProducto(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @Valid @RequestBody ImpuestoProductoCatalogoPatchRequest body,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return impuestoProductoCatalogoService.actualizar(eid, id, body);
  }

  @DeleteMapping({"/productos/impuestos-catalogo/{id}", "/servicios/impuestos-catalogo/{id}"})
  public void eliminarImpuestoCatalogoProducto(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    impuestoProductoCatalogoService.desactivar(eid, id);
  }

  @GetMapping({"/productos/categorias", "/servicios/categorias"})
  public List<ProductoCategoriaResponse> listarCategoriasProducto(
      @RequestParam(required = false) UUID empresaId, @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return productoCategoriaService.listarActivas(eid);
  }

  @PostMapping({"/productos/categorias", "/servicios/categorias"})
  public ProductoCategoriaResponse crearCategoriaProducto(
      @RequestParam(required = false) UUID empresaId,
      @Valid @RequestBody ProductoCategoriaCreateRequest body,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return productoCategoriaService.crear(eid, body, principal);
  }

  @PatchMapping({"/productos/categorias/{id}", "/servicios/categorias/{id}"})
  public ProductoCategoriaResponse actualizarCategoriaProducto(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @Valid @RequestBody ProductoCategoriaUpdateRequest body,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return productoCategoriaService.actualizar(eid, id, body, principal);
  }

  @DeleteMapping({"/productos/categorias/{id}", "/servicios/categorias/{id}"})
  public void eliminarCategoriaProducto(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    productoCategoriaService.eliminar(eid, id, principal);
  }

  @PostMapping(
      value = {"/productos/{id}/imagen", "/servicios/{id}/imagen"},
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ProductoResponse subirImagenProducto(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @RequestPart("archivo") MultipartFile archivo,
      @AuthenticationPrincipal UsuarioPrincipal principal)
      throws Exception {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    productoImagenService.subir(eid, id, archivo, principal);
    return productoService.obtener(eid, id);
  }

  @GetMapping({"/productos/plantilla-importacion", "/servicios/plantilla-importacion"})
  public ResponseEntity<byte[]> plantillaImportacionProductos(
      @RequestParam(required = false) UUID empresaId,
      @AuthenticationPrincipal UsuarioPrincipal principal,
      HttpServletRequest servletRequest) {
    empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    String tipo = tipoProductoDesdeRuta(servletRequest);
    byte[] csv = productoImportService.generarPlantillaCsv(tipo);
    String filename = "plantilla-" + tipo.toLowerCase() + ".csv";
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
        .body(csv);
  }

  @PostMapping(
      value = {"/productos/importar", "/servicios/importar"},
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ProductoImportResult importarProductos(
      @RequestParam(required = false) UUID empresaId,
      @RequestPart("archivo") MultipartFile archivo,
      @AuthenticationPrincipal UsuarioPrincipal principal,
      HttpServletRequest servletRequest) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    String tipo = tipoProductoDesdeRuta(servletRequest);
    return productoImportService.importar(eid, archivo, tipo, principal);
  }

  @GetMapping({"/productos", "/servicios"})
  public Page<ProductoResponse> listarProductos(
      @RequestParam(required = false) UUID empresaId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return productoService.listar(eid, pageable(page, size));
  }

  @PostMapping({"/productos", "/servicios"})
  public ProductoResponse crearProducto(
      @RequestParam(required = false) UUID empresaId,
      @Valid @RequestBody ProductoRequest body,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return productoService.crear(eid, body, principal);
  }

  @GetMapping({"/productos/{id}", "/servicios/{id}"})
  public ProductoResponse obtenerProducto(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return productoService.obtener(eid, id);
  }

  @PatchMapping({"/productos/{id}", "/servicios/{id}"})
  public ProductoResponse actualizarProducto(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @Valid @RequestBody ProductoRequest body,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return productoService.actualizar(eid, id, body, principal);
  }

  @DeleteMapping({"/productos/{id}", "/servicios/{id}"})
  public void eliminarProducto(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    productoService.eliminar(eid, id, principal);
  }

  private static Pageable pageable(int page, int size) {
    return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
  }

  private static String tipoDesdeRuta(HttpServletRequest request) {
    return request.getRequestURI().contains("/proveedores") ? "PROVEEDOR" : "CLIENTE";
  }

  private static String tipoProductoDesdeRuta(HttpServletRequest request) {
    return request.getRequestURI().contains("/servicios") ? "SERVICIO" : "PRODUCTO";
  }
}
