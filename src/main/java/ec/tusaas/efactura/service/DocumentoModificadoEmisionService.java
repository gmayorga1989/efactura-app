package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.documento.DocumentoElectronicoRequest;
import ec.tusaas.efactura.dto.emision.ComprobanteResponse;
import ec.tusaas.efactura.dto.emision.DocumentoModificadoRequest;
import ec.tusaas.efactura.dto.emision.FacturaItemRequest;
import ec.tusaas.efactura.emision.DocumentoModificadoRideUtil;
import ec.tusaas.efactura.emision.XmlNotaCreditoGeneratorService;
import ec.tusaas.efactura.emision.XmlNotaCreditoGeneratorService.DocumentoModificado;
import ec.tusaas.efactura.emision.XmlNotaDebitoGeneratorService;
import ec.tusaas.efactura.entity.Certificado;
import ec.tusaas.efactura.entity.Comprobante;
import ec.tusaas.efactura.entity.ComprobanteDetalle;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.entity.PuntoEmision;
import ec.tusaas.efactura.repository.CertificadoRepository;
import ec.tusaas.efactura.repository.ComprobanteRepository;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.sri.ClaveAccesoGenerator;
import ec.tusaas.efactura.sri.ComprobanteSriReemisionSupport;
import ec.tusaas.efactura.sri.xml.XmlXsdValidatorService;
import ec.tusaas.efactura.tributario.TiposComprobanteSri;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
public class DocumentoModificadoEmisionService {

  private static final String ESTADO_BORRADOR = "BORRADOR";
  private static final String TIPO_FACTURA = "FACTURA";
  private static final String ESTADO_AUTORIZADO = "AUTORIZADO";

  private final ComprobanteRepository comprobanteRepository;
  private final CertificadoRepository certificadoRepository;
  private final SecuencialService secuencialService;
  private final DocumentoEmisionSupport emisionSupport;
  private final ComprobanteSriProcesoService sriProcesoService;
  private final XmlNotaCreditoGeneratorService xmlNotaCreditoGeneratorService;
  private final XmlNotaDebitoGeneratorService xmlNotaDebitoGeneratorService;
  private final XmlXsdValidatorService xmlXsdValidatorService;
  private final AuditoriaService auditoriaService;

  @Transactional
  public ComprobanteResponse guardarBorradorDesdeDocumento(
      UUID empresaId, DocumentoElectronicoRequest body, UsuarioPrincipal principal, String tipoNombre) {
    Map<String, Object> cd = new HashMap<>(body.safeCustomData());
    if (body.subtotalSinImpuestos() != null) {
      cd.put("subtotalSinImpuestos", body.subtotalSinImpuestos());
    }
    if (body.ivaTotal() != null) {
      cd.put("ivaTotal", body.ivaTotal());
    }
    UUID facturaId = DocumentoEmisionSupport.parseUuid(cd.get("facturaOrigenId"));
    if (facturaId == null) {
      facturaId = DocumentoEmisionSupport.parseUuid(cd.get("facturaModificadaId"));
    }
    String motivo = cd.get("motivo") != null ? String.valueOf(cd.get("motivo")).trim() : "";
    DocumentoModificadoRequest req =
        new DocumentoModificadoRequest(
            body.puntoEmisionId(),
            body.fechaEmision(),
            body.tipoIdentificacionReceptor(),
            body.identificacionReceptor(),
            body.razonSocialReceptor(),
            facturaId,
            motivo,
            null,
            cd);
    return guardarBorrador(empresaId, req, principal, tipoNombre);
  }

  @Transactional
  public ComprobanteResponse guardarBorrador(
      UUID empresaId, DocumentoModificadoRequest request, UsuarioPrincipal principal, String tipoNombre) {
    return persistirBorrador(empresaId, null, request, principal, tipoNombre);
  }

