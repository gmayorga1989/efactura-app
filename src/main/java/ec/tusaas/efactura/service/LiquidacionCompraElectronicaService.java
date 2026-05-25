package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.documento.DocumentoElectronicoRequest;
import ec.tusaas.efactura.dto.emision.ComprobanteResponse;
import ec.tusaas.efactura.dto.emision.FacturaItemRequest;
import ec.tusaas.efactura.dto.emision.FacturaRequest;
import ec.tusaas.efactura.emision.XmlLiquidacionCompraGeneratorService;
import ec.tusaas.efactura.entity.Certificado;
import ec.tusaas.efactura.entity.Comprobante;
import ec.tusaas.efactura.entity.ComprobanteDetalle;
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
public class LiquidacionCompraElectronicaService {

  private static final String TIPO = "LIQUIDACION_COMPRA";
  private static final String ESTADO_BORRADOR = "BORRADOR";

  private final CertificadoRepository certificadoRepository;
  private final SecuencialService secuencialService;
  private final DocumentoEmisionSupport emisionSupport;
  private final ComprobanteSriProcesoService sriProcesoService;
  private final XmlLiquidacionCompraGeneratorService xmlLiquidacionCompraGeneratorService;
  private final XmlXsdValidatorService xmlXsdValidatorService;
  private final AuditoriaService auditoriaService;

  @Transactional
  public ComprobanteResponse guardarBorradorDesdeDocumento(
      UUID empresaId, DocumentoElectronicoRequest body, UsuarioPrincipal principal) {
    return guardarBorrador(empresaId, requestDesdeDocumento(body), principal);
  }

  @Transactional
  public ComprobanteResponse guardarBorrador(UUID empresaId, FacturaRequest request, UsuarioPrincipal principal) {
    return persistirBorrador(empresaId, null, request, principal);
  }

  @Transactional
  public ComprobanteResponse actualizarBorrador(
      UUID empresaId, UUID id, FacturaRequest request, UsuarioPrincipal principal) {
    Comprobante existente = emisionSupport.buscarComprobante(empresaId, id);
    if (!TIPO.equalsIgnoreCase(existente.getTipo())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El comprobante no es liquidacion de compra");
    }
    if (!ESTADO_BORRADOR.equalsIgnoreCase(existente.getEstadoSri())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se pueden editar liquidaciones en borrador");
    }
    return persistirBorrador(empresaId, existente, request, principal);
  }

