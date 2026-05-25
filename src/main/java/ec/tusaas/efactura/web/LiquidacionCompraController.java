package ec.tusaas.efactura.web;

import ec.tusaas.efactura.dto.emision.ComprobanteResponse;
import ec.tusaas.efactura.dto.emision.FacturaRequest;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.service.LiquidacionCompraElectronicaService;
import ec.tusaas.efactura.web.support.EmpresaContextoResolver;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/web/v1/proveedores/liquidaciones")
@RequiredArgsConstructor
public class LiquidacionCompraController {

  private final EmpresaContextoResolver empresaContextoResolver;
  private final LiquidacionCompraElectronicaService liquidacionCompraElectronicaService;

  @PostMapping("/borrador")
  @PreAuthorize(
      "hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN') or hasAuthority('PROVEEDOR_GESTIONAR')")
  public ComprobanteResponse guardarBorrador(
      @RequestParam(required = false) UUID empresaId,
      @AuthenticationPrincipal UsuarioPrincipal principal,
      @Valid @RequestBody FacturaRequest request) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return liquidacionCompraElectronicaService.guardarBorrador(eid, request, principal);
  }

  @PutMapping("/borrador/{id}")
  @PreAuthorize(
      "hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN') or hasAuthority('PROVEEDOR_GESTIONAR')")
  public ComprobanteResponse actualizarBorrador(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @AuthenticationPrincipal UsuarioPrincipal principal,
      @Valid @RequestBody FacturaRequest request) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return liquidacionCompraElectronicaService.actualizarBorrador(eid, id, request, principal);
  }

  @PostMapping("/{id}/emitir")
  @PreAuthorize(
      "hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN') or hasAuthority('PROVEEDOR_GESTIONAR')")
  public ComprobanteResponse emitirBorrador(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return liquidacionCompraElectronicaService.emitirBorrador(eid, id, idempotencyKey, principal);
  }
}