  @Transactional
  public ComprobanteResponse actualizarBorrador(
      UUID empresaId, UUID id, DocumentoModificadoRequest request, UsuarioPrincipal principal, String tipoNombre) {
    Comprobante existente = emisionSupport.buscarComprobante(empresaId, id);
    validarTipo(existente, tipoNombre);
    if (!ESTADO_BORRADOR.equalsIgnoreCase(existente.getEstadoSri())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se pueden editar borradores");
    }
    return persistirBorrador(empresaId, existente, request, principal, tipoNombre);
  }

  @Transactional
  public ComprobanteResponse emitirBorrador(
      UUID empresaId, UUID id, String idempotencyKey, UsuarioPrincipal principal, String tipoNombre) {
    Comprobante c = emisionSupport.buscarComprobante(empresaId, id);
    validarTipo(c, tipoNombre);
    if (!ESTADO_BORRADOR.equalsIgnoreCase(c.getEstadoSri())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El comprobante no esta en borrador");
    }
    DocumentoModificadoRequest request = requestDesdeBorrador(c);
    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
      c.setIdempotencyKey(idempotencyKey.trim());
    }
    return completarEmision(empresaId, c, request, principal, tipoNombre);
  }

  @Transactional
  public ComprobanteResponse reemitirAlSri(UUID empresaId, UUID id, String tipoNombre) {
    Comprobante c = emisionSupport.buscarComprobante(empresaId, id);
    validarTipo(c, tipoNombre);
    ComprobanteSriReemisionSupport.validarPuedeReemitir(c.getEstadoSri());
    TipoConfig cfg = config(tipoNombre);
    DocumentoModificadoRequest request = requestDesdeBorrador(c);
    Empresa empresa = c.getEmpresa();
    Certificado certificado =
        certificadoRepository
            .findFirstByEmpresa_IdAndActivoParaFirmaIsTrue(empresaId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.PRECONDITION_REQUIRED, "No existe certificado activo para firma"));
    Comprobante factura = cargarFacturaModificada(empresaId, request, cfg.etiqueta());
    DocumentoModificado docModificado = documentoModificadoDesde(factura);
    List<FacturaItemRequest> items = resolverItems(empresaId, request);
    String xml = cfg.xmlGenerator().apply(c, request, docModificado, items);
    sriProcesoService.ejecutar(c, empresa, certificado, xml, cfg.xsdValidator());
    sriProcesoService.asegurarRide(c);
    return emisionSupport.toResponse(c);
  }

  private ComprobanteResponse persistirBorrador(
      UUID empresaId,
      Comprobante existente,
      DocumentoModificadoRequest request,
      UsuarioPrincipal principal,
      String tipoNombre) {
    TipoConfig cfg = config(tipoNombre);
    PuntoEmision punto = emisionSupport.resolverPunto(empresaId, request.puntoEmisionId());
    Empresa empresa = punto.getEmpresa();
    List<FacturaItemRequest> items = resolverItems(empresaId, request);
    DocumentoEmisionSupport.Totales totales = DocumentoEmisionSupport.calcularTotales(items);
    Comprobante c = existente != null ? existente : new Comprobante();
    if (existente == null) {
      UUID provisional = UUID.randomUUID();
      c.setSecuencial(DocumentoEmisionSupport.secuencialProvisional(provisional));
      c.setClaveAcceso(DocumentoEmisionSupport.claveProvisional(provisional));
      c.setTipo(cfg.tipoNombre());
      c.setTipoCodigo(cfg.codigoSri());
      c.setOrigen("WEB");
      c.setUsuarioCreacion(principal.getEmail());
    }
    c.setEmpresa(empresa);
    c.setEstablecimientoCodigo(punto.getEstablecimiento().getCodigo());
    c.setPuntoEmisionCodigo(punto.getCodigo());
    c.setFechaEmision(request.fechaEmisionOrToday());
    c.setRazonSocialReceptor(request.razonSocialReceptor());
    c.setIdentificacionReceptor(request.identificacionReceptor());
    DocumentoEmisionSupport.aplicarTotales(c, totales);
    c.setAmbienteSri(empresa.getAmbienteSri());
    c.setTipoEmision(empresa.getTipoEmision());
    c.setEstadoSri(ESTADO_BORRADOR);
    Comprobante facturaRef = cargarFacturaModificadaSiExiste(empresaId, request, cfg.etiqueta());
    c.setCustomData(customDataEnriquecida(request, facturaRef));
    DocumentoEmisionSupport.marcarModificacion(c, principal.getEmail());
    c = emisionSupport.guardarComprobante(c);
    emisionSupport.eliminarDetalles(c.getId());
    emisionSupport.guardarDetalles(c, empresa, items, principal.getEmail());
    return emisionSupport.toResponse(c);
  }

  private ComprobanteResponse completarEmision(
      UUID empresaId,
      Comprobante c,
      DocumentoModificadoRequest request,
      UsuarioPrincipal principal,
      String tipoNombre) {
    TipoConfig cfg = config(tipoNombre);
    PuntoEmision punto = emisionSupport.resolverPunto(empresaId, request.puntoEmisionId());
    Empresa empresa = punto.getEmpresa();
    Certificado certificado =
        certificadoRepository
            .findFirstByEmpresa_IdAndActivoParaFirmaIsTrue(empresaId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.PRECONDITION_REQUIRED, "No existe certificado activo para firma"));
    Comprobante factura = cargarFacturaModificada(empresaId, request, cfg.etiqueta());
    DocumentoModificado docModificado = documentoModificadoDesde(factura);
    List<FacturaItemRequest> items = resolverItems(empresaId, request);
    DocumentoEmisionSupport.Totales totales = DocumentoEmisionSupport.calcularTotales(items);

    long sec = secuencialService.reservarSiguiente(empresaId, punto.getId(), cfg.codigoSri(), principal);
    String secuencial9 = String.format("%09d", sec);
    String claveAcceso =
        ClaveAccesoGenerator.generar(
            request.fechaEmisionOrToday(),
            cfg.codigoSri(),
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
    c.setRazonSocialReceptor(request.razonSocialReceptor());
    c.setIdentificacionReceptor(request.identificacionReceptor());
    DocumentoEmisionSupport.aplicarTotales(c, totales);
    c.setCustomData(customDataEnriquecida(request, factura));
    DocumentoEmisionSupport.marcarModificacion(c, principal.getEmail());
    c = emisionSupport.guardarComprobante(c);
    emisionSupport.eliminarDetalles(c.getId());
    emisionSupport.guardarDetalles(c, empresa, items, principal.getEmail());

    String xml = cfg.xmlGenerator().apply(c, request, docModificado, items);
    sriProcesoService.ejecutar(c, empresa, certificado, xml, cfg.xsdValidator());
    sriProcesoService.asegurarRide(c);
    registrarAuditoria(empresaId, c, principal, cfg.auditEvent());
    return emisionSupport.toResponse(c);
  }

  private List<FacturaItemRequest> resolverItems(UUID empresaId, DocumentoModificadoRequest request) {
    if (request.items() != null && !request.items().isEmpty()) {
      return request.items();
    }
    UUID facturaId = request.facturaModificadaId();
    if (facturaId == null) {
      Map<String, Object> cd = request.safeCustomData();
      facturaId = DocumentoEmisionSupport.parseUuid(cd.get("facturaOrigenId"));
      if (facturaId == null) {
        facturaId = DocumentoEmisionSupport.parseUuid(cd.get("facturaModificadaId"));
      }
    }
    if (facturaId != null) {
      List<ComprobanteDetalle> detalles = emisionSupport.listarDetalles(facturaId);
      if (!detalles.isEmpty()) {
        return detalles.stream().map(emisionSupport::itemDesdeDetalle).toList();
      }
    }
    return List.of(lineaSintetica(request));
  }

  private FacturaItemRequest lineaSintetica(DocumentoModificadoRequest request) {
    Map<String, Object> cd = request.safeCustomData();
    BigDecimal subtotal =
        cd.get("subtotalSinImpuestos") instanceof Number n
            ? BigDecimal.valueOf(n.doubleValue())
            : BigDecimal.ZERO;
    BigDecimal iva = cd.get("ivaTotal") instanceof Number n ? BigDecimal.valueOf(n.doubleValue()) : BigDecimal.ZERO;
    BigDecimal ivaPct = BigDecimal.ZERO;
    if (subtotal.signum() > 0 && iva.signum() > 0) {
      ivaPct = iva.multiply(BigDecimal.valueOf(100)).divide(subtotal, 2, RoundingMode.HALF_UP);
    }
    return new FacturaItemRequest(
        "DOC01",
        null,
        "Ajuste documento modificado",
        BigDecimal.ONE,
        subtotal.max(BigDecimal.ZERO),
        BigDecimal.ZERO,
        ivaPct,
        ivaPct.compareTo(BigDecimal.ZERO) == 0 ? "0" : "4",
        Map.of());
  }

  private Comprobante cargarFacturaModificada(UUID empresaId, DocumentoModificadoRequest request, String etiqueta) {
    UUID facturaId = request.facturaModificadaId();
    if (facturaId == null) {
      Map<String, Object> cd = request.safeCustomData();
      facturaId = DocumentoEmisionSupport.parseUuid(cd.get("facturaOrigenId"));
      if (facturaId == null) {
        facturaId = DocumentoEmisionSupport.parseUuid(cd.get("facturaModificadaId"));
      }
    }
    if (facturaId == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "El documento requiere la factura modificada (facturaModificadaId)");
    }
    Comprobante factura = emisionSupport.buscarComprobante(empresaId, facturaId);
    if (!TIPO_FACTURA.equalsIgnoreCase(factura.getTipo())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El documento sustento debe ser una factura");
    }
    if (!ESTADO_AUTORIZADO.equalsIgnoreCase(factura.getEstadoSri())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Solo se puede modificar contra facturas autorizadas por el SRI");
    }
    return factura;
  }

  private static DocumentoModificado documentoModificadoDesde(Comprobante factura) {
    String num =
        factura.getEstablecimientoCodigo()
            + "-"
            + factura.getPuntoEmisionCodigo()
            + "-"
            + factura.getSecuencial();
    return new DocumentoModificado(num, factura.getFechaEmision());
  }

  private DocumentoModificadoRequest requestDesdeBorrador(Comprobante c) {
    Map<String, Object> cd = c.getCustomData() == null ? Map.of() : c.getCustomData();
    Object puntoRaw = cd.get("puntoEmisionId");
    if (puntoRaw == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Borrador sin punto de emision");
    }
    UUID puntoId = UUID.fromString(String.valueOf(puntoRaw));
    String tipoId = String.valueOf(cd.getOrDefault("tipoIdentificacionReceptor", "04"));
    UUID facturaId = DocumentoEmisionSupport.parseUuid(cd.get("facturaOrigenId"));
    if (facturaId == null) {
      facturaId = DocumentoEmisionSupport.parseUuid(cd.get("facturaModificadaId"));
    }
    String motivo = cd.get("motivo") != null ? String.valueOf(cd.get("motivo")) : "";
    List<FacturaItemRequest> items =
        emisionSupport.listarDetalles(c.getId()).stream().map(emisionSupport::itemDesdeDetalle).toList();
    return new DocumentoModificadoRequest(
        puntoId,
        c.getFechaEmision(),
        tipoId,
        c.getIdentificacionReceptor(),
        c.getRazonSocialReceptor(),
        facturaId,
        motivo,
        items,
        cd);
  }

  private Comprobante cargarFacturaModificadaSiExiste(
      UUID empresaId, DocumentoModificadoRequest request, String etiqueta) {
    try {
      return cargarFacturaModificada(empresaId, request, etiqueta);
    } catch (ResponseStatusException e) {
      if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
        return null;
      }
      throw e;
    }
  }

  private static Map<String, Object> customDataEnriquecida(
      DocumentoModificadoRequest request, Comprobante facturaModificada) {
    Map<String, Object> cd = new HashMap<>(request.safeCustomData());
    cd.put("puntoEmisionId", request.puntoEmisionId().toString());
    cd.put("tipoIdentificacionReceptor", request.tipoIdentificacionReceptor());
    if (request.facturaModificadaId() != null) {
      cd.put("facturaOrigenId", request.facturaModificadaId().toString());
      cd.put("facturaModificadaId", request.facturaModificadaId().toString());
    }
    if (request.motivo() != null && !request.motivo().isBlank()) {
      cd.put("motivo", request.motivo().trim());
    }
    if (facturaModificada != null) {
      DocumentoModificadoRideUtil.enriquecerCustomDataDesdeFactura(cd, facturaModificada);
    }
    return cd;
  }

  private void registrarAuditoria(UUID empresaId, Comprobante c, UsuarioPrincipal principal, String evento) {
    Map<String, Object> detalle = new HashMap<>();
    detalle.put("claveAcceso", c.getClaveAcceso());
    detalle.put("estadoSri", c.getEstadoSri());
    detalle.put(
        "secuencial",
        c.getEstablecimientoCodigo() + "-" + c.getPuntoEmisionCodigo() + "-" + c.getSecuencial());
    detalle.put("valorTotal", c.getValorTotal() == null ? null : c.getValorTotal().toPlainString());
    String actor = principal.getEmail();
    if (principal.getApiKeyId() != null) {
      actor = "apiKey:" + principal.getApiKeyId();
    }
    auditoriaService.registrar(evento, empresaId, actor, "Comprobante", c.getId(), detalle);
  }

  private static void validarTipo(Comprobante c, String tipoNombre) {
    if (!tipoNombre.equalsIgnoreCase(c.getTipo())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Tipo de comprobante no coincide");
    }
  }

  private TipoConfig config(String tipoNombre) {
    if ("NOTA_DEBITO".equalsIgnoreCase(tipoNombre)) {
      return new TipoConfig(
          "NOTA_DEBITO",
          TiposComprobanteSri.NOTA_DEBITO,
          "nota de debito",
          "NOTA_DEBITO_EMITIDA",
          (c, req, doc, items) ->
              xmlNotaDebitoGeneratorService.generarXmlInicial(
                  c, req, new XmlNotaDebitoGeneratorService.DocumentoModificado(doc.numDocModificado(), doc.fechaEmisionDocSustento()), items),
          xml -> xmlXsdValidatorService.validarNotaDebito(xml));
    }
    if ("NOTA_CREDITO".equalsIgnoreCase(tipoNombre)) {
      return new TipoConfig(
          "NOTA_CREDITO",
          TiposComprobanteSri.NOTA_CREDITO,
          "nota de credito",
          "NOTA_CREDITO_EMITIDA",
          (c, req, doc, items) -> xmlNotaCreditoGeneratorService.generarXmlInicial(c, toNcRequest(req), doc, items),
          xml -> xmlXsdValidatorService.validarNotaCredito(xml));
    }
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo no soportado: " + tipoNombre);
  }

  private static ec.tusaas.efactura.dto.emision.NotaCreditoRequest toNcRequest(DocumentoModificadoRequest r) {
    return new ec.tusaas.efactura.dto.emision.NotaCreditoRequest(
        r.puntoEmisionId(),
        r.fechaEmision(),
        r.tipoIdentificacionReceptor(),
        r.identificacionReceptor(),
        r.razonSocialReceptor(),
        r.facturaModificadaId(),
        r.motivo(),
        r.items(),
        r.customData());
  }

  private record TipoConfig(
      String tipoNombre,
      String codigoSri,
      String etiqueta,
      String auditEvent,
      XmlGenerator xmlGenerator,
      java.util.function.Consumer<String> xsdValidator) {}

  @FunctionalInterface
  private interface XmlGenerator {
    String apply(
        Comprobante c,
        DocumentoModificadoRequest req,
        DocumentoModificado doc,
        List<FacturaItemRequest> items);
  }
}