  @Transactional
  public ComprobanteResponse emitirBorrador(
      UUID empresaId, UUID id, String idempotencyKey, UsuarioPrincipal principal) {
    Comprobante c = emisionSupport.buscarComprobante(empresaId, id);
    if (!TIPO.equalsIgnoreCase(c.getTipo())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El comprobante no es liquidacion de compra");
    }
    if (!ESTADO_BORRADOR.equalsIgnoreCase(c.getEstadoSri())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El comprobante no esta en borrador");
    }
    FacturaRequest request = requestDesdeBorrador(c);
    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
      c.setIdempotencyKey(idempotencyKey.trim());
    }
    return completarEmision(empresaId, c, request, principal);
  }

  @Transactional
  public ComprobanteResponse reemitirAlSri(UUID empresaId, UUID id) {
    Comprobante c = emisionSupport.buscarComprobante(empresaId, id);
    if (!TIPO.equalsIgnoreCase(c.getTipo())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El comprobante no es liquidacion de compra");
    }
    ComprobanteSriReemisionSupport.validarPuedeReemitir(c.getEstadoSri());
    FacturaRequest request = requestDesdeBorrador(c);
    Empresa empresa = c.getEmpresa();
    Certificado certificado =
        certificadoRepository
            .findFirstByEmpresa_IdAndActivoParaFirmaIsTrue(empresaId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.PRECONDITION_REQUIRED, "No existe certificado activo para firma"));
    String xml = xmlLiquidacionCompraGeneratorService.generarXmlInicial(c, request);
    sriProcesoService.ejecutar(c, empresa, certificado, xml, xmlXsdValidatorService::validarLiquidacionCompra);
    sriProcesoService.asegurarRide(c);
    return emisionSupport.toResponse(c);
  }

  private ComprobanteResponse persistirBorrador(
      UUID empresaId, Comprobante existente, FacturaRequest request, UsuarioPrincipal principal) {
    PuntoEmision punto = emisionSupport.resolverPunto(empresaId, request.puntoEmisionId());
    Empresa empresa = punto.getEmpresa();
    List<FacturaItemRequest> items = resolverItems(request);
    DocumentoEmisionSupport.Totales totales = DocumentoEmisionSupport.calcularTotales(items);
    Comprobante c = existente != null ? existente : new Comprobante();
    if (existente == null) {
      UUID provisional = UUID.randomUUID();
      c.setSecuencial(DocumentoEmisionSupport.secuencialProvisional(provisional));
      c.setClaveAcceso(DocumentoEmisionSupport.claveProvisional(provisional));
      c.setTipo(TIPO);
      c.setTipoCodigo(TiposComprobanteSri.LIQUIDACION_COMPRA);
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
    c.setCustomData(customDataEnriquecida(request));
    DocumentoEmisionSupport.marcarModificacion(c, principal.getEmail());
    c = emisionSupport.guardarComprobante(c);
    emisionSupport.eliminarDetalles(c.getId());
    emisionSupport.guardarDetalles(c, empresa, items, principal.getEmail());
    return emisionSupport.toResponse(c);
  }

  private ComprobanteResponse completarEmision(
      UUID empresaId, Comprobante c, FacturaRequest request, UsuarioPrincipal principal) {
    PuntoEmision punto = emisionSupport.resolverPunto(empresaId, request.puntoEmisionId());
    Empresa empresa = punto.getEmpresa();
    Certificado certificado =
        certificadoRepository
            .findFirstByEmpresa_IdAndActivoParaFirmaIsTrue(empresaId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.PRECONDITION_REQUIRED, "No existe certificado activo para firma"));
    List<FacturaItemRequest> items = resolverItems(request);
    DocumentoEmisionSupport.Totales totales = DocumentoEmisionSupport.calcularTotales(items);
    long sec =
        secuencialService.reservarSiguiente(empresaId, punto.getId(), TiposComprobanteSri.LIQUIDACION_COMPRA, principal);
    String secuencial9 = String.format("%09d", sec);
    String claveAcceso =
        ClaveAccesoGenerator.generar(
            request.fechaEmisionOrToday(),
            TiposComprobanteSri.LIQUIDACION_COMPRA,
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
    c.setCustomData(customDataEnriquecida(request));
    DocumentoEmisionSupport.marcarModificacion(c, principal.getEmail());
    c = emisionSupport.guardarComprobante(c);
    emisionSupport.eliminarDetalles(c.getId());
    emisionSupport.guardarDetalles(c, empresa, items, principal.getEmail());
    String xml = xmlLiquidacionCompraGeneratorService.generarXmlInicial(c, request);
    sriProcesoService.ejecutar(c, empresa, certificado, xml, xmlXsdValidatorService::validarLiquidacionCompra);
    sriProcesoService.asegurarRide(c);
    registrarAuditoria(empresaId, c, principal);
    return emisionSupport.toResponse(c);
  }

  private static FacturaRequest requestDesdeDocumento(DocumentoElectronicoRequest body) {
    Map<String, Object> cd = new HashMap<>(body.safeCustomData());
    if (body.subtotalSinImpuestos() != null) {
      cd.put("subtotalSinImpuestos", body.subtotalSinImpuestos());
    }
    if (body.ivaTotal() != null) {
      cd.put("ivaTotal", body.ivaTotal());
    }
    List<FacturaItemRequest> items = List.of(lineaSintetica(body, cd));
    return new FacturaRequest(
        body.puntoEmisionId(),
        body.fechaEmision(),
        body.tipoIdentificacionReceptor(),
        body.identificacionReceptor(),
        body.razonSocialReceptor(),
        null,
        items,
        null,
        cd,
        null);
  }

  private static FacturaItemRequest lineaSintetica(DocumentoElectronicoRequest body, Map<String, Object> cd) {
    BigDecimal sub = body.subtotalSinImpuestos() == null ? BigDecimal.ZERO : body.subtotalSinImpuestos();
    BigDecimal iva = body.ivaTotal() == null ? BigDecimal.ZERO : body.ivaTotal();
    BigDecimal ivaPct = BigDecimal.ZERO;
    if (sub.signum() > 0 && iva.signum() > 0) {
      ivaPct = iva.multiply(BigDecimal.valueOf(100)).divide(sub, 2, RoundingMode.HALF_UP);
    }
    return new FacturaItemRequest(
        "LC01", null, "Liquidacion de compra", BigDecimal.ONE, sub, BigDecimal.ZERO, ivaPct,
        ivaPct.signum() == 0 ? "0" : "4", cd);
  }

  private List<FacturaItemRequest> resolverItems(FacturaRequest request) {
    if (request.items() != null && !request.items().isEmpty()) {
      return request.items();
    }
    return List.of(lineaSinteticaDesdeCustom(request));
  }

  private FacturaItemRequest lineaSinteticaDesdeCustom(FacturaRequest request) {
    Map<String, Object> cd = request.safeCustomData();
    BigDecimal sub =
        cd.get("subtotalSinImpuestos") instanceof Number n ? BigDecimal.valueOf(n.doubleValue()) : BigDecimal.ZERO;
    BigDecimal iva = cd.get("ivaTotal") instanceof Number n ? BigDecimal.valueOf(n.doubleValue()) : BigDecimal.ZERO;
    BigDecimal ivaPct = BigDecimal.ZERO;
    if (sub.signum() > 0 && iva.signum() > 0) {
      ivaPct = iva.multiply(BigDecimal.valueOf(100)).divide(sub, 2, RoundingMode.HALF_UP);
    }
    return new FacturaItemRequest(
        "LC01", null, "Liquidacion de compra", BigDecimal.ONE, sub, BigDecimal.ZERO, ivaPct,
        ivaPct.signum() == 0 ? "0" : "4", cd);
  }

  private FacturaRequest requestDesdeBorrador(Comprobante c) {
    Map<String, Object> cd = c.getCustomData() == null ? Map.of() : c.getCustomData();
    UUID puntoId = UUID.fromString(String.valueOf(cd.get("puntoEmisionId")));
    String tipoId = String.valueOf(cd.getOrDefault("tipoIdentificacionReceptor", "04"));
    List<FacturaItemRequest> items =
        emisionSupport.listarDetalles(c.getId()).stream().map(emisionSupport::itemDesdeDetalle).toList();
    return new FacturaRequest(
        puntoId,
        c.getFechaEmision(),
        tipoId,
        c.getIdentificacionReceptor(),
        c.getRazonSocialReceptor(),
        null,
        items,
        null,
        cd,
        null);
  }

  private static Map<String, Object> customDataEnriquecida(FacturaRequest request) {
    Map<String, Object> cd = new HashMap<>(request.safeCustomData());
    cd.put("puntoEmisionId", request.puntoEmisionId().toString());
    cd.put("tipoIdentificacionReceptor", request.tipoIdentificacionReceptor());
    return cd;
  }

  private void registrarAuditoria(UUID empresaId, Comprobante c, UsuarioPrincipal principal) {
    Map<String, Object> detalle = new HashMap<>();
    detalle.put("claveAcceso", c.getClaveAcceso());
    detalle.put("estadoSri", c.getEstadoSri());
    auditoriaService.registrar("LIQUIDACION_COMPRA_EMITIDA", empresaId, principal.getEmail(), "Comprobante", c.getId(), detalle);
  }
}
