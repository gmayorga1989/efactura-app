package ec.tusaas.efactura.web;

import ec.tusaas.efactura.dto.emision.ComprobanteResponse;
import ec.tusaas.efactura.dto.emision.NotaCreditoRequest;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.service.NotaCreditoElectronicaService;
import ec.tusaas.efactura.web.support.EmpresaContextoResolver;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/web/v1/ventas/notas-credito")
@RequiredArgsConstructor
public class NotaCreditoController {

  private final EmpresaContextoResolver empresaContextoResolver;
  private final NotaCreditoElectronicaService notaCreditoElectronicaService;

  @PostMapping("/borrador")
  @PreAuthorize(
      "hasAuthority('FACTURA_EMITIR') or hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN') "
          + "or hasAuthority('VENTAS_GESTIONAR')")
  public ComprobanteResponse guardarBorrador(
      @RequestParam(required = false) UUID empresaId,
      @AuthenticationPrincipal UsuarioPrincipal principal,
      @Valid @RequestBody NotaCreditoRequest request) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return notaCreditoElectronicaService.guardarBorrador(eid, request, principal);
  }

  @PutMapping("/borrador/{id}")
  @PreAuthorize(
      "hasAuthority('FACTURA_EMITIR') or hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN') "
          + "or hasAuthority('VENTAS_GESTIONAR')")
  public ComprobanteResponse actualizarBorrador(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @AuthenticationPrincipal UsuarioPrincipal principal,
      @Valid @RequestBody NotaCreditoRequest request) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return notaCreditoElectronicaService.actualizarBorrador(eid, id, request, principal);
  }

  @PostMapping("/{id}/emitir")
  @PreAuthorize(
      "hasAuthority('FACTURA_EMITIR') or hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN') "
          + "or hasAuthority('VENTAS_GESTIONAR')")
  public ComprobanteResponse emitirBorrador(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return notaCreditoElectronicaService.emitirBorrador(eid, id, idempotencyKey, principal);
  }

  @GetMapping("/{id}")
  @PreAuthorize(
      "hasAuthority('FACTURA_EMITIR') or hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN') "
          + "or hasAuthority('VENTAS_GESTIONAR') or hasAuthority('REPORTE_VER')")
  public ComprobanteResponse obtener(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return notaCreditoElectronicaService.obtener(eid, id);
  }
}
