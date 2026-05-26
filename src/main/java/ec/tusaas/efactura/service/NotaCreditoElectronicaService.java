package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.documento.DocumentoElectronicoRequest;
import ec.tusaas.efactura.dto.emision.ComprobanteDetalleResponse;
import ec.tusaas.efactura.dto.emision.ComprobanteResponse;
import ec.tusaas.efactura.dto.emision.FacturaItemRequest;
import ec.tusaas.efactura.dto.emision.NotaCreditoRequest;
import ec.tusaas.efactura.emision.EstadoSri;
import ec.tusaas.efactura.emision.ComprobanteEmailUtil;
import ec.tusaas.efactura.emision.DocumentoModificadoRideUtil;
import ec.tusaas.efactura.emision.XmlNotaCreditoGeneratorService;
import ec.tusaas.efactura.emision.XmlNotaCreditoGeneratorService.DocumentoModificado;
import ec.tusaas.efactura.entity.Certificado;
import ec.tusaas.efactura.util.ComprobanteVendedorMapper;
import ec.tusaas.efactura.entity.Comprobante;
import ec.tusaas.efactura.entity.ComprobanteArchivo;
import ec.tusaas.efactura.entity.ComprobanteDetalle;
import ec.tusaas.efactura.entity.ComprobanteLogSri;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.entity.PuntoEmision;
import ec.tusaas.efactura.repository.CertificadoRepository;
import ec.tusaas.efactura.repository.ComprobanteArchivoRepository;
import ec.tusaas.efactura.repository.ComprobanteDetalleRepository;
import ec.tusaas.efactura.repository.ComprobanteLogSriRepository;
import ec.tusaas.efactura.repository.ComprobanteRepository;
import ec.tusaas.efactura.repository.PuntoEmisionRepository;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.sri.ClaveAccesoGenerator;
import ec.tusaas.efactura.sri.ComprobanteSriReemisionSupport;
import ec.tusaas.efactura.sri.client.SriAutorizacionClient;
import ec.tusaas.efactura.sri.client.SriRecepcionClient;
import ec.tusaas.efactura.sri.signature.SignedXml;
import ec.tusaas.efactura.sri.signature.XmlSignerService;
import ec.tusaas.efactura.sri.xml.XmlXsdValidatorService;
import ec.tusaas.efactura.storage.LocalComprobanteStorage;
import ec.tusaas.efactura.emision.Sha256;
import ec.tusaas.efactura.tributario.TiposComprobanteSri;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
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
public class NotaCreditoElectronicaService {

  private static final String ESTADO_BORRADOR = "BORRADOR";
  private static final String TIPO_FACTURA = "FACTURA";
  private static final String ESTADO_AUTORIZADO = "AUTORIZADO";

  private final PuntoEmisionRepository puntoEmisionRepository;
  private final CertificadoRepository certificadoRepository;
  private final ComprobanteRepository comprobanteRepository;
  private final ComprobanteDetalleRepository comprobanteDetalleRepository;
  private final ComprobanteArchivoRepository comprobanteArchivoRepository;
  private final ComprobanteLogSriRepository comprobanteLogSriRepository;
  private final SecuencialService secuencialService;
  private final XmlNotaCreditoGeneratorService xmlNotaCreditoGeneratorService;
  private final XmlSignerService xmlSignerService;
  private final SriRecepcionClient sriRecepcionClient;
  private final SriAutorizacionClient sriAutorizacionClient;
  private final LocalComprobanteStorage localComprobanteStorage;
  private final XmlXsdValidatorService xmlXsdValidatorService;
  private final ComprobanteRideService comprobanteRideService;
  private final ComprobanteSriProcesoService comprobanteSriProcesoService;
  private final AuditoriaService auditoriaService;

  @Transactional
  public ComprobanteResponse guardarBorradorDesdeDocumento(
      UUID empresaId, DocumentoElectronicoRequest request, UsuarioPrincipal principal) {
    Map<String, Object> cd = new HashMap<>(request.safeCustomData());
    if (request.subtotalSinImpuestos() != null) {
      cd.put("subtotalSinImpuestos", request.subtotalSinImpuestos());
    }
    if (request.ivaTotal() != null) {
      cd.put("ivaTotal", request.ivaTotal());
    }
    UUID facturaId = parseUuid(cd.get("facturaOrigenId"));
    if (facturaId == null) {
      facturaId = parseUuid(cd.get("facturaModificadaId"));
    }
    String motivo = cd.get("motivo") != null ? String.valueOf(cd.get("motivo")).trim() : "";
    NotaCreditoRequest nc =
        new NotaCreditoRequest(
            request.puntoEmisionId(),
            request.fechaEmision(),
            request.tipoIdentificacionReceptor(),
            request.identificacionReceptor(),
            request.razonSocialReceptor(),
            facturaId,
            motivo,
            null,
            cd);
    return guardarBorrador(empresaId, nc, principal);
  }

