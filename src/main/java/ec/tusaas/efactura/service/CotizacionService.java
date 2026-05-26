package ec.tusaas.efactura.service;

import ec.tusaas.efactura.cotizacion.CotizacionHtmlRenderer;
import ec.tusaas.efactura.cotizacion.CotizacionPlantillaUtil;
import ec.tusaas.efactura.dto.cotizacion.CotizacionAdjuntoRequest;
import ec.tusaas.efactura.dto.cotizacion.CotizacionAdjuntoResponse;
import ec.tusaas.efactura.dto.cotizacion.CotizacionConvertirRequest;
import ec.tusaas.efactura.dto.cotizacion.CotizacionEnviarCorreoRequest;
import ec.tusaas.efactura.dto.cotizacion.CotizacionItemRequest;
import ec.tusaas.efactura.dto.cotizacion.CotizacionItemResponse;
import ec.tusaas.efactura.dto.cotizacion.CotizacionRequest;
import ec.tusaas.efactura.dto.cotizacion.CotizacionResponse;
import ec.tusaas.efactura.dto.emision.ComprobanteResponse;
import ec.tusaas.efactura.dto.emision.FacturaItemRequest;
import ec.tusaas.efactura.dto.emision.FacturaRequest;
import ec.tusaas.efactura.entity.Cliente;
import ec.tusaas.efactura.entity.Cotizacion;
import ec.tusaas.efactura.entity.CotizacionAdjunto;
import ec.tusaas.efactura.entity.CotizacionDetalle;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.entity.Vendedor;
import ec.tusaas.efactura.repository.ClienteRepository;
import ec.tusaas.efactura.repository.ComprobanteRepository;
import ec.tusaas.efactura.repository.CotizacionAdjuntoRepository;
import ec.tusaas.efactura.repository.CotizacionDetalleRepository;
import ec.tusaas.efactura.repository.CotizacionRepository;
import ec.tusaas.efactura.repository.EmpresaRepository;
import ec.tusaas.efactura.repository.VendedorRepository;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.storage.ObjectStorageService;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CotizacionService {

  private final EmpresaRepository empresaRepository;
  private final CotizacionRepository cotizacionRepository;
  private final CotizacionDetalleRepository cotizacionDetalleRepository;
  private final CotizacionAdjuntoRepository cotizacionAdjuntoRepository;
  private final ClienteRepository clienteRepository;
  private final VendedorRepository vendedorRepository;
  private final ComprobanteRepository comprobanteRepository;
  private final FacturaElectronicaService facturaElectronicaService;
  private final EmailNotificationService emailNotificationService;
  private final CotizacionAdjuntoStorageService cotizacionAdjuntoStorageService;
  private final ObjectStorageService objectStorageService;

  @Transactional(readOnly = true)
  public Page<CotizacionResponse> listar(
      UUID empresaId,
      String estado,
      UUID vendedorId,
      LocalDate fechaDesde,
      LocalDate fechaHasta,
      String q,
      Pageable pageable) {
    Specification<Cotizacion> spec =
        (root, query, cb) -> {
          List<Predicate> preds = new ArrayList<>();
          preds.add(cb.equal(root.get("empresa").get("id"), empresaId));
          preds.add(cb.equal(root.get("estadoRegistro"), "ACTIVO"));
          if (estado != null && !estado.isBlank()) {
            preds.add(cb.equal(cb.upper(root.get("estado")), estado.trim().toUpperCase()));
          }
          if (vendedorId != null) {
            preds.add(cb.equal(root.get("vendedor").get("id"), vendedorId));
          }
          if (fechaDesde != null) {
            preds.add(cb.greaterThanOrEqualTo(root.get("fechaEmision"), fechaDesde));
          }
          if (fechaHasta != null) {
            preds.add(cb.lessThanOrEqualTo(root.get("fechaEmision"), fechaHasta));
          }
          if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            preds.add(
                cb.or(
                    cb.like(cb.lower(root.get("numero")), like),
                    cb.like(cb.lower(root.get("razonSocialReceptor")), like),
                    cb.like(cb.lower(root.get("identificacionReceptor")), like)));
          }
          query.orderBy(cb.desc(root.get("fechaEmision")), cb.desc(root.get("fechaCreacion")));
          return cb.and(preds.toArray(Predicate[]::new));
        };
    return cotizacionRepository.findAll(spec, pageable).map(this::toResponseResumen);
  }

  @Transactional(readOnly = true)
  public CotizacionResponse obtener(UUID empresaId, UUID id) {
    return toResponseCompleto(load(empresaId, id));
  }

  @Transactional(readOnly = true)
  public String previewHtml(UUID empresaId, UUID id) {
    Cotizacion c = load(empresaId, id);
    return CotizacionHtmlRenderer.render(
        c.getEmpresa(),
        c,
        cotizacionDetalleRepository.findByCotizacion_IdOrderByLineaAsc(id),
        cotizacionAdjuntoRepository.findByCotizacion_IdAndEstadoOrderByOrdenAsc(id, "ACTIVO"),
        null);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> plantillaEmpresa(UUID empresaId) {
    Empresa e = loadEmpresa(empresaId);
    Object raw = e.getConfigExtra() != null ? e.getConfigExtra().get(CotizacionPlantillaUtil.CONFIG_EMPRESA_KEY) : null;
    if (raw instanceof Map<?, ?> m) {
      Map<String, Object> map = new HashMap<>();
      m.forEach((k, v) -> map.put(String.valueOf(k), v));
      return CotizacionPlantillaUtil.merge(CotizacionPlantillaUtil.porDefecto(), map);
    }
    return CotizacionPlantillaUtil.porDefecto();
  }

  @Transactional
  public Map<String, Object> guardarPlantillaEmpresa(UUID empresaId, Map<String, Object> plantilla, UsuarioPrincipal principal) {
    Empresa e = loadEmpresa(empresaId);
    Map<String, Object> custom = e.getConfigExtra() != null ? new HashMap<>(e.getConfigExtra()) : new HashMap<>();
    custom.put(CotizacionPlantillaUtil.CONFIG_EMPRESA_KEY, CotizacionPlantillaUtil.merge(CotizacionPlantillaUtil.porDefecto(), plantilla));
    e.setConfigExtra(custom);
    e.setUsuarioModificacion(principal.getEmail());
    empresaRepository.save(e);
    return plantillaEmpresa(empresaId);
  }

  @Transactional(readOnly = true)
  public String previewPlantillaEmpresaHtml(UUID empresaId, Map<String, Object> plantilla) {
    Empresa empresa = loadEmpresa(empresaId);
    Map<String, Object> merged =
        CotizacionPlantillaUtil.merge(plantillaEmpresa(empresaId), plantilla != null ? plantilla : Map.of());
    return CotizacionHtmlRenderer.renderDemo(empresa, merged);
  }

  @Transactional
  public Map<String, Object> subirBannerPlantillaEmpresa(
      UUID empresaId, MultipartFile archivo, UsuarioPrincipal principal) throws Exception {
    if (archivo == null || archivo.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Imagen vacía");
    }
    if (archivo.getSize() > 3 * 1024 * 1024) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Banner máximo 3 MB");
    }
    String ct = archivo.getContentType();
    if (ct == null || !(ct.startsWith("image/"))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo imágenes para banner");
    }
    Empresa e = loadEmpresa(empresaId);
    String key = "cotizaciones/" + empresaId + "/banner-" + UUID.randomUUID() + ".img";
    objectStorageService.guardarPublico(key, archivo.getBytes(), ct);
    String url = objectStorageService.publicUrl(key);
    Map<String, Object> plantilla = plantillaEmpresa(empresaId);
    plantilla.put("bannerImageUrl", url);
    plantilla.put("bannerStorageKey", key);
    guardarPlantillaEmpresa(empresaId, plantilla, principal);
    return Map.of("bannerImageUrl", url);
  }

  @Transactional
  public CotizacionAdjuntoResponse subirAdjuntoArchivo(
      UUID empresaId, UUID cotizacionId, MultipartFile archivo, UsuarioPrincipal principal) throws Exception {
    return cotizacionAdjuntoStorageService.subirArchivo(empresaId, cotizacionId, archivo, principal);
  }

  public void eliminarAdjunto(UUID empresaId, UUID cotizacionId, UUID adjuntoId, UsuarioPrincipal principal)
      throws Exception {
    cotizacionAdjuntoStorageService.eliminarAdjunto(empresaId, cotizacionId, adjuntoId, principal);
  }

  @Transactional
  public CotizacionResponse crear(UUID empresaId, CotizacionRequest body, UsuarioPrincipal principal) {
    return persistir(empresaId, null, body, principal);
  }

  @Transactional
  public CotizacionResponse actualizar(UUID empresaId, UUID id, CotizacionRequest body, UsuarioPrincipal principal) {
    Cotizacion c = load(empresaId, id);
    if ("CONVERTIDA".equalsIgnoreCase(c.getEstado())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "La cotización ya fue convertida a factura");
    }
    return persistir(empresaId, c, body, principal);
  }

  @Transactional
  public void anular(UUID empresaId, UUID id, UsuarioPrincipal principal) {
    Cotizacion c = load(empresaId, id);
    c.setEstado("ANULADA");
    c.setUsuarioModificacion(principal.getEmail());
    c.setFechaModificacion(Instant.now());
    cotizacionRepository.save(c);
  }

  @Transactional
  public CotizacionResponse aceptar(UUID empresaId, UUID id, UsuarioPrincipal principal) {
    Cotizacion c = load(empresaId, id);
    validarEstadoComercial(c, Set.of("BORRADOR", "ENVIADA"));
    c.setEstado("ACEPTADA");
    c.setUsuarioModificacion(principal.getEmail());
    c.setFechaModificacion(Instant.now());
    cotizacionRepository.save(c);
    return toResponseCompleto(c);
  }

  @Transactional
  public CotizacionResponse rechazar(UUID empresaId, UUID id, UsuarioPrincipal principal) {
    Cotizacion c = load(empresaId, id);
    validarEstadoComercial(c, Set.of("BORRADOR", "ENVIADA", "ACEPTADA"));
    c.setEstado("RECHAZADA");
    c.setUsuarioModificacion(principal.getEmail());
    c.setFechaModificacion(Instant.now());
    cotizacionRepository.save(c);
    return toResponseCompleto(c);
  }

  private static void validarEstadoComercial(Cotizacion c, Set<String> permitidos) {
    String estado = c.getEstado() != null ? c.getEstado().toUpperCase() : "";
    if ("CONVERTIDA".equals(estado) || "ANULADA".equals(estado)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Estado no permite cambiar la cotización");
    }
    if (!permitidos.contains(estado)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "No se puede cambiar el estado desde " + c.getEstado());
    }
  }

  @Transactional
  public ComprobanteResponse convertirAFactura(
      UUID empresaId, UUID id, CotizacionConvertirRequest body, UsuarioPrincipal principal) {
    Cotizacion c = load(empresaId, id);
    if ("CONVERTIDA".equalsIgnoreCase(c.getEstado())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya convertida");
    }
    if ("ANULADA".equalsIgnoreCase(c.getEstado())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Cotización anulada");
    }
    List<CotizacionDetalle> detalles = cotizacionDetalleRepository.findByCotizacion_IdOrderByLineaAsc(id);
    if (detalles.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sin líneas de detalle");
    }
    List<FacturaItemRequest> items =
        detalles.stream()
            .map(
                d ->
                    new FacturaItemRequest(
                        d.getCodigoPrincipal() != null ? d.getCodigoPrincipal() : "ITEM",
                        d.getCodigoAuxiliar(),
                        d.getDescripcion(),
                        d.getCantidad(),
                        d.getPrecioUnitario(),
                        d.getDescuento() != null ? d.getDescuento() : BigDecimal.ZERO,
                        d.getIvaPorcentaje(),
                        null,
                        null))
            .toList();
    Map<String, Object> custom = new HashMap<>();
    custom.put("cotizacionId", id.toString());
    custom.put("cotizacionNumero", c.getNumero());
    FacturaRequest factura =
        new FacturaRequest(
            body.puntoEmisionId(),
            c.getFechaEmision(),
            c.getTipoIdentificacionReceptor(),
            c.getIdentificacionReceptor(),
            c.getRazonSocialReceptor(),
            c.getEmailReceptor(),
            items,
            List.of(),
            custom,
            c.getVendedor() != null ? c.getVendedor().getId() : null,
            c.getEmailReceptor() != null ? List.of(c.getEmailReceptor()) : List.of());
    ComprobanteResponse resp = facturaElectronicaService.guardarBorrador(empresaId, factura, principal);
    var comprobante =
        comprobanteRepository
            .findByIdAndEmpresa_Id(resp.id(), empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Factura no creada"));
    if (c.getVendedor() != null) {
      comprobante.setVendedor(c.getVendedor());
      comprobanteRepository.save(comprobante);
    }
    c.setEstado("CONVERTIDA");
    c.setComprobante(comprobante);
    c.setFechaConversion(Instant.now());
    c.setUsuarioModificacion(principal.getEmail());
    c.setFechaModificacion(Instant.now());
    cotizacionRepository.save(c);
    return resp;
  }

  @Transactional
  public boolean enviarCorreo(UUID empresaId, UUID id, CotizacionEnviarCorreoRequest body) {
    Cotizacion c = load(empresaId, id);
    List<CotizacionDetalle> detalles = cotizacionDetalleRepository.findByCotizacion_IdOrderByLineaAsc(id);
    List<CotizacionAdjunto> adjuntos = cotizacionAdjuntoRepository.findByCotizacion_IdAndEstadoOrderByOrdenAsc(id, "ACTIVO");
    String html = CotizacionHtmlRenderer.render(c.getEmpresa(), c, detalles, adjuntos, body.mensajeAdicional());
    String subject =
        body.asunto() != null && !body.asunto().isBlank()
            ? body.asunto().trim()
            : "Cotización " + c.getNumero() + " — " + c.getEmpresa().getRazonSocial();
    boolean ok = false;
    for (String email : body.destinatarios()) {
      if (email != null && !email.isBlank()) {
        ok =
            emailNotificationService.enviar(
                    empresaId,
                    "COTIZACION_ENVIADA",
                    email.trim(),
                    c.getRazonSocialReceptor(),
                    subject,
                    html,
                    "Cotización " + c.getNumero(),
                    Map.of("cotizacionId", id.toString(), "numero", c.getNumero()))
                || ok;
      }
    }
    if (ok && !"CONVERTIDA".equalsIgnoreCase(c.getEstado())) {
      c.setEstado("ENVIADA");
      c.setFechaEnvio(Instant.now());
      cotizacionRepository.save(c);
    }
    return ok;
  }

  private CotizacionResponse persistir(UUID empresaId, Cotizacion existente, CotizacionRequest body, UsuarioPrincipal principal) {
    Empresa empresa = loadEmpresa(empresaId);
    Totales tot = calcularTotales(body.items());
    Cotizacion c = existente != null ? existente : new Cotizacion();
    if (existente == null) {
      c.setEmpresa(empresa);
      c.setNumero(generarNumero(empresaId, body.fechaEmisionOrToday()));
      c.setUsuarioCreacion(principal.getEmail());
    }
    c.setFechaEmision(body.fechaEmisionOrToday());
    c.setValidezDias(body.validezDiasOrDefault());
    c.setTipoIdentificacionReceptor(body.tipoIdentificacionReceptor());
    c.setIdentificacionReceptor(body.identificacionReceptor());
    c.setRazonSocialReceptor(body.razonSocialReceptor());
    c.setEmailReceptor(body.emailReceptor());
    c.setSubtotalSinImpuestos(tot.subtotal());
    c.setDescuentoTotal(tot.descuento());
    c.setIvaTotal(tot.iva());
    c.setValorTotal(tot.total());
    c.setIntroduccionHtml(body.introduccionHtml());
    c.setCondicionesHtml(body.condicionesHtml());
    c.setPlantillaJson(
        CotizacionPlantillaUtil.merge(
            plantillaEmpresa(empresaId),
            body.plantillaJson() != null ? body.plantillaJson() : Map.of()));
    if (body.clienteId() != null) {
      Cliente cl =
          clienteRepository
              .findByIdAndEmpresa_Id(body.clienteId(), empresaId)
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
      c.setCliente(cl);
    } else {
      c.setCliente(null);
    }
    if (body.vendedorId() != null) {
      c.setVendedor(loadVendedor(empresaId, body.vendedorId()));
    } else {
      c.setVendedor(null);
    }
    c.setUsuarioModificacion(principal.getEmail());
    c.setFechaModificacion(Instant.now());
    c = cotizacionRepository.save(c);
    cotizacionDetalleRepository.deleteByCotizacion_Id(c.getId());
    guardarDetalles(c, empresa, body.items());
    cotizacionAdjuntoRepository.deleteByCotizacion_IdAndTipo(c.getId(), "ENLACE");
    guardarAdjuntos(c, empresa, body.adjuntos());
    return toResponseCompleto(c);
  }

  private void guardarDetalles(Cotizacion c, Empresa empresa, List<CotizacionItemRequest> items) {
    int linea = 1;
    for (CotizacionItemRequest it : items) {
      CotizacionDetalle d = new CotizacionDetalle();
      d.setCotizacion(c);
      d.setEmpresa(empresa);
      d.setLinea(linea++);
      d.setCodigoPrincipal(it.codigoPrincipal());
      d.setCodigoAuxiliar(it.codigoAuxiliar());
      d.setDescripcion(it.descripcion());
      d.setCantidad(it.cantidad());
      d.setPrecioUnitario(it.precioUnitario());
      d.setDescuento(it.descuento() != null ? it.descuento() : BigDecimal.ZERO);
      d.setIvaPorcentaje(it.ivaPorcentaje());
      BigDecimal bruto = it.cantidad().multiply(it.precioUnitario());
      BigDecimal neto = bruto.subtract(d.getDescuento()).max(BigDecimal.ZERO);
      d.setPrecioTotalSinImpuesto(neto.setScale(2, RoundingMode.HALF_UP));
      cotizacionDetalleRepository.save(d);
    }
  }

  private void guardarAdjuntos(Cotizacion c, Empresa empresa, List<CotizacionAdjuntoRequest> adjuntos) {
    if (adjuntos == null) {
      return;
    }
    int orden = 0;
    for (CotizacionAdjuntoRequest a : adjuntos) {
      if (a.url() == null || a.url().isBlank()) {
        continue;
      }
      CotizacionAdjunto adj = new CotizacionAdjunto();
      adj.setCotizacion(c);
      adj.setEmpresa(empresa);
      adj.setTipo("ENLACE");
      adj.setProveedor(normalizarProveedor(a.proveedor(), a.url()));
      adj.setTitulo(a.titulo());
      adj.setUrl(a.url().trim());
      adj.setOrden(a.orden() != null ? a.orden() : orden++);
      cotizacionAdjuntoRepository.save(adj);
    }
  }

  private String normalizarProveedor(String proveedor, String url) {
    if (proveedor != null && !proveedor.isBlank()) {
      return proveedor.trim().toUpperCase();
    }
    String u = url.toLowerCase();
    if (u.contains("drive.google") || u.contains("docs.google")) {
      return "GOOGLE_DRIVE";
    }
    if (u.contains("onedrive") || u.contains("sharepoint")) {
      return "ONEDRIVE";
    }
    if (u.contains("dropbox")) {
      return "DROPBOX";
    }
    return "OTRO";
  }

  private String generarNumero(UUID empresaId, LocalDate fecha) {
    String prefijo = "COT-" + fecha.getYear() + "-";
    long n = cotizacionRepository.countByEmpresa_IdAndNumeroStartingWith(empresaId, prefijo) + 1;
    return prefijo + String.format("%05d", n);
  }

  private Totales calcularTotales(List<CotizacionItemRequest> items) {
    BigDecimal subtotal = BigDecimal.ZERO;
    BigDecimal descuento = BigDecimal.ZERO;
    BigDecimal iva = BigDecimal.ZERO;
    for (CotizacionItemRequest it : items) {
      BigDecimal bruto = it.cantidad().multiply(it.precioUnitario());
      BigDecimal desc = it.descuento() != null ? it.descuento() : BigDecimal.ZERO;
      BigDecimal neto = bruto.subtract(desc).max(BigDecimal.ZERO);
      subtotal = subtotal.add(neto);
      descuento = descuento.add(desc);
      if (it.ivaPorcentaje() != null && it.ivaPorcentaje().signum() > 0) {
        iva =
            iva.add(
                neto.multiply(it.ivaPorcentaje()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
      }
    }
    BigDecimal total = subtotal.add(iva);
    return new Totales(subtotal.setScale(2, RoundingMode.HALF_UP), descuento.setScale(2, RoundingMode.HALF_UP), iva.setScale(2, RoundingMode.HALF_UP), total.setScale(2, RoundingMode.HALF_UP));
  }

  private record Totales(BigDecimal subtotal, BigDecimal descuento, BigDecimal iva, BigDecimal total) {}

  private Cotizacion load(UUID empresaId, UUID id) {
    return cotizacionRepository
        .findByIdAndEmpresa_Id(id, empresaId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cotización no encontrada"));
  }

  private Empresa loadEmpresa(UUID empresaId) {
    return empresaRepository
        .findById(empresaId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));
  }

  private Vendedor loadVendedor(UUID empresaId, UUID id) {
    return vendedorRepository
        .findByIdAndEmpresa_Id(id, empresaId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendedor no encontrado"));
  }

  private CotizacionResponse toResponseResumen(Cotizacion c) {
    return new CotizacionResponse(
        c.getId(),
        c.getNumero(),
        c.getFechaEmision(),
        c.getValidezDias(),
        c.getFechaEmision().plusDays(c.getValidezDias()),
        c.getEstado(),
        c.getCliente() != null ? c.getCliente().getId() : null,
        c.getVendedor() != null ? c.getVendedor().getId() : null,
        c.getVendedor() != null ? nombreVendedor(c.getVendedor()) : null,
        c.getTipoIdentificacionReceptor(),
        c.getIdentificacionReceptor(),
        c.getRazonSocialReceptor(),
        c.getEmailReceptor(),
        c.getMoneda(),
        c.getSubtotalSinImpuestos(),
        c.getDescuentoTotal(),
        c.getIvaTotal(),
        c.getValorTotal(),
        null,
        null,
        c.getPlantillaJson(),
        c.getComprobante() != null ? c.getComprobante().getId() : null,
        c.getFechaEnvio(),
        c.getFechaConversion(),
        List.of(),
        List.of());
  }

  private CotizacionResponse toResponseCompleto(Cotizacion c) {
    UUID id = c.getId();
    List<CotizacionItemResponse> items =
        cotizacionDetalleRepository.findByCotizacion_IdOrderByLineaAsc(id).stream().map(this::toItem).toList();
    List<CotizacionAdjuntoResponse> adj =
        cotizacionAdjuntoRepository.findByCotizacion_IdAndEstadoOrderByOrdenAsc(id, "ACTIVO").stream()
            .map(CotizacionAdjuntoStorageService::toResponse)
            .toList();
    CotizacionResponse base = toResponseResumen(c);
    return new CotizacionResponse(
        base.id(),
        base.numero(),
        base.fechaEmision(),
        base.validezDias(),
        base.fechaVencimiento(),
        base.estado(),
        base.clienteId(),
        base.vendedorId(),
        base.vendedorNombre(),
        base.tipoIdentificacionReceptor(),
        base.identificacionReceptor(),
        base.razonSocialReceptor(),
        base.emailReceptor(),
        base.moneda(),
        base.subtotalSinImpuestos(),
        base.descuentoTotal(),
        base.ivaTotal(),
        base.valorTotal(),
        c.getIntroduccionHtml(),
        c.getCondicionesHtml(),
        c.getPlantillaJson(),
        base.comprobanteId(),
        base.fechaEnvio(),
        base.fechaConversion(),
        items,
        adj);
  }

  private CotizacionItemResponse toItem(CotizacionDetalle d) {
    return new CotizacionItemResponse(
        d.getId(),
        d.getLinea(),
        d.getProducto() != null ? d.getProducto().getId() : null,
        d.getCodigoPrincipal(),
        d.getCodigoAuxiliar(),
        d.getDescripcion(),
        d.getCantidad(),
        d.getPrecioUnitario(),
        d.getDescuento(),
        d.getIvaPorcentaje(),
        d.getPrecioTotalSinImpuesto());
  }

  private String nombreVendedor(Vendedor v) {
    String n = v.getNombres() != null ? v.getNombres().trim() : "";
    String a = v.getApellidos() != null ? v.getApellidos().trim() : "";
    return (n + " " + a).trim();
  }
}
