package ec.tusaas.efactura.web;

import ec.tusaas.efactura.dto.sridescarga.SriApiKeyCreatedResponse;
import ec.tusaas.efactura.dto.sridescarga.SriApiKeyStatusResponse;
import ec.tusaas.efactura.dto.sridescarga.SriBotLogResponse;
import ec.tusaas.efactura.dto.sridescarga.SriWebhookResponse;
import ec.tusaas.efactura.dto.sridescarga.SriWebhookUpsertRequest;
import ec.tusaas.efactura.dto.sridescarga.SriComprobanteRecibidoResponse;
import ec.tusaas.efactura.dto.sridescarga.SriPagedResponse;
import ec.tusaas.efactura.dto.sridescarga.SriPortalCredentialStatusResponse;
import ec.tusaas.efactura.dto.sridescarga.SriPortalCredentialUpsertRequest;
import ec.tusaas.efactura.dto.sridescarga.SriSyncTriggerRequest;
import ec.tusaas.efactura.dto.sridescarga.SriSyncRunResponse;
import ec.tusaas.efactura.dto.sridescarga.SriComprobanteResumenMensualResponse;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import java.util.List;
import ec.tusaas.efactura.service.SriDescargaPortalService;
import ec.tusaas.efactura.web.support.EmpresaContextoResolver;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/web/v1/sri-descarga")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PLATFORM_ADMIN') or hasAuthority('EMPRESA_ADMIN')")
public class SriDescargaController {

  private final SriDescargaPortalService sriDescargaPortalService;
  private final EmpresaContextoResolver empresaContextoResolver;

  @GetMapping("/portal-credentials")
  public SriPortalCredentialStatusResponse obtenerCredenciales(
      @RequestParam(required = false) UUID empresaId, @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return sriDescargaPortalService.obtenerEstado(eid, principal);
  }

  @PutMapping("/portal-credentials")
  public SriPortalCredentialStatusResponse guardarCredenciales(
      @RequestParam(required = false) UUID empresaId,
      @Valid @RequestBody SriPortalCredentialUpsertRequest request,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return sriDescargaPortalService.guardar(eid, request, principal);
  }

  @DeleteMapping("/portal-credentials")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminarCredenciales(
      @RequestParam(required = false) UUID empresaId, @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    sriDescargaPortalService.eliminar(eid, principal);
  }

  @GetMapping("/sync/status")
  public SriSyncRunResponse consultarSync(
      @RequestParam(required = false) UUID empresaId, @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return sriDescargaPortalService.consultarUltimoSync(eid, principal);
  }

  @PostMapping("/sync")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public SriSyncRunResponse sincronizar(
      @RequestParam(required = false) UUID empresaId,
      @RequestBody(required = false) SriSyncTriggerRequest request,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return sriDescargaPortalService.sincronizarAhora(eid, principal, request);
  }

  @GetMapping("/comprobantes")
  public SriPagedResponse<SriComprobanteRecibidoResponse> listarComprobantes(
      @RequestParam(required = false) UUID empresaId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) java.time.LocalDate fechaDesde,
      @RequestParam(required = false) java.time.LocalDate fechaHasta,
      @RequestParam(required = false) String claveAcceso,
      @RequestParam(required = false) String rucEmisor,
      @RequestParam(required = false) String razonSocial,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return sriDescargaPortalService.listarComprobantes(
        eid, page, size, fechaDesde, fechaHasta, claveAcceso, rucEmisor, razonSocial, principal);
  }

  @GetMapping("/comprobantes/resumen-mensual")
  public List<SriComprobanteResumenMensualResponse> resumenMensual(
      @RequestParam(required = false) UUID empresaId,
      @RequestParam(required = false) Integer anio,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return sriDescargaPortalService.resumenMensual(eid, anio != null ? anio : java.time.LocalDate.now().getYear(), principal);
  }

  @GetMapping("/comprobantes/{comprobanteId}/xml")
  public ResponseEntity<byte[]> descargarXml(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID comprobanteId,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    byte[] xml = sriDescargaPortalService.descargarXmlComprobante(eid, comprobanteId, principal);
    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=\"" + comprobanteId + ".xml\"")
        .contentType(MediaType.APPLICATION_XML)
        .body(xml);
  }

  @GetMapping(value = "/sync-runs/{syncRunId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public ResponseEntity<StreamingResponseBody> streamSyncEvents(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID syncRunId,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    StreamingResponseBody body = sriDescargaPortalService.streamSyncEvents(eid, syncRunId, principal);
    return ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(body);
  }

  @GetMapping("/comprobantes/{comprobanteId}")
  public SriComprobanteRecibidoResponse obtenerComprobante(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID comprobanteId,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return sriDescargaPortalService.obtenerComprobante(eid, comprobanteId, principal);
  }

  @GetMapping("/sync-runs")
  public List<SriSyncRunResponse> listarSyncRuns(
      @RequestParam(required = false) UUID empresaId,
      @RequestParam(defaultValue = "20") int limit,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return sriDescargaPortalService.listarSyncRuns(eid, limit, principal);
  }

  @GetMapping("/sync-runs/{syncRunId}/bot-logs")
  public List<SriBotLogResponse> listarBotLogs(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID syncRunId,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return sriDescargaPortalService.listarBotLogs(eid, syncRunId, principal);
  }

  @GetMapping("/integration/api-key")
  public SriApiKeyStatusResponse apiKeyStatus(
      @RequestParam(required = false) UUID empresaId, @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return sriDescargaPortalService.apiKeyStatus(eid, principal);
  }

  @PostMapping("/integration/api-key")
  public SriApiKeyCreatedResponse generarApiKey(
      @RequestParam(required = false) UUID empresaId, @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return sriDescargaPortalService.generarApiKey(eid, principal);
  }

  @DeleteMapping("/integration/api-key")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void revocarApiKey(
      @RequestParam(required = false) UUID empresaId, @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    sriDescargaPortalService.revocarApiKey(eid, principal);
  }

  @GetMapping("/integration/webhooks")
  public List<SriWebhookResponse> listarWebhooks(
      @RequestParam(required = false) UUID empresaId, @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return sriDescargaPortalService.listarWebhooks(eid, principal);
  }

  @PutMapping("/integration/webhooks")
  public SriWebhookResponse guardarWebhook(
      @RequestParam(required = false) UUID empresaId,
      @Valid @RequestBody SriWebhookUpsertRequest request,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return sriDescargaPortalService.guardarWebhook(eid, request, principal);
  }

  @DeleteMapping("/integration/webhooks/{webhookId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminarWebhook(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID webhookId,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    sriDescargaPortalService.eliminarWebhook(eid, webhookId, principal);
  }
}
