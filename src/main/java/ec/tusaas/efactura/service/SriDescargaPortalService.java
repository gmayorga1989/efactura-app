package ec.tusaas.efactura.service;

import ec.tusaas.efactura.config.props.SriDownloadIntegrationProperties;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.dto.sridescarga.SriComprobanteResumenMensualResponse;
import ec.tusaas.efactura.integration.sridownload.SriDownloadIntegrationClient;
import ec.tusaas.efactura.integration.sridownload.SriDownloadSyncEventStreamClient;
import ec.tusaas.efactura.repository.EmpresaRepository;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SriDescargaPortalService {

  private final SriDownloadIntegrationProperties integrationProperties;
  private final SriDownloadIntegrationClient integrationClient;
  private final SriDownloadSyncEventStreamClient syncEventStreamClient;
  private final EmpresaRepository empresaRepository;
  private final EmpresaTributarioService empresaTributarioService;

  @Transactional(readOnly = true)
  public SriPortalCredentialStatusResponse obtenerEstado(UUID empresaId, UsuarioPrincipal principal) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    if (!integrationProperties.enabled()) {
      return disabledStatus();
    }
    try {
      Map<String, Object> remote = integrationClient.getCredentialStatus(empresaId.toString());
      return mapStatus(remote);
    } catch (HttpStatusCodeException ex) {
      throw mapRemoteError(ex);
    }
  }

  @Transactional
  public SriPortalCredentialStatusResponse guardar(
      UUID empresaId, SriPortalCredentialUpsertRequest request, UsuarioPrincipal principal) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    assertEnabled();
    Empresa empresa =
        empresaRepository.findById(empresaId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("ruc", empresa.getRuc());
    body.put("razonSocial", empresa.getRazonSocial());
    body.put("ambiente", ambientePortal(empresa.getAmbienteSri()));
    body.put("portalUsuario", request.portalUsuario().trim());
    body.put("portalClave", request.portalClave());

    try {
      Map<String, Object> remote = integrationClient.upsertCredentials(empresaId.toString(), body);
      return mapStatus(remote);
    } catch (HttpStatusCodeException ex) {
      throw mapRemoteError(ex);
    }
  }

  @Transactional(readOnly = true)
  public SriSyncRunResponse consultarUltimoSync(UUID empresaId, UsuarioPrincipal principal) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    assertEnabled();
    try {
      Map<String, Object> remote = integrationClient.getLatestSync(empresaId.toString());
      return mapSyncRun(remote);
    } catch (HttpStatusCodeException ex) {
      if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aún no hay sincronizaciones SRI");
      }
      throw mapRemoteError(ex);
    }
  }

  @Transactional
  public SriSyncRunResponse sincronizarAhora(
      UUID empresaId, UsuarioPrincipal principal, SriSyncTriggerRequest request) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    assertEnabled();
    Empresa empresa =
        empresaRepository.findById(empresaId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    asegurarProvision(empresa);
    Map<String, Object> body = new LinkedHashMap<>();
    if (request != null) {
      if (request.fechaDesde() != null) {
        body.put("fechaDesde", request.fechaDesde().toString());
      }
      if (request.fechaHasta() != null) {
        body.put("fechaHasta", request.fechaHasta().toString());
      }
    }
    try {
      Map<String, Object> remote = integrationClient.triggerSync(empresaId.toString(), body);
      return mapSyncRun(remote);
    } catch (HttpStatusCodeException ex) {
      if (ex.getStatusCode() == HttpStatus.CONFLICT) {
        return consultarUltimoSync(empresaId, principal);
      }
      throw mapRemoteError(ex);
    }
  }

  private void asegurarProvision(Empresa empresa) {
    Map<String, Object> status = integrationClient.getIntegrationStatus(empresa.getId().toString());
    if (Boolean.TRUE.equals(status.get("provisioned"))) {
      return;
    }
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("empresaId", empresa.getId().toString());
    body.put("ruc", empresa.getRuc());
    body.put("razonSocial", empresa.getRazonSocial());
    body.put("ambiente", empresa.getAmbienteSri() == 2 ? "PRODUCCION" : "PRUEBAS");
    integrationClient.provision(body);
  }

  @Transactional(readOnly = true)
  public SriPagedResponse<SriComprobanteRecibidoResponse> listarComprobantes(
      UUID empresaId,
      int page,
      int size,
      LocalDate fechaDesde,
      LocalDate fechaHasta,
      String claveAcceso,
      String rucEmisor,
      String razonSocial,
      UsuarioPrincipal principal) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    assertEnabled();
    try {
      Map<String, Object> remote =
          integrationClient.listComprobantes(
              empresaId.toString(), page, size, fechaDesde, fechaHasta, claveAcceso, rucEmisor, razonSocial);
      return mapPagedComprobantes(remote);
    } catch (HttpStatusCodeException ex) {
      throw mapRemoteError(ex);
    }
  }

  @Transactional(readOnly = true)
  public List<SriComprobanteResumenMensualResponse> resumenMensual(
      UUID empresaId, int anio, UsuarioPrincipal principal) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    assertEnabled();
    try {
      List<Map<String, Object>> remote =
          integrationClient.resumenMensual(empresaId.toString(), anio > 0 ? anio : LocalDate.now().getYear());
      return remote.stream().map(this::mapResumenMensual).toList();
    } catch (HttpStatusCodeException ex) {
      throw mapRemoteError(ex);
    }
  }

  @Transactional(readOnly = true)
  public byte[] descargarXmlComprobante(UUID empresaId, UUID comprobanteId, UsuarioPrincipal principal) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    assertEnabled();
    try {
      byte[] xml = integrationClient.downloadComprobanteXml(empresaId.toString(), comprobanteId);
      if (xml == null || xml.length == 0) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "XML no disponible");
      }
      return xml;
    } catch (HttpStatusCodeException ex) {
      throw mapRemoteError(ex);
    }
  }

  public org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody streamSyncEvents(
      UUID empresaId, UUID syncRunId, UsuarioPrincipal principal) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    assertEnabled();
    return syncEventStreamClient.openStream(
        integrationClient.syncEventsUrl(empresaId.toString(), syncRunId));
  }

  @Transactional(readOnly = true)
  public SriComprobanteRecibidoResponse obtenerComprobante(
      UUID empresaId, UUID comprobanteId, UsuarioPrincipal principal) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    assertEnabled();
    try {
      Map<String, Object> remote = integrationClient.getComprobante(empresaId.toString(), comprobanteId);
      return mapComprobante(remote);
    } catch (HttpStatusCodeException ex) {
      throw mapRemoteError(ex);
    }
  }

  @Transactional(readOnly = true)
  public List<SriSyncRunResponse> listarSyncRuns(UUID empresaId, int limit, UsuarioPrincipal principal) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    assertEnabled();
    try {
      List<Map<String, Object>> remote = integrationClient.listSyncRuns(empresaId.toString(), limit);
      return remote.stream().map(this::mapSyncRun).toList();
    } catch (HttpStatusCodeException ex) {
      throw mapRemoteError(ex);
    }
  }

  @Transactional(readOnly = true)
  public List<SriBotLogResponse> listarBotLogs(
      UUID empresaId, UUID syncRunId, UsuarioPrincipal principal) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    assertEnabled();
    try {
      List<Map<String, Object>> remote = integrationClient.listBotLogs(empresaId.toString(), syncRunId);
      return remote.stream().map(this::mapBotLog).toList();
    } catch (HttpStatusCodeException ex) {
      throw mapRemoteError(ex);
    }
  }

  @Transactional
  public void eliminar(UUID empresaId, UsuarioPrincipal principal) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    assertEnabled();
    try {
      integrationClient.deleteCredentials(empresaId.toString());
    } catch (HttpStatusCodeException ex) {
      throw mapRemoteError(ex);
    }
  }

  @Transactional(readOnly = true)
  public SriApiKeyStatusResponse apiKeyStatus(UUID empresaId, UsuarioPrincipal principal) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    assertEnabled();
    try {
      Map<String, Object> remote = integrationClient.getApiKeyStatus(empresaId.toString());
      return mapApiKeyStatus(remote);
    } catch (HttpStatusCodeException ex) {
      throw mapRemoteError(ex);
    }
  }

  @Transactional
  public SriApiKeyCreatedResponse generarApiKey(UUID empresaId, UsuarioPrincipal principal) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    assertEnabled();
    asegurarProvision(
        empresaRepository.findById(empresaId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
    try {
      Map<String, Object> remote = integrationClient.generateApiKey(empresaId.toString());
      return mapApiKeyCreated(remote);
    } catch (HttpStatusCodeException ex) {
      throw mapRemoteError(ex);
    }
  }

  @Transactional
  public void revocarApiKey(UUID empresaId, UsuarioPrincipal principal) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    assertEnabled();
    try {
      integrationClient.revokeApiKey(empresaId.toString());
    } catch (HttpStatusCodeException ex) {
      throw mapRemoteError(ex);
    }
  }

  @Transactional(readOnly = true)
  public List<SriWebhookResponse> listarWebhooks(UUID empresaId, UsuarioPrincipal principal) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    assertEnabled();
    try {
      List<Map<String, Object>> remote = integrationClient.listWebhooks(empresaId.toString());
      return remote.stream().map(this::mapWebhook).toList();
    } catch (HttpStatusCodeException ex) {
      throw mapRemoteError(ex);
    }
  }

  @Transactional
  public SriWebhookResponse guardarWebhook(
      UUID empresaId, SriWebhookUpsertRequest request, UsuarioPrincipal principal) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    assertEnabled();
    asegurarProvision(
        empresaRepository.findById(empresaId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
    Map<String, Object> body = new LinkedHashMap<>();
    if (request.id() != null) {
      body.put("id", request.id().toString());
    }
    body.put("url", request.url());
    if (request.secret() != null && !request.secret().isBlank()) {
      body.put("secret", request.secret());
    }
    if (request.eventos() != null && !request.eventos().isEmpty()) {
      body.put("eventos", request.eventos());
    }
    try {
      Map<String, Object> remote = integrationClient.upsertWebhook(empresaId.toString(), body);
      return mapWebhook(remote);
    } catch (HttpStatusCodeException ex) {
      throw mapRemoteError(ex);
    }
  }

  @Transactional
  public void eliminarWebhook(UUID empresaId, UUID webhookId, UsuarioPrincipal principal) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    assertEnabled();
    try {
      integrationClient.deleteWebhook(empresaId.toString(), webhookId);
    } catch (HttpStatusCodeException ex) {
      throw mapRemoteError(ex);
    }
  }

  private void assertEnabled() {
    if (!integrationProperties.enabled()) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Servicio de descarga SRI no habilitado en este entorno");
    }
  }

  private static SriPortalCredentialStatusResponse disabledStatus() {
    return new SriPortalCredentialStatusResponse(false, false, null, false, null, null);
  }

  private SriPortalCredentialStatusResponse mapStatus(Map<String, Object> remote) {
    if (remote == null) {
      return new SriPortalCredentialStatusResponse(true, false, null, false, null, null);
    }
    UUID subscriberId = parseUuid(remote.get("subscriberId"));
    boolean configured = Boolean.TRUE.equals(remote.get("configured"));
    String masked = remote.get("portalUsuarioMasked") != null ? remote.get("portalUsuarioMasked").toString() : null;
    Instant vigenteDesde = parseInstant(remote.get("vigenteDesde"));
    return new SriPortalCredentialStatusResponse(
        true, subscriberId != null, subscriberId, configured, masked, vigenteDesde);
  }

  private static String ambientePortal(short ambienteSri) {
    return ambienteSri == 2 ? "PRODUCCION" : "PRUEBAS";
  }

  private static UUID parseUuid(Object value) {
    if (value == null) {
      return null;
    }
    return UUID.fromString(value.toString());
  }

  private SriSyncRunResponse mapSyncRun(Map<String, Object> remote) {
    if (remote == null) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Respuesta vacía del servicio de descarga SRI");
    }
    return new SriSyncRunResponse(
        parseUuid(remote.get("id")),
        parseUuid(remote.get("subscriberId")),
        stringVal(remote.get("tipo")),
        stringVal(remote.get("estado")),
        remote.get("fechaDesde") != null ? LocalDate.parse(remote.get("fechaDesde").toString()) : null,
        remote.get("fechaHasta") != null ? LocalDate.parse(remote.get("fechaHasta").toString()) : null,
        intVal(remote.get("comprobantesNuevos")),
        intVal(remote.get("comprobantesDuplicados")),
        stringVal(remote.get("mensaje")),
        parseInstant(remote.get("iniciadoEn")),
        parseInstant(remote.get("finalizadoEn")));
  }

  private static String stringVal(Object value) {
    return value != null ? value.toString() : null;
  }

  private static int intVal(Object value) {
    if (value instanceof Number n) {
      return n.intValue();
    }
    return value != null ? Integer.parseInt(value.toString()) : 0;
  }

  private static Instant parseInstant(Object value) {
    if (value == null) {
      return null;
    }
    return Instant.parse(value.toString());
  }

  @SuppressWarnings("unchecked")
  private SriPagedResponse<SriComprobanteRecibidoResponse> mapPagedComprobantes(Map<String, Object> remote) {
    if (remote == null) {
      return new SriPagedResponse<>(List.of(), 0, 0, 20, 0);
    }
    List<Map<String, Object>> raw = remote.get("content") instanceof List<?> list ? (List<Map<String, Object>>) list : List.of();
    List<SriComprobanteRecibidoResponse> content = new ArrayList<>();
    for (Map<String, Object> item : raw) {
      content.add(mapComprobante(item));
    }
    long total = remote.get("totalElements") instanceof Number n ? n.longValue() : content.size();
    int page = intVal(remote.get("page"));
    int size = intVal(remote.get("size"));
    if (size <= 0) {
      size = 20;
    }
    int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) total / size);
    return new SriPagedResponse<>(content, total, page, size, totalPages);
  }

  private SriComprobanteResumenMensualResponse mapResumenMensual(Map<String, Object> remote) {
    if (remote == null) {
      return new SriComprobanteResumenMensualResponse(LocalDate.now().getYear(), 1, 0, BigDecimal.ZERO);
    }
    int anio = intVal(remote.get("anio"));
    int mes = intVal(remote.get("mes"));
    long total = remote.get("totalComprobantes") instanceof Number n ? n.longValue() : 0L;
    BigDecimal valor = BigDecimal.ZERO;
    if (remote.get("valorTotal") instanceof Number n) {
      valor = BigDecimal.valueOf(n.doubleValue());
    } else if (remote.get("valorTotal") != null) {
      valor = new BigDecimal(remote.get("valorTotal").toString());
    }
    return new SriComprobanteResumenMensualResponse(anio, mes, total, valor);
  }

  private SriComprobanteRecibidoResponse mapComprobante(Map<String, Object> remote) {
    if (remote == null) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Comprobante vacío del servicio SRI");
    }
    LocalDate fechaEmision =
        remote.get("fechaEmision") != null ? LocalDate.parse(remote.get("fechaEmision").toString()) : null;
    BigDecimal valorTotal = null;
    if (remote.get("valorTotal") instanceof Number n) {
      valorTotal = BigDecimal.valueOf(n.doubleValue());
    } else if (remote.get("valorTotal") != null) {
      valorTotal = new BigDecimal(remote.get("valorTotal").toString());
    }
    @SuppressWarnings("unchecked")
    Map<String, Object> metadata =
        remote.get("metadata") instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    return new SriComprobanteRecibidoResponse(
        parseUuid(remote.get("id")),
        stringVal(remote.get("claveAcceso")),
        stringVal(remote.get("tipoComprobante")),
        stringVal(remote.get("rucEmisor")),
        stringVal(remote.get("razonSocialEmisor")),
        fechaEmision,
        parseInstant(remote.get("fechaAutorizacion")),
        valorTotal,
        stringVal(remote.get("xmlStorageKey")),
        stringVal(remote.get("origen")),
        Boolean.TRUE.equals(remote.get("procesado")),
        stringVal(remote.get("estado")),
        parseInstant(remote.get("fechaCreacion")),
        metadata);
  }

  private SriBotLogResponse mapBotLog(Map<String, Object> remote) {
    return new SriBotLogResponse(
        parseUuid(remote.get("id")),
        parseUuid(remote.get("syncRunId")),
        stringVal(remote.get("tipoOperacion")),
        stringVal(remote.get("estado")),
        stringVal(remote.get("mensaje")),
        remote.get("duracionMs") instanceof Number n ? n.intValue() : null,
        parseInstant(remote.get("fecha")));
  }

  private SriApiKeyStatusResponse mapApiKeyStatus(Map<String, Object> remote) {
    if (remote == null) {
      return new SriApiKeyStatusResponse(false, null);
    }
    boolean configured = Boolean.TRUE.equals(remote.get("configured"));
    String masked =
        remote.get("maskedPreview") != null
            ? remote.get("maskedPreview").toString()
            : remote.get("masked") != null ? remote.get("masked").toString() : null;
    return new SriApiKeyStatusResponse(configured, masked);
  }

  private SriApiKeyCreatedResponse mapApiKeyCreated(Map<String, Object> remote) {
    if (remote == null) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Respuesta vacía al generar API key");
    }
    return new SriApiKeyCreatedResponse(
        stringVal(remote.get("apiKey")), stringVal(remote.get("message")));
  }

  @SuppressWarnings("unchecked")
  private SriWebhookResponse mapWebhook(Map<String, Object> remote) {
    if (remote == null) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Webhook vacío del servicio SRI");
    }
    List<String> eventos =
        remote.get("eventos") instanceof List<?> list
            ? list.stream().map(Object::toString).toList()
            : List.of();
    return new SriWebhookResponse(
        parseUuid(remote.get("id")),
        stringVal(remote.get("url")),
        eventos,
        stringVal(remote.get("estado")),
        parseInstant(remote.get("fechaCreacion")));
  }

  private static ResponseStatusException mapRemoteError(HttpStatusCodeException ex) {
    String msg =
        ex.getResponseBodyAsString() != null && !ex.getResponseBodyAsString().isBlank()
            ? ex.getResponseBodyAsString()
            : ex.getMessage();
    return new ResponseStatusException(HttpStatus.valueOf(ex.getStatusCode().value()), msg);
  }
}
