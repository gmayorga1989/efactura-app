package ec.tusaas.efactura.web;

import ec.tusaas.efactura.dto.vendedor.VendedorKpiResponse;
import ec.tusaas.efactura.dto.vendedor.VendedorMetaRequest;
import ec.tusaas.efactura.dto.vendedor.VendedorMetaResponse;
import ec.tusaas.efactura.dto.vendedor.VendedorRequest;
import ec.tusaas.efactura.dto.vendedor.VendedorResponse;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.service.VendedorService;
import ec.tusaas.efactura.web.support.EmpresaContextoResolver;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/web/v1/ventas/vendedores")
@RequiredArgsConstructor
public class VendedorController {

  private static final String VENTAS =
      "hasAuthority('VENTAS_GESTIONAR') or hasAuthority('FACTURA_EMITIR') or hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN')";

  private final EmpresaContextoResolver empresaContextoResolver;
  private final VendedorService vendedorService;

  @GetMapping
  @PreAuthorize(VENTAS)
  public Page<VendedorResponse> listar(
      @RequestParam(required = false) UUID empresaId,
      @RequestParam(required = false) String estado,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
    return vendedorService.listar(eid, estado, pageable);
  }

  @GetMapping("/activos")
  @PreAuthorize(VENTAS)
  public List<VendedorResponse> activos(
      @RequestParam(required = false) UUID empresaId, @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return vendedorService.listarActivos(eid);
  }

  @GetMapping("/kpis")
  @PreAuthorize(VENTAS)
  public List<VendedorKpiResponse> kpis(
      @RequestParam(required = false) UUID empresaId,
      @RequestParam int anio,
      @RequestParam int mes,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return vendedorService.kpis(eid, anio, mes);
  }

  @GetMapping("/{id}")
  @PreAuthorize(VENTAS)
  public VendedorResponse obtener(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return vendedorService.obtener(eid, id);
  }

  @PostMapping
  @PreAuthorize(VENTAS)
  public VendedorResponse crear(
      @RequestParam(required = false) UUID empresaId,
      @Valid @RequestBody VendedorRequest body,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return vendedorService.crear(eid, body, principal);
  }

  @PutMapping("/{id}")
  @PreAuthorize(VENTAS)
  public VendedorResponse actualizar(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @Valid @RequestBody VendedorRequest body,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return vendedorService.actualizar(eid, id, body, principal);
  }

  @GetMapping("/{id}/metas")
  @PreAuthorize(VENTAS)
  public List<VendedorMetaResponse> metas(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @RequestParam int anio,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return vendedorService.listarMetas(eid, id, anio);
  }

  @PutMapping("/{id}/metas")
  @PreAuthorize(VENTAS)
  public VendedorMetaResponse guardarMeta(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @Valid @RequestBody VendedorMetaRequest body,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return vendedorService.guardarMeta(eid, id, body, principal);
  }
}
