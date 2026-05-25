package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.documento.DocumentoElectronicoRequest;
import ec.tusaas.efactura.dto.emision.ComprobanteResponse;
import ec.tusaas.efactura.dto.emision.RetencionImpuestoRequest;
import ec.tusaas.efactura.dto.emision.RetencionRequest;
import ec.tusaas.efactura.emision.XmlRetencionGeneratorService;
import ec.tusaas.efactura.entity.Certificado;
import ec.tusaas.efactura.entity.Comprobante;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.entity.PuntoEmision;
import ec.tusaas.efactura.repository.CertificadoRepository;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.sri.ClaveAccesoGenerator;
import ec.tusaas.efactura.sri.ComprobanteSriReemisionSupport;
import ec.tusaas.efactura.sri.xml.XmlXsdValidatorService;
import ec.tusaas.efactura.tributario.TiposComprobanteSri;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RetencionElectronicaService {

  private static final String TIPO = "RETENCION";
  private static final String ESTADO_BORRADOR = "BORRADOR";
  private static final DateTimeFormatter PERIODO = DateTimeFormatter.ofPattern("MM/yyyy");

  private final CertificadoRepository certificadoRepository;
  private final SecuencialService secuencialService;
  private final DocumentoEmisionSupport emisionSupport;
  private final ComprobanteSriProcesoService sriProcesoService;
  private final XmlRetencionGeneratorService xmlRetencionGeneratorService;
  private final XmlXsdValidatorService xmlXsdValidatorService;
  private final AuditoriaService auditoriaService;

  @Transactional
  public ComprobanteResponse guardarBorradorDesdeDocumento(
      UUID empresaId, DocumentoElectronicoRequest body, UsuarioPrincipal principal) {
    return guardarBorrador(empresaId, requestDesdeDocumento(body), principal);
  }

  @Transactional
  public ComprobanteResponse guardarBorrador(UUID empresaId, RetencionRequest request, UsuarioPrincipal principal) {
    return persistirBorrador(empresaId, null, request, principal);
  }

  @Transactional
  public ComprobanteResponse actualizarBorrador(
      UUID empresaId, UUID id, RetencionRequest request, UsuarioPrincipal principal) {
    Comprobante existente = emisionSupport.buscarComprobante(empresaId, id);
    if (!TIPO.equalsIgnoreCase(existente.getTipo())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El comprobante no es retencion");
    }
    if (!ESTADO_BORRADOR.equalsIgnoreCase(existente.getEstadoSri())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se pueden editar retenciones en borrador");
    }
    return persistirBorrador(empresaId, existente, request, principal);
  }

  @Transactional
  public ComprobanteResponse emitirBorrador(
      UUID empresaId, UUID id, String idempotencyKey, UsuarioPrincipal principal) {
    Comprobante c = emisionSupport.buscarComprobante(empresaId, id);
    if (!TIPO.equalsIgnoreCase(c.getTipo())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El comprobante no es retencion");
    }
    if (!ESTADO_BORRADOR.equalsIgnoreCase(c.getEstadoSri())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El comprobante no esta en borrador");
    }
    RetencionRequest request = requestDesdeBorrador(c);
    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
      c.setIdempotencyKey(idempotencyKey.trim());
    }
    return completarEmision(empresaId, c, request, principal);
  }

  @Transactional
  public ComprobanteResponse reemitirAlSri(UUID empresaId, UUID id) {
    Comprobante c = emisionSupport.buscarComprobante(empresaId, id);
    if (!TIPO.equalsIgnoreCase(c.getTipo())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El comprobante no es retencion");
    }
    ComprobanteSriReemisionSupport.validarPuedeReemitir(c.getEstadoSri());
    RetencionRequest request = requestDesdeBorrador(c);
    Empresa empresa = c.getEmpresa();
    Certificado certificado =
        certificadoRepository
            .findFirstByEmpresa_IdAndActivoParaFirmaIsTrue(empresaId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.PRECONDITION_REQUIRED, "No existe certificado activo para firma"));
    String xml = xmlRetencionGeneratorService.generarXmlInicial(c, request);
    sriProcesoService.ejecutar(c, empresa, certificado, xml, xmlXsdValidatorService::validarRetencion);
    sriProcesoService.asegurarRide(c);
    return emisionSupport.toResponse(c);
  }

  private ComprobanteResponse persistirBorrador(
      UUID empresaId, Comprobante existente, RetencionRequest request, UsuarioPrincipal principal) {
    PuntoEmision punto = emisionSupport.resolverPunto(empresaId, request.puntoEmisionId());
    Empresa empresa = punto.getEmpresa();
    BigDecimal totalRetenido = totalRetenido(request.impuestos());
    Comprobante c = existente != null ? existente : new Comprobante();
    if (existente == null) {
      UUID provisional = UUID.randomUUID();
      c.setSecuencial(DocumentoEmisionSupport.secuencialProvisional(provisional));
      c.setClaveAcceso(DocumentoEmisionSupport.claveProvisional(provisional));
      c.setTipo(TIPO);
      c.setTipoCodigo(TiposComprobanteSri.RETENCION);
      c.setOrigen("WEB");
      c.setUsuarioCreacion(principal.getEmail());
    }
    c.setEmpresa(empresa);
    c.setEstablecimientoCodigo(punto.getEstablecimiento().getCodigo());
    c.setPuntoEmisionCodigo(punto.getCodigo());
    c.setFechaEmision(request.fechaEmisionOrToday());
    c.setRazonSocialReceptor(request.razonSocialSujetoRetenido());
    c.setIdentificacionReceptor(request.identificacionSujetoRetenido());
    c.setValorTotal(totalRetenido);
    c.setIvaTotal(totalRetenido);
    c.setAmbienteSri(empresa.getAmbienteSri());
    c.setTipoEmision(empresa.getTipoEmision());
    c.setEstadoSri(ESTADO_BORRADOR);
    c.setCustomData(customDataEnriquecida(request));
    DocumentoEmisionSupport.marcarModificacion(c, principal.getEmail());
    c = emisionSupport.guardarComprobante(c);
    return emisionSupport.toResponse(c);
  }

  private ComprobanteResponse completarEmision(
      UUID empresaId, Comprobante c, RetencionRequest request, UsuarioPrincipal principal) {
    PuntoEmision punto = emisionSupport.resolverPunto(empresaId, request.puntoEmisionId());
    Empresa empresa = punto.getEmpresa();
    Certificado certificado =
        certificadoRepository
            .findFirstByEmpresa_IdAndActivoParaFirmaIsTrue(empresaId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.PRECONDITION_REQUIRED, "No existe certificado activo para firma"));
    BigDecimal totalRetenido = totalRetenido(request.impuestos());
    long sec = secuencialService.reservarSiguiente(empresaId, punto.getId(), TiposComprobanteSri.RETENCION, principal);
    String secuencial9 = String.format("%09d", sec);
    String claveAcceso =
        ClaveAccesoGenerator.generar(
            request.fechaEmisionOrToday(),
            TiposComprobanteSri.RETENCION,
            empresa.getRuc(),
            empresa.getAmbienteSri(),
            empresa.getTipoEmision(),
            punto.getEstablecimiento().getCodigo(),
            punto.getCodigo(),
            sec,
            ClaveAccesoGenerator.ochoDigitosAleatorios());
    c.setEstablecimientoCodigo(punto.getEstablecimiento().getCodigo());
    c.setPuntoEmisionCodigo(punto.getCodigo());
    c.setSecuencial(secuencial9);
    c.setClaveAcceso(claveAcceso);
    c.setFechaEmision(request.fechaEmisionOrToday());
    c.setRazonSocialReceptor(request.razonSocialSujetoRetenido());
    c.setIdentificacionReceptor(request.identificacionSujetoRetenido());
    c.setValorTotal(totalRetenido);
    c.setIvaTotal(totalRetenido);
    c.setCustomData(customDataEnriquecida(request));
    DocumentoEmisionSupport.marcarModificacion(c, principal.getEmail());
    c = emisionSupport.guardarComprobante(c);
    String xml = xmlRetencionGeneratorService.generarXmlInicial(c, request);
    sriProcesoService.ejecutar(c, empresa, certificado, xml, xmlXsdValidatorService::validarRetencion);
    sriProcesoService.asegurarRide(c);
    auditoriaService.registrar("RETENCION_EMITIDA", empresaId, principal.getEmail(), "Comprobante", c.getId(), Map.of());
    return emisionSupport.toResponse(c);
  }

  private static BigDecimal totalRetenido(List<RetencionImpuestoRequest> impuestos) {
    return impuestos.stream()
        .map(RetencionImpuestoRequest::valorRetenido)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .setScale(2, RoundingMode.HALF_UP);
  }

  private static RetencionRequest requestDesdeDocumento(DocumentoElectronicoRequest body) {
    Map<String, Object> cd = new HashMap<>(body.safeCustomData());
    LocalDate fecha = body.fechaEmisionOrToday();
    String periodo = fecha.format(PERIODO);
    BigDecimal base = body.subtotalSinImpuestos() == null ? BigDecimal.ZERO : body.subtotalSinImpuestos();
    BigDecimal retenido = body.valorTotal() == null ? body.ivaTotal() : body.valorTotal();
    if (retenido == null) {
      retenido = BigDecimal.ZERO;
    }
    BigDecimal pct = BigDecimal.ZERO;
    if (base.signum() > 0 && retenido.signum() > 0) {
      pct = retenido.multiply(BigDecimal.valueOf(100)).divide(base, 2, RoundingMode.HALF_UP);
    }
    List<RetencionImpuestoRequest> impuestos =
        List.of(
            new RetencionImpuestoRequest(
                "1",
                str(cd, "codigoRetencion", "303"),
                base,
                pct,
                retenido,
                "01",
                str(cd, "numDocSustento", null),
                null));
    return new RetencionRequest(
        body.puntoEmisionId(),
        body.fechaEmision(),
        periodo,
        body.tipoIdentificacionReceptor(),
        body.identificacionReceptor(),
        body.razonSocialReceptor(),
        impuestos,
        cd);
  }

  private RetencionRequest requestDesdeBorrador(Comprobante c) {
    Map<String, Object> cd = c.getCustomData() == null ? Map.of() : c.getCustomData();
    UUID puntoId = UUID.fromString(String.valueOf(cd.get("puntoEmisionId")));
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> raw = (List<Map<String, Object>>) cd.get("impuestos");
    List<RetencionImpuestoRequest> impuestos =
        raw == null || raw.isEmpty() ? requestDesdeDocumentoFallback(c, cd) : impuestosDesdeMaps(raw);
    return new RetencionRequest(
        puntoId,
        c.getFechaEmision(),
        String.valueOf(cd.getOrDefault("periodoFiscal", c.getFechaEmision().format(PERIODO))),
        String.valueOf(cd.getOrDefault("tipoIdentificacionSujetoRetenido", "04")),
        c.getIdentificacionReceptor(),
        c.getRazonSocialReceptor(),
        impuestos,
        cd);
  }

  private List<RetencionImpuestoRequest> requestDesdeDocumentoFallback(Comprobante c, Map<String, Object> cd) {
    BigDecimal retenido = c.getValorTotal() == null ? BigDecimal.ZERO : c.getValorTotal();
    return List.of(
        new RetencionImpuestoRequest("1", "303", retenido, BigDecimal.ZERO, retenido, "01", null, c.getFechaEmision()));
  }

  private static List<RetencionImpuestoRequest> impuestosDesdeMaps(List<Map<String, Object>> raw) {
    return raw.stream()
        .map(
            m ->
                new RetencionImpuestoRequest(
                    String.valueOf(m.getOrDefault("codigo", "1")),
                    String.valueOf(m.getOrDefault("codigoRetencion", "303")),
                    num(m.get("baseImponible")),
                    num(m.get("porcentajeRetener")),
                    num(m.get("valorRetenido")),
                    m.get("codDocSustento") != null ? String.valueOf(m.get("codDocSustento")) : "01",
                    m.get("numDocSustento") != null ? String.valueOf(m.get("numDocSustento")) : null,
                    null))
        .toList();
  }

  private static BigDecimal num(Object v) {
    if (v instanceof Number n) {
      return BigDecimal.valueOf(n.doubleValue());
    }
    try {
      return new BigDecimal(String.valueOf(v));
    } catch (Exception e) {
      return BigDecimal.ZERO;
    }
  }

  private static Map<String, Object> customDataEnriquecida(RetencionRequest request) {
    Map<String, Object> cd = new HashMap<>(request.safeCustomData());
    cd.put("puntoEmisionId", request.puntoEmisionId().toString());
    cd.put("periodoFiscal", request.periodoFiscal());
    cd.put("tipoIdentificacionSujetoRetenido", request.tipoIdentificacionSujetoRetenido());
    cd.put(
        "impuestos",
        request.impuestos().stream()
            .map(
                i ->
                    Map.<String, Object>of(
                        "codigo", i.codigo(),
                        "codigoRetencion", i.codigoRetencion(),
                        "baseImponible", i.baseImponible(),
                        "porcentajeRetener", i.porcentajeRetener(),
                        "valorRetenido", i.valorRetenido(),
                        "codDocSustento", i.codDocSustento() == null ? "01" : i.codDocSustento(),
                        "numDocSustento", i.numDocSustento()))
            .toList());
    return cd;
  }

  private static String str(Map<String, Object> cd, String key, String def) {
    Object v = cd.get(key);
    return v == null ? def : String.valueOf(v);
  }
}
