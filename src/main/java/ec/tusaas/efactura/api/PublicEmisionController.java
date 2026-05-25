package ec.tusaas.efactura.api;

import ec.tusaas.efactura.dto.emision.ComprobanteResponse;
import ec.tusaas.efactura.dto.emision.FacturaRequest;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.service.FacturaElectronicaService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * API pública para integraciones (autenticación {@code X-Api-Key: prefix.secret}).
 */
@RestController
@RequestMapping("/api/public/v1")
@RequiredArgsConstructor
public class PublicEmisionController {

  private final FacturaElectronicaService facturaElectronicaService;

  private static UUID empresaDesdePrincipal(UsuarioPrincipal principal) {
    if (principal.getEmpresaId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "API key sin empresa asociada");
    }
    return principal.getEmpresaId();
  }

  @PostMapping("/facturas")
  @PreAuthorize("hasAuthority('FACTURA_EMITIR')")
  public ComprobanteResponse emitirFactura(
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @AuthenticationPrincipal UsuarioPrincipal principal,
      @Valid @RequestBody FacturaRequest request) {
    UUID eid = empresaDesdePrincipal(principal);
    return facturaElectronicaService.emitir(eid, request, idempotencyKey, principal);
  }

  @GetMapping("/comprobantes/{id}")
  @PreAuthorize("hasAuthority('FACTURA_EMITIR')")
  public ComprobanteResponse obtenerComprobante(
      @PathVariable UUID id, @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaDesdePrincipal(principal);
    return facturaElectronicaService.obtener(eid, id);
  }

  @GetMapping(value = "/comprobantes/{id}/xml-generado", produces = MediaType.APPLICATION_XML_VALUE)
  @PreAuthorize("hasAuthority('FACTURA_EMITIR')")
  public ResponseEntity<String> xmlGenerado(
      @PathVariable UUID id, @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaDesdePrincipal(principal);
    return ResponseEntity.ok(facturaElectronicaService.obtenerXml(eid, id, "XML_GENERADO"));
  }

  @GetMapping(value = "/comprobantes/{id}/xml-firmado", produces = MediaType.APPLICATION_XML_VALUE)
  @PreAuthorize("hasAuthority('FACTURA_EMITIR')")
  public ResponseEntity<String> xmlFirmado(
      @PathVariable UUID id, @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaDesdePrincipal(principal);
    try {
      return ResponseEntity.ok(facturaElectronicaService.obtenerXml(eid, id, "XML_FIRMADO"));
    } catch (ResponseStatusException notFound) {
      if (notFound.getStatusCode().value() != 404) {
        throw notFound;
      }
      return ResponseEntity.ok(facturaElectronicaService.obtenerXml(eid, id, "XML_FIRMADO_STUB"));
    }
  }

  @GetMapping(value = "/comprobantes/{id}/xml-autorizado", produces = MediaType.APPLICATION_XML_VALUE)
  @PreAuthorize("hasAuthority('FACTURA_EMITIR')")
  public ResponseEntity<String> xmlAutorizado(
      @PathVariable UUID id, @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaDesdePrincipal(principal);
    return ResponseEntity.ok(facturaElectronicaService.obtenerXml(eid, id, "XML_AUTORIZADO"));
  }

  @GetMapping(value = "/comprobantes/{id}/ride", produces = MediaType.APPLICATION_PDF_VALUE)
  @PreAuthorize("hasAuthority('FACTURA_EMITIR')")
  public ResponseEntity<byte[]> ride(
      @PathVariable UUID id, @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaDesdePrincipal(principal);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"ride-" + id + ".pdf\"")
        .body(facturaElectronicaService.obtenerRide(eid, id));
  }
}