  @Transactional
  public ComprobanteResponse guardarBorrador(
      UUID empresaId, NotaCreditoRequest request, UsuarioPrincipal principal) {
    return persistirBorrador(empresaId, null, request, principal);
  }

  @Transactional
  public ComprobanteResponse actualizarBorrador(
      UUID empresaId, UUID id, NotaCreditoRequest request, UsuarioPrincipal principal) {
    Comprobante existente =
        comprobanteRepository
            .findByIdAndEmpresa_Id(id, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comprobante no encontrado"));
    if (!"NOTA_CREDITO".equalsIgnoreCase(existente.getTipo())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El comprobante no es una nota de credito");
    }
    if (!ESTADO_BORRADOR.equalsIgnoreCase(existente.getEstadoSri())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se pueden editar notas de credito en borrador");
    }
    return persistirBorrador(empresaId, existente, request, principal);
  }

  @Transactional
  public ComprobanteResponse emitirBorrador(
      UUID empresaId, UUID id, String idempotencyKey, UsuarioPrincipal principal) {
    Comprobante c =
        comprobanteRepository
            .findByIdAndEmpresa_Id(id, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comprobante no encontrado"));
    if (!"NOTA_CREDITO".equalsIgnoreCase(c.getTipo())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El comprobante no es una nota de credito");
    }
    if (!ESTADO_BORRADOR.equalsIgnoreCase(c.getEstadoSri())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El comprobante no esta en borrador");
    }
    NotaCreditoRequest request = requestDesdeBorrador(c);
    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
      c.setIdempotencyKey(idempotencyKey.trim());
    }
    return completarEmisionDesdeBorrador(empresaId, c, request, principal);
  }

  @Transactional
  public ComprobanteResponse reemitirAlSri(UUID empresaId, UUID id) {
    Comprobante c =
        comprobanteRepository
            .findByIdAndEmpresa_Id(id, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comprobante no encontrado"));
    if (!"NOTA_CREDITO".equalsIgnoreCase(c.getTipo())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El comprobante no es una nota de credito");
    }
    ComprobanteSriReemisionSupport.validarPuedeReemitir(c.getEstadoSri());
    Certificado certificado =
        certificadoRepository
            .findFirstByEmpresa_IdAndActivoParaFirmaIsTrue(empresaId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.PRECONDITION_REQUIRED, "No existe certificado activo para firma"));
    NotaCreditoRequest request = requestDesdeBorrador(c);
    Comprobante factura = cargarFacturaModificada(empresaId, request);
    DocumentoModificado docModificado = documentoModificadoDesde(factura);
    List<FacturaItemRequest> items = resolverItems(empresaId, request);
    ejecutarProcesoSri(c, c.getEmpresa(), request, docModificado, items, certificado);
    asegurarRide(c);
    return toResponse(c);
  }

  @Transactional(readOnly = true)
  public ComprobanteResponse obtener(UUID empresaId, UUID id) {
    Comprobante c =
        comprobanteRepository
            .findByIdAndEmpresa_Id(id, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comprobante no encontrado"));
    if (!"NOTA_CREDITO".equalsIgnoreCase(c.getTipo())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comprobante no encontrado");
    }
    return toResponse(c);
  }

  private ComprobanteResponse persistirBorrador(
      UUID empresaId, Comprobante existente, NotaCreditoRequest request, UsuarioPrincipal principal) {
    PuntoEmision punto = resolverPunto(empresaId, request.puntoEmisionId());
    Empresa empresa = punto.getEmpresa();
    List<FacturaItemRequest> items = resolverItems(empresaId, request);
    Totales totales = calcularTotales(items);
    Comprobante c = existente != null ? existente : new Comprobante();
    if (existente == null) {
      UUID provisional = UUID.randomUUID();
      c.setSecuencial(secuencialProvisional(provisional));
      c.setClaveAcceso(claveProvisional(provisional));
      c.setTipo("NOTA_CREDITO");
      c.setTipoCodigo(TiposComprobanteSri.NOTA_CREDITO);
      c.setOrigen("WEB");
      c.setUsuarioCreacion(principal.getEmail());
    }
    c.setEmpresa(empresa);
    c.setEstablecimientoCodigo(punto.getEstablecimiento().getCodigo());
    c.setPuntoEmisionCodigo(punto.getCodigo());
    c.setFechaEmision(request.fechaEmisionOrToday());
    c.setRazonSocialReceptor(request.razonSocialReceptor());
    c.setIdentificacionReceptor(request.identificacionReceptor());
    c.setSubtotalSinImpuestos(totales.subtotal());
    c.setSubtotal12(totales.subtotalGravado());
    c.setSubtotal0(totales.subtotal0());
    c.setDescuentoTotal(totales.descuento());
    c.setIvaTotal(totales.iva());
    c.setValorTotal(totales.total());
    c.setAmbienteSri(empresa.getAmbienteSri());
    c.setTipoEmision(empresa.getTipoEmision());
    c.setEstadoSri(ESTADO_BORRADOR);
    Comprobante facturaRef = cargarFacturaModificadaSiExiste(empresaId, request);
    c.setCustomData(customDataEnriquecida(request, facturaRef));
    c.setUsuarioModificacion(principal.getEmail());
    c.setFechaModificacion(Instant.now());
    c = comprobanteRepository.save(c);
    comprobanteDetalleRepository.deleteByComprobante_Id(c.getId());
    guardarDetalles(c, empresa, items, principal.getEmail());
    return toResponse(c);
  }

  private ComprobanteResponse completarEmisionDesdeBorrador(
      UUID empresaId, Comprobante c, NotaCreditoRequest request, UsuarioPrincipal principal) {
    PuntoEmision punto = resolverPunto(empresaId, request.puntoEmisionId());
    Empresa empresa = punto.getEmpresa();
    Certificado certificado =
        certificadoRepository
            .findFirstByEmpresa_IdAndActivoParaFirmaIsTrue(empresaId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.PRECONDITION_REQUIRED, "No existe certificado activo para firma"));
    Comprobante factura = cargarFacturaModificada(empresaId, request);
    DocumentoModificado docModificado = documentoModificadoDesde(factura);
    List<FacturaItemRequest> items = resolverItems(empresaId, request);
    Totales totales = calcularTotales(items);

    long sec =
        secuencialService.reservarSiguiente(empresaId, punto.getId(), TiposComprobanteSri.NOTA_CREDITO, principal);
    String secuencial9 = String.format("%09d", sec);
    String claveAcceso =
        ClaveAccesoGenerator.generar(
            request.fechaEmisionOrToday(),
            TiposComprobanteSri.NOTA_CREDITO,
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
    c.setSubtotalSinImpuestos(totales.subtotal());
    c.setSubtotal12(totales.subtotalGravado());
    c.setSubtotal0(totales.subtotal0());
    c.setDescuentoTotal(totales.descuento());
    c.setIvaTotal(totales.iva());
    c.setValorTotal(totales.total());
    c.setCustomData(customDataEnriquecida(request, factura));
    c.setUsuarioModificacion(principal.getEmail());
    c.setFechaModificacion(Instant.now());
    c = comprobanteRepository.save(c);
    comprobanteDetalleRepository.deleteByComprobante_Id(c.getId());
    guardarDetalles(c, empresa, items, principal.getEmail());
    ejecutarProcesoSri(c, empresa, request, docModificado, items, certificado);
    asegurarRide(c);
    registrarAuditoria(empresaId, c, principal);
    return toResponse(c);
  }

  private void ejecutarProcesoSri(
      Comprobante c,
      Empresa empresa,
      NotaCreditoRequest request,
      DocumentoModificado docModificado,
      List<FacturaItemRequest> items,
      Certificado certificado) {
    c.setEstadoSri(EstadoSri.FIRMADO);
    comprobanteRepository.save(c);
    String xmlGenerado = xmlNotaCreditoGeneratorService.generarXmlInicial(c, request, docModificado, items);
    try {
      xmlXsdValidatorService.validarNotaCredito(xmlGenerado);
    } catch (IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), e);
    }
    SignedXml xmlFirmado = xmlSignerService.firmar(xmlGenerado, certificado);
    registrarArchivo(c, "XML_GENERADO", xmlGenerado);
    registrarArchivo(c, xmlFirmado.stub() ? "XML_FIRMADO_STUB" : "XML_FIRMADO", xmlFirmado.xml());
    var recepcion = sriRecepcionClient.enviar(xmlFirmado.xml(), c.getAmbienteSri());
    c.setEstadoSri(recepcion.estado());
    comprobanteRepository.save(c);
    registrarLogSri(
        c,
        empresa,
        "RECEPCION",
        "claveAcceso=" + c.getClaveAcceso(),
        recepcion.rawResponse(),
        recepcion.httpStatus(),
        recepcion.mensaje());

    if ("RECIBIDA".equalsIgnoreCase(recepcion.estado())) {
      var autorizacion = sriAutorizacionClient.consultar(c.getClaveAcceso(), c.getAmbienteSri());
      c.setEstadoSri(normalizarEstadoAutorizacion(autorizacion.estado()));
      c.setNumeroAutorizacion(autorizacion.numeroAutorizacion());
      c.setFechaAutorizacion(autorizacion.fechaAutorizacion());
      comprobanteRepository.save(c);
      if ("AUTORIZADO".equalsIgnoreCase(c.getEstadoSri()) && autorizacion.rawResponse() != null) {
        registrarArchivo(c, "XML_AUTORIZADO", autorizacion.rawResponse());
      }
      registrarLogSri(
          c,
          empresa,
          "AUTORIZACION",
          "claveAcceso=" + c.getClaveAcceso(),
          autorizacion.rawResponse(),
          200,
          autorizacion.mensaje());
      comprobanteSriProcesoService.notificarClienteSiAutorizado(c);
    }
  }

