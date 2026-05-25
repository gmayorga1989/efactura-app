package ec.tusaas.efactura.service;



import ec.tusaas.efactura.dto.ride.RidePlantillaDto;

import ec.tusaas.efactura.emision.ride.RideDocumentoTitulo;
import ec.tusaas.efactura.tributario.TiposComprobanteSri;

import ec.tusaas.efactura.emision.ride.RideLayoutPdfBuilder;

import ec.tusaas.efactura.emision.ride.RideLogoService;

import ec.tusaas.efactura.entity.Comprobante;

import ec.tusaas.efactura.entity.ComprobanteDetalle;

import ec.tusaas.efactura.entity.Empresa;

import ec.tusaas.efactura.repository.EmpresaRepository;

import ec.tusaas.efactura.security.UsuarioPrincipal;

import java.math.BigDecimal;
import java.time.Instant;

import java.time.LocalDate;

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

public class RidePlantillaService {



  private final EmpresaRepository empresaRepository;

  private final EmpresaTributarioService empresaTributarioService;

  private final RideLogoService rideLogoService;



  public RidePlantillaDto desdeEmpresa(Empresa empresa) {

    return desdeEmpresa(empresa, "FACTURA");

  }



  @SuppressWarnings("unchecked")

  public RidePlantillaDto desdeEmpresa(Empresa empresa, String tipoComprobante) {

    if (empresa == null || empresa.getConfigExtra() == null) {

      return RidePlantillaDto.porDefecto();

    }

    String tipo = RideDocumentoTitulo.normalizarTipo(tipoComprobante);

    Map<String, Object> extra = empresa.getConfigExtra();



    Object porTipos = extra.get(RidePlantillaDto.PLANTILLAS_KEY);

    if (porTipos instanceof Map<?, ?> map) {

      Object raw = map.get(tipo);

      if (raw != null) {

        return RidePlantillaDto.fromMap(raw);

      }

    }



    Object legado = extra.get(RidePlantillaDto.CONFIG_KEY);

    if (legado != null) {

      return RidePlantillaDto.fromMap(legado);

    }



    return RidePlantillaDto.porDefecto();

  }



  @Transactional(readOnly = true)

  public RidePlantillaDto obtener(UUID empresaId, String tipoComprobante, UsuarioPrincipal principal) {

    empresaTributarioService.validarGestionEmpresa(empresaId, principal);

    Empresa empresa = cargarEmpresa(empresaId);

    return desdeEmpresa(empresa, tipoComprobante);

  }



  @Transactional

  public RidePlantillaDto guardar(

      UUID empresaId, String tipoComprobante, RidePlantillaDto dto, UsuarioPrincipal principal) {

    empresaTributarioService.validarGestionEmpresa(empresaId, principal);

    Empresa empresa = cargarEmpresa(empresaId);

    String tipo = RideDocumentoTitulo.normalizarTipo(tipoComprobante);



    Map<String, Object> extra = new HashMap<>(empresa.getConfigExtra());

    Map<String, Object> plantillas = leerMapaPlantillas(extra);

    plantillas.put(tipo, dto.toMap());

    extra.put(RidePlantillaDto.PLANTILLAS_KEY, plantillas);

    extra.remove(RidePlantillaDto.CONFIG_KEY);



    empresa.setConfigExtra(extra);

    empresa.setFechaModificacion(Instant.now());

    empresa.setUsuarioModificacion(principal.getEmail());

    empresaRepository.save(empresa);

    return desdeEmpresa(empresa, tipo);

  }



  @Transactional(readOnly = true)

  public byte[] vistaPrevia(

      UUID empresaId, String tipoComprobante, RidePlantillaDto plantilla, UsuarioPrincipal principal) {

    empresaTributarioService.validarGestionEmpresa(empresaId, principal);

    Empresa empresa = cargarEmpresa(empresaId);

    String tipo = RideDocumentoTitulo.normalizarTipo(tipoComprobante);

    Comprobante muestra = comprobanteMuestra(empresa, tipo);

    List<ComprobanteDetalle> detalles = detallesMuestra(muestra);

    RideDocumentoTitulo titulo = RideDocumentoTitulo.fromTipo(tipo);

    var logo = rideLogoService.cargarLogo(empresa);

    return RideLayoutPdfBuilder.generar(muestra, detalles, plantilla, titulo, logo);

  }



