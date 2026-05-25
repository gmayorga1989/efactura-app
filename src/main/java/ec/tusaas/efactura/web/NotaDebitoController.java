package ec.tusaas.efactura.web;

import ec.tusaas.efactura.dto.emision.ComprobanteResponse;
import ec.tusaas.efactura.dto.emision.DocumentoModificadoRequest;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.service.DocumentoModificadoEmisionService;
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
@RequestMapping("/api/web/v1/ventas/notas-debito")
@RequiredArgsConstructor
public class NotaDebitoController {

  private static final String TIPO = "NOTA_DEBITO";

  private final EmpresaContextoResolver empresaContextoResolver;
  private final DocumentoModificadoEmisionService documentoModificadoEmisionService;

  @PostMapping("/borrador")
  @PreAuthorize(
      "hasAuthority('FACTURA_EMITIR') or hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN') "
          + "or hasAuthority('VENTAS_GESTIONAR')")
  public ComprobanteResponse guardarBorrador(
      @RequestParam(required = false) UUID empresaId,
      @AuthenticationPrincipal UsuarioPrincipal principal,
      @Valid @RequestBody DocumentoModificadoRequest request) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return documentoModificadoEmisionService.guardarBorrador(eid, request, principal, TIPO);
  }

  @PutMapping("/borrador/{id}")
  @PreAuthorize(
      "hasAuthority('FACTURA_EMITIR') or hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN') "
          + "or hasAuthority('VENTAS_GESTIONAR')")
  public ComprobanteResponse actualizarBorrador(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @AuthenticationPrincipal UsuarioPrincipal principal,
      @Valid @RequestBody DocumentoModificadoRequest request) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return documentoModificadoEmisionService.actualizarBorrador(eid, id, request, principal, TIPO);
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
    return documentoModificadoEmisionService.emitirBorrador(eid, id, idempotencyKey, principal, TIPO);
  }
}
