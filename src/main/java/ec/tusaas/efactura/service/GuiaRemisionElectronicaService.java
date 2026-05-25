package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.documento.DocumentoElectronicoRequest;
import ec.tusaas.efactura.dto.emision.ComprobanteResponse;
import ec.tusaas.efactura.dto.emision.FacturaItemRequest;
import ec.tusaas.efactura.dto.emision.GuiaRemisionRequest;
import ec.tusaas.efactura.emision.XmlGuiaRemisionGeneratorService;
import ec.tusaas.efactura.emision.XmlGuiaRemisionGeneratorService.DocSustento;
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
public class GuiaRemisionElectronicaService {

  private static final String TIPO = "GUIA_REMISION";
  private static final String ESTADO_BORRADOR = "BORRADOR";
  private static final String ESTADO_AUTORIZADO = "AUTORIZADO";

  private final CertificadoRepository certificadoRepository;
  private final SecuencialService secuencialService;
  private final DocumentoEmisionSupport emisionSupport;
  private final ComprobanteSriProcesoService sriProcesoService;
  private final XmlGuiaRemisionGeneratorService xmlGuiaRemisionGeneratorService;
  private final XmlXsdValidatorService xmlXsdValidatorService;
  private final AuditoriaService auditoriaService;

  @Transactional
  public ComprobanteResponse guardarBorradorDesdeDocumento(
      UUID empresaId, DocumentoElectronicoRequest body, UsuarioPrincipal principal) {
    return guardarBorrador(empresaId, requestDesdeDocumento(body), principal);
  }

  @Transactional
  public ComprobanteResponse guardarBorrador(UUID empresaId, GuiaRemisionRequest request, UsuarioPrincipal principal) {
    return persistirBorrador(empresaId, null, request, principal);
  }

  @Transactional
  public ComprobanteResponse actualizarBorrador(
      UUID empresaId, UUID id, GuiaRemisionRequest request, UsuarioPrincipal principal) {
    Comprobante existente = emisionSupport.buscarComprobante(empresaId, id);
    if (!TIPO.equalsIgnoreCase(existente.getTipo())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El comprobante no es guia de remision");
    }
    if (!ESTADO_BORRADOR.equalsIgnoreCase(existente.getEstadoSri())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se pueden editar guias en borrador");
    }
    return persistirBorrador(empresaId, existente, request, principal);
  }

