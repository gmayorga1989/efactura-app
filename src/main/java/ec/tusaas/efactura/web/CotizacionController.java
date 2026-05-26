package ec.tusaas.efactura.web;

import ec.tusaas.efactura.dto.cotizacion.CotizacionAdjuntoResponse;
import ec.tusaas.efactura.dto.cotizacion.CotizacionConvertirRequest;
import ec.tusaas.efactura.dto.cotizacion.CotizacionEnviarCorreoRequest;
import ec.tusaas.efactura.dto.cotizacion.CotizacionRequest;
import ec.tusaas.efactura.dto.cotizacion.CotizacionResponse;
import ec.tusaas.efactura.dto.emision.ComprobanteResponse;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.service.CotizacionService;
import ec.tusaas.efactura.web.support.EmpresaContextoResolver;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/web/v1/ventas/cotizaciones")
@RequiredArgsConstructor
public class CotizacionController {

  private static final String VENTAS =
      "hasAuthority('VENTAS_GESTIONAR') or hasAuthority('FACTURA_EMITIR') or hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN')";

  private final EmpresaContextoResolver empresaContextoResolver;
  private final CotizacionService cotizacionService;

  @GetMapping
  @PreAuthorize(VENTAS)
  public Page<CotizacionResponse> listar(
      @RequestParam(required = false) UUID empresaId,
      @RequestParam(required = false) String estado,
      @RequestParam(required = false) UUID vendedorId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
    return cotizacionService.listar(eid, estado, vendedorId, fechaDesde, fechaHasta, q, pageable);
  }

  @GetMapping("/plantilla-empresa")
  @PreAuthorize(VENTAS)
  public Map<String, Object> plantillaEmpresa(
      @RequestParam(required = false) UUID empresaId, @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return cotizacionService.plantillaEmpresa(eid);
  }

  @PostMapping(value = "/plantilla-empresa/vista-previa", produces = MediaType.TEXT_HTML_VALUE)
  @PreAuthorize(VENTAS)
  public ResponseEntity<String> previewPlantillaEmpresa(
      @RequestParam(required = false) UUID empresaId,
      @RequestBody(required = false) Map<String, Object> plantilla,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return ResponseEntity.ok(cotizacionService.previewPlantillaEmpresaHtml(eid, plantilla));
  }

  @PostMapping("/plantilla-empresa/banner")
  @PreAuthorize(VENTAS)
  public Map<String, Object> subirBannerPlantilla(
      @RequestParam(required = false) UUID empresaId,
      @RequestPart("archivo") MultipartFile archivo,
      @AuthenticationPrincipal UsuarioPrincipal principal) throws Exception {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return cotizacionService.subirBannerPlantillaEmpresa(eid, archivo, principal);
  }

  @PostMapping("/{id}/adjuntos/archivo")
  @PreAuthorize(VENTAS)
  public CotizacionAdjuntoResponse subirAdjuntoArchivo(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @RequestPart("archivo") MultipartFile archivo,
      @AuthenticationPrincipal UsuarioPrincipal principal) throws Exception {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return cotizacionService.subirAdjuntoArchivo(eid, id, archivo, principal);
  }

  @DeleteMapping("/{id}/adjuntos/{adjuntoId}")
  @PreAuthorize(VENTAS)
  public void eliminarAdjunto(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @PathVariable UUID adjuntoId,
      @AuthenticationPrincipal UsuarioPrincipal principal)
      throws Exception {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    cotizacionService.eliminarAdjunto(eid, id, adjuntoId, principal);
  }

  @PutMapping("/plantilla-empresa")
  @PreAuthorize(VENTAS)
  public Map<String, Object> guardarPlantillaEmpresa(
      @RequestParam(required = false) UUID empresaId,
      @RequestBody Map<String, Object> plantilla,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return cotizacionService.guardarPlantillaEmpresa(eid, plantilla, principal);
  }

  @GetMapping("/{id}")
  @PreAuthorize(VENTAS)
  public CotizacionResponse obtener(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return cotizacionService.obtener(eid, id);
  }

  @GetMapping(value = "/{id}/preview", produces = MediaType.TEXT_HTML_VALUE)
  @PreAuthorize(VENTAS)
  public ResponseEntity<String> preview(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return ResponseEntity.ok(cotizacionService.previewHtml(eid, id));
  }

  @PostMapping
  @PreAuthorize(VENTAS)
  public CotizacionResponse crear(
      @RequestParam(required = false) UUID empresaId,
      @Valid @RequestBody CotizacionRequest body,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return cotizacionService.crear(eid, body, principal);
  }

  @PutMapping("/{id}")
  @PreAuthorize(VENTAS)
  public CotizacionResponse actualizar(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @Valid @RequestBody CotizacionRequest body,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return cotizacionService.actualizar(eid, id, body, principal);
  }

  @PostMapping("/{id}/anular")
  @PreAuthorize(VENTAS)
  public void anular(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    cotizacionService.anular(eid, id, principal);
  }

  @PostMapping("/{id}/aceptar")
  @PreAuthorize(VENTAS)
  public CotizacionResponse aceptar(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return cotizacionService.aceptar(eid, id, principal);
  }

  @PostMapping("/{id}/rechazar")
  @PreAuthorize(VENTAS)
  public CotizacionResponse rechazar(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return cotizacionService.rechazar(eid, id, principal);
  }

  @PostMapping("/{id}/convertir-factura")
  @PreAuthorize(VENTAS)
  public ComprobanteResponse convertir(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @Valid @RequestBody CotizacionConvertirRequest body,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return cotizacionService.convertirAFactura(eid, id, body, principal);
  }

  @PostMapping("/{id}/enviar-correo")
  @PreAuthorize(VENTAS)
  public Map<String, Object> enviarCorreo(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @Valid @RequestBody CotizacionEnviarCorreoRequest body,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    boolean ok = cotizacionService.enviarCorreo(eid, id, body);
    return Map.of("enviado", ok);
  }
}