  private static String normalizarEstadoRecepcion(String estado) {
    if (estado == null || estado.isBlank()) {
      return EstadoSri.ERROR;
    }
    if ("DEVUELTA".equalsIgnoreCase(estado.trim())) {
      return EstadoSri.DEVUELTO;
    }
    return estado.trim();
  }

  private Comprobante cargarFacturaModificada(UUID empresaId, NotaCreditoRequest request) {
    UUID facturaId = request.facturaModificadaId();
    if (facturaId == null) {
      Map<String, Object> cd = request.safeCustomData();
      facturaId = parseUuid(cd.get("facturaOrigenId"));
      if (facturaId == null) {
        facturaId = parseUuid(cd.get("facturaModificadaId"));
      }
    }
    if (facturaId == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "La nota de credito requiere la factura modificada (facturaModificadaId)");
    }
    Comprobante factura =
        comprobanteRepository
            .findByIdAndEmpresa_Id(facturaId, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura modificada no encontrada"));
    if (!TIPO_FACTURA.equalsIgnoreCase(factura.getTipo())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El documento sustento debe ser una factura");
    }
    if (!ESTADO_AUTORIZADO.equalsIgnoreCase(factura.getEstadoSri())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Solo se puede acreditar contra facturas autorizadas por el SRI");
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

  private List<FacturaItemRequest> resolverItems(UUID empresaId, NotaCreditoRequest request) {
    if (request.items() != null && !request.items().isEmpty()) {
      return request.items();
    }
    UUID facturaId = request.facturaModificadaId();
    if (facturaId == null) {
      Map<String, Object> cd = request.safeCustomData();
      facturaId = parseUuid(cd.get("facturaOrigenId"));
      if (facturaId == null) {
        facturaId = parseUuid(cd.get("facturaModificadaId"));
      }
    }
    if (facturaId != null) {
      List<ComprobanteDetalle> detalles =
          comprobanteDetalleRepository.findByComprobante_IdOrderByLineaAsc(facturaId);
      if (!detalles.isEmpty()) {
        return detalles.stream().map(this::itemDesdeDetalle).toList();
      }
    }
    return List.of(lineaSinteticaDesdeTotales(request));
  }

  private FacturaItemRequest lineaSinteticaDesdeTotales(NotaCreditoRequest request) {
    Map<String, Object> cd = request.safeCustomData();
    BigDecimal subtotal =
        cd.get("subtotalSinImpuestos") instanceof Number n
            ? BigDecimal.valueOf(n.doubleValue())
            : BigDecimal.ZERO;
    BigDecimal iva = cd.get("ivaTotal") instanceof Number n ? BigDecimal.valueOf(n.doubleValue()) : BigDecimal.ZERO;
    if (subtotal.signum() == 0 && request.items() == null) {
      subtotal = BigDecimal.ZERO;
    }
    BigDecimal ivaPct = BigDecimal.ZERO;
    if (subtotal.signum() > 0 && iva.signum() > 0) {
      ivaPct =
          iva.multiply(BigDecimal.valueOf(100)).divide(subtotal, 2, RoundingMode.HALF_UP);
    }
    return new FacturaItemRequest(
        "NC01",
        null,
        "Ajuste nota de credito",
        BigDecimal.ONE,
        subtotal.max(BigDecimal.ZERO),
        BigDecimal.ZERO,
        ivaPct,
        ivaPct.compareTo(BigDecimal.ZERO) == 0 ? "0" : "4",
        Map.of());
  }

  private NotaCreditoRequest requestDesdeBorrador(Comprobante c) {
    Map<String, Object> cd = c.getCustomData() == null ? Map.of() : c.getCustomData();
    Object puntoRaw = cd.get("puntoEmisionId");
    if (puntoRaw == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Borrador sin punto de emision");
    }
    UUID puntoId = UUID.fromString(String.valueOf(puntoRaw));
    String tipoId = String.valueOf(cd.getOrDefault("tipoIdentificacionReceptor", "04"));
    UUID facturaId = parseUuid(cd.get("facturaOrigenId"));
    if (facturaId == null) {
      facturaId = parseUuid(cd.get("facturaModificadaId"));
    }
    String motivo = cd.get("motivo") != null ? String.valueOf(cd.get("motivo")) : "";
    List<ComprobanteDetalle> detalles =
        comprobanteDetalleRepository.findByComprobante_IdOrderByLineaAsc(c.getId());
    List<FacturaItemRequest> items = detalles.stream().map(this::itemDesdeDetalle).toList();
    return new NotaCreditoRequest(
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

  private FacturaItemRequest itemDesdeDetalle(ComprobanteDetalle d) {
    Map<String, Object> dcd = d.getCustomData() == null ? Map.of() : d.getCustomData();
    BigDecimal ivaPct = BigDecimal.ZERO;
    Object ivaRaw = dcd.get("ivaPorcentaje");
    if (ivaRaw instanceof Number n) {
      ivaPct = BigDecimal.valueOf(n.doubleValue());
    } else if (ivaRaw != null) {
      try {
        ivaPct = new BigDecimal(String.valueOf(ivaRaw));
      } catch (NumberFormatException ignored) {
        ivaPct = BigDecimal.ZERO;
      }
    }
    String ivaCodigo =
        dcd.get("ivaCodigoPorcentaje") != null ? String.valueOf(dcd.get("ivaCodigoPorcentaje")) : null;
    return new FacturaItemRequest(
        d.getCodigoPrincipal(),
        d.getCodigoAuxiliar(),
        d.getDescripcion(),
        d.getCantidad(),
        d.getPrecioUnitario(),
        d.getDescuento(),
        ivaPct,
        ivaCodigo,
        dcd);
  }

  private void guardarDetalles(
      Comprobante c, Empresa empresa, List<FacturaItemRequest> items, String usuario) {
    int linea = 1;
    for (FacturaItemRequest item : items) {
      ComprobanteDetalle d = new ComprobanteDetalle();
      d.setComprobante(c);
      d.setEmpresa(empresa);
      d.setLinea(linea++);
      d.setCodigoPrincipal(item.codigoPrincipal());
      d.setCodigoAuxiliar(item.codigoAuxiliar());
      d.setDescripcion(item.descripcion());
      d.setCantidad(item.cantidad());
      d.setPrecioUnitario(item.precioUnitario());
      d.setDescuento(nullToZero(item.descuento()).setScale(2, RoundingMode.HALF_UP));
      d.setPrecioTotalSinImpuesto(lineSubtotal(item).setScale(2, RoundingMode.HALF_UP));
      Map<String, Object> dcd = new HashMap<>(item.safeCustomData());
      if (item.ivaPorcentaje() != null) {
        dcd.put("ivaPorcentaje", item.ivaPorcentaje());
      }
      if (item.ivaCodigoPorcentaje() != null && !item.ivaCodigoPorcentaje().isBlank()) {
        dcd.put("ivaCodigoPorcentaje", item.ivaCodigoPorcentaje());
      }
      d.setCustomData(dcd);
      d.setUsuarioCreacion(usuario);
      comprobanteDetalleRepository.save(d);
    }
  }

  private ComprobanteArchivo asegurarRide(Comprobante c) {
    return comprobanteArchivoRepository
        .findFirstByComprobante_IdAndTipoOrderByFechaCreacionDesc(c.getId(), "RIDE_PDF")
        .orElseGet(() -> registrarRide(c));
  }

  private ComprobanteArchivo registrarRide(Comprobante c) {
    byte[] pdf = comprobanteRideService.generarPdf(c);
    ComprobanteArchivo a = new ComprobanteArchivo();
    a.setComprobante(c);
    a.setTipo("RIDE_PDF");
    a.setSha256(Sha256.hex(pdf));
    a.setTamanoBytes((long) pdf.length);
    try {
      a.setStorageKey(localComprobanteStorage.guardarBytes(c.getEmpresa().getId(), c.getId(), "RIDE_PDF", "pdf", pdf));
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo almacenar el RIDE");
    }
    return comprobanteArchivoRepository.save(a);
  }

  private void registrarArchivo(Comprobante c, String tipo, String contenido) {
    ComprobanteArchivo a = new ComprobanteArchivo();
    a.setComprobante(c);
    a.setTipo(tipo);
    a.setSha256(Sha256.hex(contenido));
    a.setTamanoBytes((long) contenido.getBytes(StandardCharsets.UTF_8).length);
    try {
      a.setStorageKey(localComprobanteStorage.guardarTexto(c.getEmpresa().getId(), c.getId(), tipo, contenido));
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo almacenar el XML");
    }
    comprobanteArchivoRepository.save(a);
  }

  private void registrarLogSri(
      Comprobante c,
      Empresa empresa,
      String operacion,
      String request,
      String response,
      int httpStatus,
      String mensaje) {
    ComprobanteLogSri log = new ComprobanteLogSri();
    log.setComprobante(c);
    log.setEmpresa(empresa);
    log.setOperacion(operacion);
    log.setRequest(request);
    log.setResponse(response);
    log.setHttpStatus(httpStatus);
    log.setDuracionMs(0);
    log.setErrorMensaje(mensaje);
    log.setFecha(Instant.now());
    comprobanteLogSriRepository.save(log);
  }

  private void registrarAuditoria(UUID empresaId, Comprobante c, UsuarioPrincipal principal) {
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
    auditoriaService.registrar(
        "NOTA_CREDITO_EMITIDA", empresaId, actor, "Comprobante", c.getId(), detalle);
  }

  private ComprobanteResponse toResponse(Comprobante c) {
    List<ComprobanteDetalleResponse> detalles =
        comprobanteDetalleRepository.findByComprobante_IdOrderByLineaAsc(c.getId()).stream()
            .map(
                d ->
                    new ComprobanteDetalleResponse(
                        d.getId(),
                        d.getLinea(),
                        d.getCodigoPrincipal(),
                        d.getCodigoAuxiliar(),
                        d.getDescripcion(),
                        d.getCantidad(),
                        d.getPrecioUnitario(),
                        d.getDescuento(),
                        d.getPrecioTotalSinImpuesto(),
                        d.getCustomData()))
            .toList();
    return new ComprobanteResponse(
        c.getId(),
        c.getEmpresa().getId(),
        c.getTipo(),
        c.getTipoCodigo(),
        c.getEstablecimientoCodigo() + "-" + c.getPuntoEmisionCodigo() + "-" + c.getSecuencial(),
        c.getClaveAcceso(),
        c.getFechaEmision(),
        c.getRazonSocialReceptor(),
        c.getIdentificacionReceptor(),
        c.getSubtotalSinImpuestos(),
        c.getDescuentoTotal(),
        c.getIvaTotal(),
        c.getValorTotal(),
        c.getEstadoSri(),
        c.getNumeroAutorizacion(),
        c.getFechaAutorizacion(),
        null,
        ComprobanteVendedorMapper.vendedorId(c),
        ComprobanteVendedorMapper.vendedorNombre(c),
        c.getCustomData(),
        detalles);
  }

  private Comprobante cargarFacturaModificadaSiExiste(UUID empresaId, NotaCreditoRequest request) {
    try {
      return cargarFacturaModificada(empresaId, request);
    } catch (ResponseStatusException e) {
      if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
        return null;
      }
      throw e;
    }
  }

  private static Map<String, Object> customDataEnriquecida(NotaCreditoRequest request, Comprobante facturaModificada) {
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
      ComprobanteEmailUtil.copiarDatosReceptorSiAusente(cd, facturaModificada.getCustomData());
    }
    return cd;
  }

  private PuntoEmision resolverPunto(UUID empresaId, UUID puntoEmisionId) {
    PuntoEmision punto =
        puntoEmisionRepository
            .findByIdAndEmpresa_Id(puntoEmisionId, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Punto de emision no encontrado"));
    validarPuntoActivo(punto);
    return punto;
  }

  private static void validarPuntoActivo(PuntoEmision punto) {
    if (!"ACTIVO".equals(punto.getEstado()) || !"ACTIVO".equals(punto.getEstablecimiento().getEstado())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "El establecimiento o punto de emision se encuentra inactivo");
    }
  }

  private static Totales calcularTotales(List<FacturaItemRequest> items) {
    if (items == null || items.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nota de credito requiere al menos una linea");
    }
    BigDecimal subtotal = BigDecimal.ZERO;
    BigDecimal subtotal0 = BigDecimal.ZERO;
    BigDecimal subtotalGravado = BigDecimal.ZERO;
    BigDecimal descuento = BigDecimal.ZERO;
    BigDecimal iva = BigDecimal.ZERO;
    for (FacturaItemRequest item : items) {
      BigDecimal lineGross = item.cantidad().multiply(item.precioUnitario());
      BigDecimal lineDiscount = nullToZero(item.descuento());
      BigDecimal lineSubtotal = lineGross.subtract(lineDiscount);
      if (lineSubtotal.signum() < 0) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Descuento mayor al subtotal de linea");
      }
      BigDecimal ivaPct = nullToZero(item.ivaPorcentaje());
      subtotal = subtotal.add(lineSubtotal);
      if (ivaPct.signum() == 0) {
        subtotal0 = subtotal0.add(lineSubtotal);
      } else {
        subtotalGravado = subtotalGravado.add(lineSubtotal);
      }
      descuento = descuento.add(lineDiscount);
      iva = iva.add(lineSubtotal.multiply(ivaPct).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
    }
    subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
    subtotal0 = subtotal0.setScale(2, RoundingMode.HALF_UP);
    subtotalGravado = subtotalGravado.setScale(2, RoundingMode.HALF_UP);
    descuento = descuento.setScale(2, RoundingMode.HALF_UP);
    iva = iva.setScale(2, RoundingMode.HALF_UP);
    return new Totales(
        subtotal,
        subtotal0,
        subtotalGravado,
        descuento,
        iva,
        subtotal.add(iva).setScale(2, RoundingMode.HALF_UP));
  }

  private static BigDecimal lineSubtotal(FacturaItemRequest item) {
    return item.cantidad().multiply(item.precioUnitario()).subtract(nullToZero(item.descuento()));
  }

  private static BigDecimal nullToZero(BigDecimal v) {
    return v == null ? BigDecimal.ZERO : v;
  }

  private static String normalizarEstadoAutorizacion(String estado) {
    if (estado == null || estado.isBlank()) {
      return EstadoSri.PENDIENTE_AUTORIZACION;
    }
    return estado;
  }

  private static UUID parseUuid(Object raw) {
    if (raw == null) {
      return null;
    }
    try {
      return UUID.fromString(String.valueOf(raw).trim());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private static String secuencialProvisional(UUID id) {
    String hex = id.toString().replace("-", "").toUpperCase();
    return ("B" + hex.substring(0, 8)).substring(0, 9);
  }

  private static String claveProvisional(UUID id) {
    String digits = id.toString().replaceAll("[^0-9]", "");
    if (digits.length() < 47) {
      digits = (digits + "12345678901234567890123456789012345678901234567").substring(0, 47);
    }
    return ("99" + digits).substring(0, 49);
  }

  private record Totales(
      BigDecimal subtotal,
      BigDecimal subtotal0,
      BigDecimal subtotalGravado,
      BigDecimal descuento,
      BigDecimal iva,
      BigDecimal total) {}
}