  @Transactional
  public ComprobanteResponse emitirBorrador(
      UUID empresaId, UUID id, String idempotencyKey, UsuarioPrincipal principal) {
    Comprobante c = emisionSupport.buscarComprobante(empresaId, id);
    if (!TIPO.equalsIgnoreCase(c.getTipo())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El comprobante no es guia de remision");
    }
    if (!ESTADO_BORRADOR.equalsIgnoreCase(c.getEstadoSri())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El comprobante no esta en borrador");
    }
    GuiaRemisionRequest request = requestDesdeBorrador(c);
    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
      c.setIdempotencyKey(idempotencyKey.trim());
    }
    return completarEmision(empresaId, c, request, principal);
  }

  @Transactional
  public ComprobanteResponse reemitirAlSri(UUID empresaId, UUID id) {
    Comprobante c = emisionSupport.buscarComprobante(empresaId, id);
    if (!TIPO.equalsIgnoreCase(c.getTipo())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El comprobante no es guia de remision");
    }
    ComprobanteSriReemisionSupport.validarPuedeReemitir(c.getEstadoSri());
    GuiaRemisionRequest request = requestDesdeBorrador(c);
    Empresa empresa = c.getEmpresa();
    Certificado certificado =
        certificadoRepository
            .findFirstByEmpresa_IdAndActivoParaFirmaIsTrue(empresaId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.PRECONDITION_REQUIRED, "No existe certificado activo para firma"));
    DocSustento docSustento = resolverDocSustento(empresaId, request);
    List<FacturaItemRequest> items = resolverItems(request);
    String xml = xmlGuiaRemisionGeneratorService.generarXmlInicial(c, request, docSustento, items);
    sriProcesoService.ejecutar(c, empresa, certificado, xml, xmlXsdValidatorService::validarGuiaRemision);
    sriProcesoService.asegurarRide(c);
    return emisionSupport.toResponse(c);
  }

  private ComprobanteResponse persistirBorrador(
      UUID empresaId, Comprobante existente, GuiaRemisionRequest request, UsuarioPrincipal principal) {
    PuntoEmision punto = emisionSupport.resolverPunto(empresaId, request.puntoEmisionId());
    Empresa empresa = punto.getEmpresa();
    List<FacturaItemRequest> items = resolverItems(request);
    Comprobante c = existente != null ? existente : new Comprobante();
    if (existente == null) {
      UUID provisional = UUID.randomUUID();
      c.setSecuencial(DocumentoEmisionSupport.secuencialProvisional(provisional));
      c.setClaveAcceso(DocumentoEmisionSupport.claveProvisional(provisional));
      c.setTipo(TIPO);
      c.setTipoCodigo(TiposComprobanteSri.GUIA_REMISION);
      c.setOrigen("WEB");
      c.setUsuarioCreacion(principal.getEmail());
    }
    c.setEmpresa(empresa);
    c.setEstablecimientoCodigo(punto.getEstablecimiento().getCodigo());
    c.setPuntoEmisionCodigo(punto.getCodigo());
    c.setFechaEmision(request.fechaEmisionOrToday());
    c.setRazonSocialReceptor(request.razonSocialDestinatario());
    c.setIdentificacionReceptor(request.identificacionDestinatario());
    c.setValorTotal(BigDecimal.ZERO);
    c.setAmbienteSri(empresa.getAmbienteSri());
    c.setTipoEmision(empresa.getTipoEmision());
    c.setEstadoSri(ESTADO_BORRADOR);
    c.setCustomData(customDataEnriquecida(empresaId, request));
    DocumentoEmisionSupport.marcarModificacion(c, principal.getEmail());
    c = emisionSupport.guardarComprobante(c);
    emisionSupport.eliminarDetalles(c.getId());
    emisionSupport.guardarDetalles(c, empresa, items, principal.getEmail());
    return emisionSupport.toResponse(c);
  }

  private ComprobanteResponse completarEmision(
      UUID empresaId, Comprobante c, GuiaRemisionRequest request, UsuarioPrincipal principal) {
    PuntoEmision punto = emisionSupport.resolverPunto(empresaId, request.puntoEmisionId());
    Empresa empresa = punto.getEmpresa();
    Certificado certificado =
        certificadoRepository
            .findFirstByEmpresa_IdAndActivoParaFirmaIsTrue(empresaId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.PRECONDITION_REQUIRED, "No existe certificado activo para firma"));
    DocSustento docSustento = resolverDocSustento(empresaId, request);
    List<FacturaItemRequest> items = resolverItems(request);
    long sec = secuencialService.reservarSiguiente(empresaId, punto.getId(), TiposComprobanteSri.GUIA_REMISION, principal);
    String secuencial9 = String.format("%09d", sec);
    String claveAcceso =
        ClaveAccesoGenerator.generar(
            request.fechaEmisionOrToday(),
            TiposComprobanteSri.GUIA_REMISION,
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
    c.setRazonSocialReceptor(request.razonSocialDestinatario());
    c.setIdentificacionReceptor(request.identificacionDestinatario());
    c.setCustomData(customDataEnriquecida(empresaId, request));
    DocumentoEmisionSupport.marcarModificacion(c, principal.getEmail());
    c = emisionSupport.guardarComprobante(c);
    emisionSupport.eliminarDetalles(c.getId());
    emisionSupport.guardarDetalles(c, empresa, items, principal.getEmail());
    String xml = xmlGuiaRemisionGeneratorService.generarXmlInicial(c, request, docSustento, items);
    sriProcesoService.ejecutar(c, empresa, certificado, xml, xmlXsdValidatorService::validarGuiaRemision);
    sriProcesoService.asegurarRide(c);
    auditoriaService.registrar("GUIA_REMISION_EMITIDA", empresaId, principal.getEmail(), "Comprobante", c.getId(), Map.of());
    return emisionSupport.toResponse(c);
  }

  private DocSustento resolverDocSustento(UUID empresaId, GuiaRemisionRequest request) {
    UUID facturaId = request.facturaSustentoId();
    if (facturaId == null) {
      facturaId = DocumentoEmisionSupport.parseUuid(request.safeCustomData().get("facturaSustentoId"));
    }
    if (facturaId == null) {
      return null;
    }
    Comprobante factura = emisionSupport.buscarComprobante(empresaId, facturaId);
    if (!ESTADO_AUTORIZADO.equalsIgnoreCase(factura.getEstadoSri())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "La factura sustento debe estar autorizada");
    }
    String num =
        factura.getEstablecimientoCodigo() + "-" + factura.getPuntoEmisionCodigo() + "-" + factura.getSecuencial();
    return new DocSustento(num, factura.getFechaEmision());
  }

  private List<FacturaItemRequest> resolverItems(GuiaRemisionRequest request) {
    if (request.items() != null && !request.items().isEmpty()) {
      return request.items();
    }
    return List.of(
        new FacturaItemRequest("GR01", null, "Traslado mercaderia", BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, "0", Map.of()));
  }

  private static GuiaRemisionRequest requestDesdeDocumento(DocumentoElectronicoRequest body) {
    Map<String, Object> cd = new HashMap<>(body.safeCustomData());
    String dirPartida = str(cd, "dirPartida", "Direccion partida");
    String placa = str(cd, "placa", "AAA0000");
    String motivo = str(cd, "motivoTraslado", "Traslado");
    String dirDest = str(cd, "dirDestinatario", "Direccion destino");
    return new GuiaRemisionRequest(
        body.puntoEmisionId(),
        body.fechaEmision(),
        dirPartida,
        str(cd, "tipoIdentificacionTransportista", "04"),
        str(cd, "identificacionTransportista", body.identificacionReceptor()),
        str(cd, "razonSocialTransportista", body.razonSocialReceptor()),
        null,
        null,
        placa,
        body.tipoIdentificacionReceptor(),
        body.identificacionReceptor(),
        body.razonSocialReceptor(),
        dirDest,
        motivo,
        DocumentoEmisionSupport.parseUuid(cd.get("facturaSustentoId")),
        null,
        cd);
  }

  private GuiaRemisionRequest requestDesdeBorrador(Comprobante c) {
    Map<String, Object> cd = c.getCustomData() == null ? Map.of() : c.getCustomData();
    UUID puntoId = UUID.fromString(String.valueOf(cd.get("puntoEmisionId")));
    List<FacturaItemRequest> items =
        emisionSupport.listarDetalles(c.getId()).stream().map(emisionSupport::itemDesdeDetalle).toList();
    return new GuiaRemisionRequest(
        puntoId,
        c.getFechaEmision(),
        str(cd, "dirPartida", "Direccion partida"),
        str(cd, "tipoIdentificacionTransportista", "04"),
        str(cd, "identificacionTransportista", "9999999999999"),
        str(cd, "razonSocialTransportista", "Transportista"),
        null,
        null,
        str(cd, "placa", "AAA0000"),
        str(cd, "tipoIdentificacionDestinatario", "04"),
        c.getIdentificacionReceptor(),
        c.getRazonSocialReceptor(),
        str(cd, "dirDestinatario", "Destino"),
        str(cd, "motivoTraslado", "Traslado"),
        DocumentoEmisionSupport.parseUuid(cd.get("facturaSustentoId")),
        items,
        cd);
  }

  private Map<String, Object> customDataEnriquecida(UUID empresaId, GuiaRemisionRequest request) {
    Map<String, Object> cd = new HashMap<>(request.safeCustomData());
    cd.put("puntoEmisionId", request.puntoEmisionId().toString());
    cd.put("dirPartida", request.dirPartida());
    cd.put("placa", request.placa());
    cd.put("motivoTraslado", request.motivoTraslado());
    cd.put("dirDestinatario", request.dirDestinatario());
    cd.put("tipoIdentificacionTransportista", request.tipoIdentificacionTransportista());
    cd.put("identificacionTransportista", request.identificacionTransportista());
    cd.put("razonSocialTransportista", request.razonSocialTransportista());
    cd.put("tipoIdentificacionDestinatario", request.tipoIdentificacionDestinatario());
    if (request.fechaIniTransporte() != null) {
      cd.put("fechaIniTransporte", request.fechaIniTransporte().toString());
    }
    if (request.fechaFinTransporte() != null) {
      cd.put("fechaFinTransporte", request.fechaFinTransporte().toString());
    }
    if (request.facturaSustentoId() != null) {
      cd.put("facturaSustentoId", request.facturaSustentoId().toString());
      enriquecerComprobanteVentaEnCustomData(empresaId, cd, request.facturaSustentoId());
    }
    String ruta = str(cd, "ruta", "");
    if (!ruta.isBlank()) {
      cd.put("ruta", ruta);
    }
    return cd;
  }

  private void enriquecerComprobanteVentaEnCustomData(UUID empresaId, Map<String, Object> cd, UUID facturaId) {
    if (facturaId == null) {
      return;
    }
    try {
      Comprobante factura = emisionSupport.buscarComprobante(empresaId, facturaId);
      String num =
          factura.getEstablecimientoCodigo()
              + "-"
              + factura.getPuntoEmisionCodigo()
              + "-"
              + factura.getSecuencial();
      cd.put("numeroComprobanteVenta", num);
      if (factura.getFechaEmision() != null) {
        cd.put("fechaEmisionComprobanteVenta", factura.getFechaEmision().toString());
      }
      if (factura.getClaveAcceso() != null && !factura.getClaveAcceso().isBlank()) {
        cd.put("claveAccesoComprobanteVenta", factura.getClaveAcceso());
      }
    } catch (ResponseStatusException ignored) {
      // Vista previa o borrador sin factura resuelta aún
    }
  }

  private static String str(Map<String, Object> cd, String key, String def) {
    Object v = cd.get(key);
    return v == null || String.valueOf(v).isBlank() ? def : String.valueOf(v).trim();
  }
}
