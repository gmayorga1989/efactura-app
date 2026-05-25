package ec.tusaas.efactura.web;

import ec.tusaas.efactura.dto.emision.ComprobanteRelacionadoResumen;
import ec.tusaas.efactura.dto.emision.ComprobanteResponse;
import ec.tusaas.efactura.dto.emision.FacturaRequest;
import ec.tusaas.efactura.dto.emision.ReenviarCorreoRequest;
import ec.tusaas.efactura.dto.emision.PuntoEmisionEmitirOption;
import ec.tusaas.efactura.dto.factura.FacturaCampoExtraItem;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.service.FacturaCamposExtraService;
import ec.tusaas.efactura.dto.comprobante.HistorialElectronicoEventoDto;
import ec.tusaas.efactura.service.ComprobanteHistorialElectronicoService;
import ec.tusaas.efactura.service.FacturaElectronicaService;
import ec.tusaas.efactura.web.support.EmpresaContextoResolver;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/web/v1")
@RequiredArgsConstructor
public class EmisionController {

  private static final String EMITIR_O_GESTIONAR =
      "hasAuthority('FACTURA_EMITIR') or hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN') "
          + "or hasAuthority('VENTAS_GESTIONAR') or hasAuthority('PROVEEDOR_GESTIONAR')";

  private static final String LEER_COMPROBANTE =
      EMITIR_O_GESTIONAR + " or hasAuthority('REPORTE_VER') or hasAuthority('COMPROBANTE_MONITOR')";

  private final EmpresaContextoResolver empresaContextoResolver;
  private final FacturaElectronicaService facturaElectronicaService;
  private final ComprobanteHistorialElectronicoService comprobanteHistorialElectronicoService;
  private final FacturaCamposExtraService facturaCamposExtraService;