  private Empresa cargarEmpresa(UUID empresaId) {

    return empresaRepository

        .findById(empresaId)

        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));

  }



  @SuppressWarnings("unchecked")

  private static Map<String, Object> leerMapaPlantillas(Map<String, Object> extra) {

    Object raw = extra.get(RidePlantillaDto.PLANTILLAS_KEY);

    if (raw instanceof Map<?, ?> map) {

      Map<String, Object> out = new HashMap<>();

      map.forEach((k, v) -> out.put(String.valueOf(k), v));

      return out;

    }

    Map<String, Object> out = new HashMap<>();

    Object legado = extra.get(RidePlantillaDto.CONFIG_KEY);

    if (legado instanceof Map<?, ?> legacy) {

      out.put("FACTURA", legacy);

    }

    return out;

  }



  private static Comprobante comprobanteMuestra(Empresa empresa, String tipo) {
    Comprobante c = new Comprobante();
    c.setEmpresa(empresa);
    c.setTipo(tipo);
    c.setTipoCodigo(tipoCodigoSri(tipo));
    c.setEstablecimientoCodigo("001");
    c.setPuntoEmisionCodigo("001");
    c.setSecuencial("000000123");
    c.setClaveAcceso("1234567890123456789012345678901234567890123456789");
    c.setFechaEmision(LocalDate.now());
    c.setRazonSocialReceptor("CLIENTE DE PRUEBA S.A.");
    c.setIdentificacionReceptor("0999999999001");
    c.setSubtotalSinImpuestos(new BigDecimal("100.00"));
    c.setDescuentoTotal(BigDecimal.ZERO);
    c.setIvaTotal(new BigDecimal("15.00"));
    c.setValorTotal(new BigDecimal("115.00"));
    c.setAmbienteSri(empresa.getAmbienteSri());
    c.setTipoEmision(empresa.getTipoEmision());
    c.setEstadoSri("AUTORIZADO");
    c.setNumeroAutorizacion(c.getClaveAcceso());
    c.setFechaAutorizacion(Instant.now());
    c.setCustomData(customDataMuestra(tipo));
    if ("RETENCION".equals(tipo)) {
      c.setRazonSocialReceptor("PROVEEDOR DE PRUEBA S.A.");
      c.setValorTotal(new BigDecimal("32.00"));
      c.setSubtotalSinImpuestos(BigDecimal.ZERO);
      c.setIvaTotal(BigDecimal.ZERO);
    }
    if ("GUIA_REMISION".equals(tipo)) {
      c.setValorTotal(BigDecimal.ZERO);
      c.setSubtotalSinImpuestos(BigDecimal.ZERO);
      c.setIvaTotal(BigDecimal.ZERO);
    }
    return c;
  }

  private static String tipoCodigoSri(String tipo) {
    return switch (tipo) {
      case "NOTA_CREDITO" -> TiposComprobanteSri.NOTA_CREDITO;
      case "NOTA_DEBITO" -> TiposComprobanteSri.NOTA_DEBITO;
      case "GUIA_REMISION" -> TiposComprobanteSri.GUIA_REMISION;
      case "RETENCION" -> TiposComprobanteSri.RETENCION;
      case "LIQUIDACION_COMPRA" -> TiposComprobanteSri.LIQUIDACION_COMPRA;
      default -> TiposComprobanteSri.FACTURA;
    };
  }

  private static Map<String, Object> customDataMuestra(String tipo) {
    Map<String, Object> cd = new HashMap<>();
    cd.put("direccionReceptor", "Av. Ejemplo 123 y calle secundaria");
    switch (tipo) {
      case "NOTA_CREDITO" -> {
        cd.put("motivo", "Devolución de mercadería (ejemplo)");
        cd.put("tipoComprobanteModificado", "FACTURA");
        cd.put("numeroFactura", "001-001-000000002");
        cd.put("fechaEmisionFacturaModificada", LocalDate.now().minusDays(5).toString());
      }
      case "NOTA_DEBITO" -> {
        cd.put("motivo", "Interés por mora");
        cd.put("tipoComprobanteModificado", "FACTURA");
        cd.put("numeroFactura", "001-001-000000002");
        cd.put("fechaEmisionFacturaModificada", LocalDate.now().minusDays(3).toString());
      }
      case "GUIA_REMISION" -> {
        cd.put("dirPartida", "Av. Amazonas N34-451, Quito");
        cd.put("placa", "PBC1234");
        cd.put("motivoTraslado", "Venta");
        cd.put("dirDestinatario", "Av. 6 de Diciembre, Quito");
        cd.put("identificacionTransportista", "0999999999001");
        cd.put("razonSocialTransportista", "TRANSPORTES EJEMPLO C.A.");
        cd.put("fechaIniTransporte", LocalDate.now().toString());
        cd.put("fechaFinTransporte", LocalDate.now().plusDays(1).toString());
        cd.put("numeroComprobanteVenta", "001-001-000000002");
        cd.put("fechaEmisionComprobanteVenta", LocalDate.now().minusDays(2).toString());
        cd.put("claveAccesoComprobanteVenta", "1234567890123456789012345678901234567890123456789");
        cd.put("ruta", "Quito — Tumbaco");
      }
      case "RETENCION" -> {
        cd.put("periodoFiscal", String.format("%02d/%d", LocalDate.now().getMonthValue(), LocalDate.now().getYear()));
        cd.put(
            "impuestos",
            List.of(
                Map.of(
                    "codigo", "2",
                    "codigoRetencion", "303",
                    "baseImponible", new BigDecimal("100.00"),
                    "porcentajeRetener", new BigDecimal("30"),
                    "valorRetenido", new BigDecimal("30.00"),
                    "codDocSustento", "01",
                    "numDocSustento", "001-001-000000456",
                    "fechaEmisionDocSustento", LocalDate.now().minusDays(10).toString()),
                Map.of(
                    "codigo", "1",
                    "codigoRetencion", "312",
                    "baseImponible", new BigDecimal("100.00"),
                    "porcentajeRetener", new BigDecimal("2"),
                    "valorRetenido", new BigDecimal("2.00"),
                    "codDocSustento", "01",
                    "numDocSustento", "001-001-000000456",
                    "fechaEmisionDocSustento", LocalDate.now().minusDays(10).toString())));
      }
      case "LIQUIDACION_COMPRA" -> {
        cd.put("direccionReceptor", "Calle proveedor 45, Guayaquil");
        cd.put(
            "pagos",
            List.of(Map.of("formaPago", "01", "total", new BigDecimal("115.00"))));
      }
      default -> cd.put(
          "pagos", List.of(Map.of("formaPago", "20", "total", new BigDecimal("115.00"))));
    }
    return cd;
  }

  private static List<ComprobanteDetalle> detallesMuestra(Comprobante c) {
    String tipo = c.getTipo() == null ? "FACTURA" : c.getTipo().toUpperCase();
    return switch (tipo) {
      case "NOTA_DEBITO" -> detalleUnico(c, "Interés por mora", "1", new BigDecimal("100.00"));
      case "GUIA_REMISION" ->
          detalleUnico(c, "Mercadería en tránsito — productos varios", "GR01", BigDecimal.ZERO);
      case "RETENCION" -> List.of();
      default -> detalleUnico(c, "Producto o servicio de ejemplo", "COD001", new BigDecimal("100.00"));
    };
  }

  private static List<ComprobanteDetalle> detalleUnico(
      Comprobante c, String descripcion, String codigo, BigDecimal precio) {
    List<ComprobanteDetalle> out = new ArrayList<>();
    ComprobanteDetalle d = new ComprobanteDetalle();
    d.setComprobante(c);
    d.setEmpresa(c.getEmpresa());
    d.setLinea(1);
    d.setCodigoPrincipal(codigo);
    d.setCodigoAuxiliar("");
    d.setDescripcion(descripcion);
    d.setCantidad(new BigDecimal("1.00"));
    d.setPrecioUnitario(precio);
    d.setDescuento(BigDecimal.ZERO);
    d.setPrecioTotalSinImpuesto(precio);
    Map<String, Object> cd = new HashMap<>();
    cd.put("ivaPorcentaje", 15);
    d.setCustomData(cd);
    out.add(d);
    return out;
  }

}

