package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.emision.ComprobanteDetalleResponse;
import ec.tusaas.efactura.dto.emision.ComprobanteRelacionadoResumen;
import ec.tusaas.efactura.dto.emision.ComprobanteResponse;
import ec.tusaas.efactura.dto.emision.FacturaItemRequest;
import ec.tusaas.efactura.dto.emision.FacturaRequest;
import ec.tusaas.efactura.dto.emision.PagoRequest;
import ec.tusaas.efactura.dto.emision.PuntoEmisionEmitirOption;
import ec.tusaas.efactura.emision.ComprobanteEmailUtil;
import ec.tusaas.efactura.emision.EstadoSri;
import ec.tusaas.efactura.emision.Sha256;
import ec.tusaas.efactura.emision.XmlFacturaGeneratorService;
import ec.tusaas.efactura.entity.Certificado;
import ec.tusaas.efactura.entity.Comprobante;
import ec.tusaas.efactura.entity.ComprobanteArchivo;
import ec.tusaas.efactura.entity.ComprobanteDetalle;
import ec.tusaas.efactura.entity.ComprobanteLogSri;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.entity.PuntoEmision;
import ec.tusaas.efactura.repository.ApiKeyRepository;
import ec.tusaas.efactura.repository.CertificadoRepository;
import ec.tusaas.efactura.repository.ComprobanteArchivoRepository;
import ec.tusaas.efactura.repository.ComprobanteDetalleRepository;
import ec.tusaas.efactura.repository.ComprobanteLogSriRepository;
import ec.tusaas.efactura.repository.ComprobanteRepository;
import ec.tusaas.efactura.entity.Vendedor;
import ec.tusaas.efactura.repository.PuntoEmisionRepository;
import ec.tusaas.efactura.repository.VendedorRepository;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.util.ComprobanteVendedorMapper;
import ec.tusaas.efactura.sri.ClaveAccesoGenerator;
import ec.tusaas.efactura.sri.ComprobanteSriReemisionSupport;
import ec.tusaas.efactura.sri.client.SriAutorizacionClient;
import ec.tusaas.efactura.sri.client.SriRecepcionClient;
import ec.tusaas.efactura.sri.signature.SignedXml;
import ec.tusaas.efactura.sri.signature.XmlSignerService;
import ec.tusaas.efactura.sri.xml.XmlXsdValidatorService;
import ec.tusaas.efactura.storage.LocalComprobanteStorage;
import ec.tusaas.efactura.tributario.TiposComprobanteSri;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class FacturaElectronicaService {

  private static final String ESTADO_BORRADOR = "BORRADOR";

  private final PuntoEmisionRepository puntoEmisionRepository;
  private final CertificadoRepository certificadoRepository;
  private final ComprobanteRepository comprobanteRepository;
  private final ComprobanteDetalleRepository comprobanteDetalleRepository;
  private final ComprobanteArchivoRepository comprobanteArchivoRepository;
  private final ComprobanteLogSriRepository comprobanteLogSriRepository;
  private final SecuencialService secuencialService;
  private final XmlFacturaGeneratorService xmlFacturaGeneratorService;
  private final XmlSignerService xmlSignerService;
  private final SriRecepcionClient sriRecepcionClient;
  private final SriAutorizacionClient sriAutorizacionClient;
  private final LocalComprobanteStorage localComprobanteStorage;
  private final XmlXsdValidatorService xmlXsdValidatorService;
  private final ComprobanteRideService comprobanteRideService;
  private final ComprobanteSriProcesoService comprobanteSriProcesoService;
  private final ComprobanteNotificacionService comprobanteNotificacionService;
  private final ApiKeyRepository apiKeyRepository;
  private final AuditoriaService auditoriaService;
  private final DashboardCacheService dashboardCacheService;
  private final VendedorRepository vendedorRepository;

  @Transactional
  public ComprobanteResponse emitir(
      UUID empresaId, FacturaRequest request, String idempotencyKey, UsuarioPrincipal principal) {
    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
      var previo =
          comprobanteRepository.findByEmpresa_IdAndIdempotencyKey(empresaId, idempotencyKey.trim());
      if (previo.isPresent()) {
        asegurarRide(previo.get());
        return toResponse(previo.get());
      }
    }

    PuntoEmision punto =
        puntoEmisionRepository
            .findByIdAndEmpresa_Id(request.puntoEmisionId(), empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Punto de emisión no encontrado"));
    validarPuntoActivo(punto);
    Empresa empresa = punto.getEmpresa();
    Certificado certificado =
        certificadoRepository
            .findFirstByEmpresa_IdAndActivoParaFirmaIsTrue(empresaId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.PRECONDITION_REQUIRED, "No existe certificado activo para firma"));

    Totales totales = calcularTotales(request.items());
    validarPagos(request, totales.total());
    long sec = secuencialService.reservarSiguiente(empresaId, punto.getId(), TiposComprobanteSri.FACTURA, principal);
    String secuencial9 = String.format("%09d", sec);
    String claveAcceso =
        ClaveAccesoGenerator.generar(
            request.fechaEmisionOrToday(),
            TiposComprobanteSri.FACTURA,
            empresa.getRuc(),
            empresa.getAmbienteSri(),
            empresa.getTipoEmision(),
            punto.getEstablecimiento().getCodigo(),
            punto.getCodigo(),
            sec,
            ClaveAccesoGenerator.ochoDigitosAleatorios());

    Comprobante c = new Comprobante();
    c.setEmpresa(empresa);
    c.setTipo("FACTURA");
    c.setTipoCodigo(TiposComprobanteSri.FACTURA);
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
    c.setAmbienteSri(empresa.getAmbienteSri());
    c.setTipoEmision(empresa.getTipoEmision());
    c.setEstadoSri(EstadoSri.FIRMADO);
    c.setIdempotencyKey(idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey.trim());
    if (principal.getApiKeyId() != null) {
      c.setOrigen("API");
      c.setApiKey(apiKeyRepository.getReferenceById(principal.getApiKeyId()));
    } else {
      c.setOrigen("WEB");
    }
    c.setCustomData(customDataEnriquecida(request));
    aplicarVendedor(c, empresaId, request);
    c.setUsuarioCreacion(principal.getEmail());
    c = comprobanteRepository.save(c);
    guardarDetalles(c, empresa, request.items(), principal.getEmail());

    ejecutarProcesoSri(c, empresa, request, certificado, principal);
    asegurarRide(c);
    registrarAuditoriaEmision(empresaId, c, principal);
    dashboardCacheService.evictEmpresa(empresaId);
    return toResponse(c);
  }

  @Transactional(readOnly = true)
  public ComprobanteResponse obtener(UUID empresaId, UUID id) {
    Comprobante c =
        comprobanteRepository
            .findByIdAndEmpresa_Id(id, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comprobante no encontrado"));
    return toResponse(c);
  }

  @Transactional(readOnly = true)
  public List<ComprobanteRelacionadoResumen> listarRelacionadosConFactura(UUID empresaId, UUID facturaId) {
    comprobanteRepository
        .findByIdAndEmpresa_Id(facturaId, empresaId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comprobante no encontrado"));
    return comprobanteRepository.findRelacionadosConFactura(empresaId, facturaId.toString()).stream()
        .map(this::toRelacionadoResumen)
        .toList();
  }

  private ComprobanteRelacionadoResumen toRelacionadoResumen(Comprobante c) {
    return new ComprobanteRelacionadoResumen(
        c.getId(),
        c.getTipo(),
        c.getEstablecimientoCodigo() + "-" + c.getPuntoEmisionCodigo() + "-" + c.getSecuencial(),
        c.getEstadoSri(),
        c.getFechaAutorizacion());
  }

  /** Listado paginado sin cargar líneas de detalle (más liviano para tablas). */
  @Transactional(readOnly = true)
  public Page<ComprobanteResponse> listar(
      UUID empresaId,
      String tipo,
      String estadoSri,
      LocalDate fechaDesde,
      LocalDate fechaHasta,
      Pageable pageable) {
  Pageable sorted =
      pageable.getSort().isSorted()
          ? pageable
          : org.springframework.data.domain.PageRequest.of(
              pageable.getPageNumber(),
              pageable.getPageSize(),
              org.springframework.data.domain.Sort.by(
                  org.springframework.data.domain.Sort.Direction.DESC,
                  "fechaEmision",
                  "fechaCreacion"));
    return comprobanteRepository
        .findAll(filtrosListado(empresaId, tipo, estadoSri, fechaDesde, fechaHasta), sorted)
        .map(this::toResumenResponse);
  }

  @Transactional
  public ComprobanteResponse guardarBorrador(
      UUID empresaId, FacturaRequest request, UsuarioPrincipal principal) {
    return persistirBorrador(empresaId, null, request, principal);
  }

  @Transactional
  public ComprobanteResponse actualizarBorrador(
      UUID empresaId, UUID id, FacturaRequest request, UsuarioPrincipal principal) {
    Comprobante existente =
        comprobanteRepository
            .findByIdAndEmpresa_Id(id, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comprobante no encontrado"));
    if (!ESTADO_BORRADOR.equalsIgnoreCase(existente.getEstadoSri())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se pueden editar facturas en borrador");
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
    if (!ESTADO_BORRADOR.equalsIgnoreCase(c.getEstadoSri())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El comprobante no está en borrador");
    }
    FacturaRequest request = requestDesdeBorrador(c);
    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
      c.setIdempotencyKey(idempotencyKey.trim());
    }
    return completarEmisionDesdeBorrador(empresaId, c, request, principal);
  }

  /** Puntos de emisión de la empresa (para UI de emisión sin permiso de tributario completo). */
  @Transactional(readOnly = true)
  public List<PuntoEmisionEmitirOption> puntosParaEmitir(UUID empresaId) {
    return puntoEmisionRepository.findByEmpresa_IdOrderByEstablecimiento_CodigoAscCodigoAsc(empresaId).stream()
        .filter(p -> "ACTIVO".equals(p.getEstado()) && "ACTIVO".equals(p.getEstablecimiento().getEstado()))
        .map(
            p ->
                new PuntoEmisionEmitirOption(
                    p.getId(),
                    p.getEstablecimiento().getCodigo(),
                    p.getCodigo(),
                    p.getNombre()))
        .toList();
  }

  @Transactional(readOnly = true)
  public String obtenerXml(UUID empresaId, UUID id, String tipo) {
    Comprobante c =
        comprobanteRepository
            .findByIdAndEmpresa_Id(id, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comprobante no encontrado"));
    ComprobanteArchivo archivo =
        comprobanteArchivoRepository
            .findFirstByComprobante_IdAndTipoOrderByFechaCreacionDesc(c.getId(), tipo)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo no encontrado"));
    try {
      return localComprobanteStorage.leerTexto(archivo.getStorageKey());
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo leer el XML");
    }
  }

  @Transactional
  public ComprobanteResponse reprocesarAutorizacion(UUID empresaId, UUID id) {
    Comprobante c =
        comprobanteRepository
            .findByIdAndEmpresa_Id(id, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comprobante no encontrado"));
    comprobanteSriProcesoService.reconsultarAutorizacion(c, c.getEmpresa());
    return toResponse(c);
  }

  /**
   * Regenera XML, firma y envía al SRI conservando clave de acceso, establecimiento, punto y secuencial.
   */
  @Transactional
  public ComprobanteResponse reemitirAlSri(UUID empresaId, UUID id) {
    Comprobante c =
        comprobanteRepository
            .findByIdAndEmpresa_Id(id, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comprobante no encontrado"));
    ComprobanteSriReemisionSupport.validarPuedeReemitir(c.getEstadoSri());
    Certificado certificado =
        certificadoRepository
            .findFirstByEmpresa_IdAndActivoParaFirmaIsTrue(empresaId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.PRECONDITION_REQUIRED, "No existe certificado activo para firma"));
    FacturaRequest request = requestDesdeBorrador(c);
    ejecutarProcesoSri(c, c.getEmpresa(), request, certificado, null);
    asegurarRide(c);
    return toResponse(c);
  }

  @Transactional
  public boolean reenviarCorreoComprobante(UUID empresaId, UUID id, String emailReceptor) {
    Comprobante c =
        comprobanteRepository
            .findByIdAndEmpresa_Id(id, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comprobante no encontrado"));
    if (ESTADO_BORRADOR.equalsIgnoreCase(c.getEstadoSri())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El borrador no tiene comprobante emitido");
    }
    if (!EstadoSri.AUTORIZADO.equalsIgnoreCase(c.getEstadoSri())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Solo se puede reenviar el correo cuando el comprobante está autorizado por el SRI");
    }
    if (emailReceptor != null && !emailReceptor.isBlank()) {
      Map<String, Object> cd =
          new HashMap<>(c.getCustomData() != null ? c.getCustomData() : Map.of());
      ComprobanteEmailUtil.aplicarEmailReceptor(cd, emailReceptor);
      c.setCustomData(cd);
      comprobanteRepository.save(c);
    }
    comprobanteSriProcesoService.asegurarRide(c);
    return comprobanteNotificacionService.enviarComprobanteCliente(c);
  }

  @Transactional
  public byte[] obtenerRide(UUID empresaId, UUID id) {
    Comprobante c =
        comprobanteRepository
            .findByIdAndEmpresa_Id(id, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comprobante no encontrado"));
    // Siempre regenera con la plantilla vigente (la copia almacenada puede quedar obsoleta).
    c.getEmpresa().getConfigExtra();
    byte[] pdf = comprobanteRideService.generarPdf(c);
    guardarRidePdf(c, pdf);
    return pdf;
  }

  private void registrarAuditoriaEmision(UUID empresaId, Comprobante c, UsuarioPrincipal principal) {
    Map<String, Object> detalle = new HashMap<>();
    detalle.put("claveAcceso", c.getClaveAcceso());
    detalle.put("estadoSri", c.getEstadoSri());
    detalle.put(
        "secuencial",
        c.getEstablecimientoCodigo() + "-" + c.getPuntoEmisionCodigo() + "-" + c.getSecuencial());
    detalle.put("valorTotal", c.getValorTotal() == null ? null : c.getValorTotal().toPlainString());
    detalle.put("origen", c.getOrigen());
    String actor = principal.getEmail();
    if (principal.getApiKeyId() != null) {
      actor = "apiKey:" + principal.getApiKeyId();
    }
    auditoriaService.registrar(
        "FACTURA_EMITIDA", empresaId, actor, "Comprobante", c.getId(), detalle);
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

  private ComprobanteArchivo asegurarRide(Comprobante c) {
    return comprobanteArchivoRepository
        .findFirstByComprobante_IdAndTipoOrderByFechaCreacionDesc(c.getId(), "RIDE_PDF")
        .orElseGet(() -> registrarRide(c));
  }

  private ComprobanteArchivo registrarRide(Comprobante c) {
    byte[] pdf = comprobanteRideService.generarPdf(c);
    return guardarRidePdf(c, pdf);
  }

  private ComprobanteArchivo guardarRidePdf(Comprobante c, byte[] pdf) {
    ComprobanteArchivo a =
        comprobanteArchivoRepository
            .findFirstByComprobante_IdAndTipoOrderByFechaCreacionDesc(c.getId(), "RIDE_PDF")
            .orElseGet(() -> {
              ComprobanteArchivo nuevo = new ComprobanteArchivo();
              nuevo.setComprobante(c);
              nuevo.setTipo("RIDE_PDF");
              return nuevo;
            });
    a.setSha256(Sha256.hex(pdf));
    a.setTamanoBytes((long) pdf.length);
    try {
      a.setStorageKey(localComprobanteStorage.guardarBytes(c.getEmpresa().getId(), c.getId(), "RIDE_PDF", "pdf", pdf));
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo almacenar el RIDE");
    }
    return comprobanteArchivoRepository.save(a);
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

  private ComprobanteResponse toResumenResponse(Comprobante c) {
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
        ultimoMensajeSri(c.getId()),
        ComprobanteVendedorMapper.vendedorId(c),
        ComprobanteVendedorMapper.vendedorNombre(c),
        c.getCustomData(),
        List.of());
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
        ultimoMensajeSri(c.getId()),
        ComprobanteVendedorMapper.vendedorId(c),
        ComprobanteVendedorMapper.vendedorNombre(c),
        c.getCustomData(),
        detalles);
  }

  private String ultimoMensajeSri(UUID comprobanteId) {
    return comprobanteLogSriRepository
        .findFirstByComprobante_IdOrderByFechaDesc(comprobanteId)
        .map(log -> log.getErrorMensaje())
        .filter(m -> m != null && !m.isBlank())
        .orElse(null);
  }

  private static Totales calcularTotales(List<FacturaItemRequest> items) {
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
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Descuento mayor al subtotal de línea");
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

  private static void validarPagos(FacturaRequest request, BigDecimal total) {
    if (request.pagos() == null || request.pagos().isEmpty()) {
      return;
    }
    BigDecimal pagos =
        request.pagos().stream()
            .map(p -> p.total() == null ? BigDecimal.ZERO : p.total())
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
    if (pagos.compareTo(total.setScale(2, RoundingMode.HALF_UP)) != 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "La suma de pagos debe coincidir con el importe total");
    }
  }

  private static void validarPuntoActivo(PuntoEmision punto) {
    if (!"ACTIVO".equals(punto.getEstado()) || !"ACTIVO".equals(punto.getEstablecimiento().getEstado())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "El establecimiento o punto de emision se encuentra inactivo");
    }
  }

  private ComprobanteResponse persistirBorrador(
      UUID empresaId, Comprobante existente, FacturaRequest request, UsuarioPrincipal principal) {
    PuntoEmision punto = resolverPunto(empresaId, request.puntoEmisionId());
    Empresa empresa = punto.getEmpresa();
    Totales totales = calcularTotales(request.items());
    Comprobante c = existente != null ? existente : new Comprobante();
    if (existente == null) {
      UUID provisional = UUID.randomUUID();
      c.setSecuencial(secuencialProvisional(provisional));
      c.setClaveAcceso(claveProvisional(provisional));
      c.setTipo("FACTURA");
      c.setTipoCodigo(TiposComprobanteSri.FACTURA);
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
    c.setCustomData(customDataEnriquecida(request));
    aplicarVendedor(c, empresaId, request);
    c.setUsuarioModificacion(principal.getEmail());
    c.setFechaModificacion(Instant.now());
    c = comprobanteRepository.save(c);
    comprobanteDetalleRepository.deleteByComprobante_Id(c.getId());
    guardarDetalles(c, empresa, request.items(), principal.getEmail());
    dashboardCacheService.evictEmpresa(empresaId);
    return toResponse(c);
  }

  private ComprobanteResponse completarEmisionDesdeBorrador(
      UUID empresaId, Comprobante c, FacturaRequest request, UsuarioPrincipal principal) {
    PuntoEmision punto = resolverPunto(empresaId, request.puntoEmisionId());
    Empresa empresa = punto.getEmpresa();
    Certificado certificado =
        certificadoRepository
            .findFirstByEmpresa_IdAndActivoParaFirmaIsTrue(empresaId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.PRECONDITION_REQUIRED, "No existe certificado activo para firma"));
    Totales totales = calcularTotales(request.items());
    validarPagos(request, totales.total());
    long sec = secuencialService.reservarSiguiente(empresaId, punto.getId(), TiposComprobanteSri.FACTURA, principal);
    String secuencial9 = String.format("%09d", sec);
    String claveAcceso =
        ClaveAccesoGenerator.generar(
            request.fechaEmisionOrToday(),
            TiposComprobanteSri.FACTURA,
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
    c.setCustomData(customDataEnriquecida(request));
    aplicarVendedor(c, empresaId, request);
    c.setUsuarioModificacion(principal.getEmail());
    c.setFechaModificacion(Instant.now());
    c = comprobanteRepository.save(c);
    comprobanteDetalleRepository.deleteByComprobante_Id(c.getId());
    guardarDetalles(c, empresa, request.items(), principal.getEmail());
    ejecutarProcesoSri(c, empresa, request, certificado, principal);
    asegurarRide(c);
    registrarAuditoriaEmision(empresaId, c, principal);
    dashboardCacheService.evictEmpresa(empresaId);
    return toResponse(c);
  }

  private FacturaRequest requestDesdeBorrador(Comprobante c) {
    Map<String, Object> cd = c.getCustomData() == null ? Map.of() : c.getCustomData();
    Object puntoRaw = cd.get("puntoEmisionId");
    if (puntoRaw == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Borrador sin punto de emision");
    }
    UUID puntoId = UUID.fromString(String.valueOf(puntoRaw));
    String tipoId = String.valueOf(cd.getOrDefault("tipoIdentificacionReceptor", "04"));
    String email =
        cd.get("emailReceptor") != null && !String.valueOf(cd.get("emailReceptor")).isBlank()
            ? String.valueOf(cd.get("emailReceptor")).trim()
            : null;
    List<ComprobanteDetalle> detalles =
        comprobanteDetalleRepository.findByComprobante_IdOrderByLineaAsc(c.getId());
    List<FacturaItemRequest> items = detalles.stream().map(this::itemDesdeDetalle).toList();
    if (items.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El borrador no tiene lineas de detalle");
    }
    UUID vendedorId = c.getVendedor() != null ? c.getVendedor().getId() : null;
    if (vendedorId == null && cd.get("vendedorId") != null) {
      try {
        vendedorId = UUID.fromString(String.valueOf(cd.get("vendedorId")));
      } catch (IllegalArgumentException ignored) {
        vendedorId = null;
      }
    }
    return new FacturaRequest(
        puntoId,
        c.getFechaEmision(),
        tipoId,
        c.getIdentificacionReceptor(),
        c.getRazonSocialReceptor(),
        email,
        items,
        List.of(new PagoRequest("20", c.getValorTotal(), null, null)),
        cd,
        vendedorId,
        null);
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

  private void ejecutarProcesoSri(
      Comprobante c, Empresa empresa, FacturaRequest request, Certificado certificado, UsuarioPrincipal principal) {
    c.setEstadoSri(EstadoSri.FIRMADO);
    comprobanteRepository.save(c);
    String xmlGenerado = xmlFacturaGeneratorService.generarXmlInicial(c, request);
    try {
      xmlXsdValidatorService.validarFactura(xmlGenerado);
    } catch (IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), e);
    }
    SignedXml xmlFirmado = xmlSignerService.firmar(xmlGenerado, certificado);
    registrarArchivo(c, "XML_GENERADO", xmlGenerado);
    registrarArchivo(c, xmlFirmado.stub() ? "XML_FIRMADO_STUB" : "XML_FIRMADO", xmlFirmado.xml());
    var recepcion = sriRecepcionClient.enviar(xmlFirmado.xml(), c.getAmbienteSri());
    c.setEstadoSri(normalizarEstadoRecepcion(recepcion.estado()));
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

  private PuntoEmision resolverPunto(UUID empresaId, UUID puntoEmisionId) {
    PuntoEmision punto =
        puntoEmisionRepository
            .findByIdAndEmpresa_Id(puntoEmisionId, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Punto de emision no encontrado"));
    validarPuntoActivo(punto);
    return punto;
  }

  private static Map<String, Object> customDataEnriquecida(FacturaRequest request) {
    Map<String, Object> cd = new HashMap<>(request.safeCustomData());
    cd.put("puntoEmisionId", request.puntoEmisionId().toString());
    cd.put("tipoIdentificacionReceptor", request.tipoIdentificacionReceptor());
    if (request.emailReceptor() != null && !request.emailReceptor().isBlank()) {
      cd.put("emailReceptor", request.emailReceptor().trim());
    }
    if (request.pagos() != null && !request.pagos().isEmpty()) {
      List<Map<String, Object>> pagos = new ArrayList<>();
      for (var pago : request.pagos()) {
        Map<String, Object> row = new HashMap<>();
        row.put("formaPago", pago.formaPago());
        row.put("total", pago.total());
        if (pago.plazo() != null) {
          row.put("plazo", pago.plazo());
        }
        if (pago.unidadTiempo() != null && !pago.unidadTiempo().isBlank()) {
          row.put("unidadTiempo", pago.unidadTiempo());
        }
        pagos.add(row);
      }
      cd.put("pagos", pagos);
    }
    if (request.vendedorId() != null) {
      cd.put("vendedorId", request.vendedorId().toString());
    }
    return cd;
  }

  private void aplicarVendedor(Comprobante c, UUID empresaId, FacturaRequest request) {
    if (request.vendedorId() == null) {
      c.setVendedor(null);
      return;
    }
    Vendedor v =
        vendedorRepository
            .findByIdAndEmpresa_Id(request.vendedorId(), empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendedor no encontrado"));
    c.setVendedor(v);
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

  private static Specification<Comprobante> filtrosListado(
      UUID empresaId,
      String tipo,
      String estadoSri,
      LocalDate fechaDesde,
      LocalDate fechaHasta) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(cb.equal(root.get("empresa").get("id"), empresaId));
      predicates.add(cb.notEqual(root.get("estado"), "ELIMINADO"));
      if (hasText(tipo)) {
        predicates.add(cb.equal(cb.upper(root.get("tipo")), tipo.trim().toUpperCase()));
      }
      if (hasText(estadoSri)) {
        predicates.add(cb.equal(cb.upper(root.get("estadoSri")), estadoSri.trim().toUpperCase()));
      }
      if (fechaDesde != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("fechaEmision"), fechaDesde));
      }
      if (fechaHasta != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("fechaEmision"), fechaHasta));
      }
      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private record Totales(
      BigDecimal subtotal,
      BigDecimal subtotal0,
      BigDecimal subtotalGravado,
      BigDecimal descuento,
      BigDecimal iva,
      BigDecimal total) {}
}