  @GetMapping("/comprobantes")
  @PreAuthorize(
      "hasAuthority('FACTURA_EMITIR') or hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN') or hasAuthority('REPORTE_VER')")
  public Page<ComprobanteResponse> listarComprobantes(
      @RequestParam(required = false) UUID empresaId,
      @RequestParam(required = false) String tipo,
      @RequestParam(required = false) String estadoSri,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    int safeSize = Math.min(Math.max(size, 1), 100);
    Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize);
    return facturaElectronicaService.listar(eid, tipo, estadoSri, fechaDesde, fechaHasta, pageable);
  }

  @GetMapping("/facturas/puntos-emision")
  @PreAuthorize(EMITIR_O_GESTIONAR)
  public List<PuntoEmisionEmitirOption> puntosEmisionParaFactura(
      @RequestParam(required = false) UUID empresaId, @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return facturaElectronicaService.puntosParaEmitir(eid);
  }

  @PostMapping("/facturas/borrador")
  @PreAuthorize(EMITIR_O_GESTIONAR)
  public ComprobanteResponse guardarBorradorFactura(
      @RequestParam(required = false) UUID empresaId,
      @AuthenticationPrincipal UsuarioPrincipal principal,
      @Valid @RequestBody FacturaRequest request) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return facturaElectronicaService.guardarBorrador(eid, request, principal);
  }

  @PutMapping("/facturas/borrador/{id}")
  @PreAuthorize(EMITIR_O_GESTIONAR)
  public ComprobanteResponse actualizarBorradorFactura(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @AuthenticationPrincipal UsuarioPrincipal principal,
      @Valid @RequestBody FacturaRequest request) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return facturaElectronicaService.actualizarBorrador(eid, id, request, principal);
  }

  @PostMapping("/facturas/{id}/emitir")
  @PreAuthorize(EMITIR_O_GESTIONAR)
  public ComprobanteResponse emitirBorradorFactura(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return facturaElectronicaService.emitirBorrador(eid, id, idempotencyKey, principal);
  }

  @PostMapping("/facturas")
  @PreAuthorize(EMITIR_O_GESTIONAR)
  public ComprobanteResponse emitirFactura(
      @RequestParam(required = false) UUID empresaId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @AuthenticationPrincipal UsuarioPrincipal principal,
      @Valid @RequestBody FacturaRequest request) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return facturaElectronicaService.emitir(eid, request, idempotencyKey, principal);
  }

  @GetMapping("/comprobantes/{id}")
  @PreAuthorize(LEER_COMPROBANTE)
  public ComprobanteResponse obtenerComprobante(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return facturaElectronicaService.obtener(eid, id);
  }

  @GetMapping("/comprobantes/{id}/relacionados")
  @PreAuthorize(LEER_COMPROBANTE)
  public List<ComprobanteRelacionadoResumen> comprobantesRelacionados(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return facturaElectronicaService.listarRelacionadosConFactura(eid, id);
  }

  @GetMapping("/comprobantes/{id}/historial-electronico")
  @PreAuthorize(LEER_COMPROBANTE)
  public List<HistorialElectronicoEventoDto> historialElectronicoComprobante(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return comprobanteHistorialElectronicoService.historialComprobante(eid, id);
  }

  @GetMapping(value = "/comprobantes/{id}/xml-generado", produces = MediaType.APPLICATION_XML_VALUE)
  @PreAuthorize(LEER_COMPROBANTE)
  public ResponseEntity<String> xmlGenerado(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return ResponseEntity.ok(facturaElectronicaService.obtenerXml(eid, id, "XML_GENERADO"));
  }

  @GetMapping(value = "/comprobantes/{id}/xml-firmado", produces = MediaType.APPLICATION_XML_VALUE)
  @PreAuthorize(LEER_COMPROBANTE)
  public ResponseEntity<String> xmlFirmado(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    try {
      return ResponseEntity.ok(facturaElectronicaService.obtenerXml(eid, id, "XML_FIRMADO"));
    } catch (org.springframework.web.server.ResponseStatusException notFound) {
      if (notFound.getStatusCode().value() != 404) {
        throw notFound;
      }
      return ResponseEntity.ok(facturaElectronicaService.obtenerXml(eid, id, "XML_FIRMADO_STUB"));
    }
  }

  @GetMapping(value = "/comprobantes/{id}/xml-autorizado", produces = MediaType.APPLICATION_XML_VALUE)
  @PreAuthorize(LEER_COMPROBANTE)
  public ResponseEntity<String> xmlAutorizado(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return ResponseEntity.ok(facturaElectronicaService.obtenerXml(eid, id, "XML_AUTORIZADO"));
  }

  @PostMapping("/comprobantes/{id}/reprocesar-autorizacion")
  @PreAuthorize(EMITIR_O_GESTIONAR)
  public ComprobanteResponse reprocesarAutorizacion(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return facturaElectronicaService.reprocesarAutorizacion(eid, id);
  }

  @PostMapping("/comprobantes/{id}/reenviar-correo")
  @PreAuthorize(EMITIR_O_GESTIONAR)
  public ResponseEntity<Map<String, Object>> reenviarCorreo(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @RequestBody(required = false) @Valid ReenviarCorreoRequest body,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    String email = body != null ? body.emailReceptor() : null;
    boolean enviado = facturaElectronicaService.reenviarCorreoComprobante(eid, id, email);
    return ResponseEntity.ok(Map.of("enviado", enviado));
  }

  @GetMapping(value = "/comprobantes/{id}/ride", produces = MediaType.APPLICATION_PDF_VALUE)
  @PreAuthorize(LEER_COMPROBANTE)
  public ResponseEntity<byte[]> ride(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"ride-" + id + ".pdf\"")
        .body(facturaElectronicaService.obtenerRide(eid, id));
  }

  @GetMapping("/facturas/campos-extra")
  @PreAuthorize(LEER_COMPROBANTE)
  public List<FacturaCampoExtraItem> listarCamposExtra(
      @RequestParam(required = false) UUID empresaId, @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return facturaCamposExtraService.listar(eid);
  }

  @PutMapping("/facturas/campos-extra")
  @PreAuthorize("hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN')")
  public void guardarCamposExtra(
      @RequestParam(required = false) UUID empresaId,
      @AuthenticationPrincipal UsuarioPrincipal principal,
      @Valid @RequestBody List<FacturaCampoExtraItem> body) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    facturaCamposExtraService.reemplazar(eid, body);
  }
}
