package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.emision.ComprobanteDetalleResponse;
import ec.tusaas.efactura.dto.emision.ComprobanteResponse;
import ec.tusaas.efactura.dto.emision.FacturaItemRequest;
import ec.tusaas.efactura.entity.Comprobante;
import ec.tusaas.efactura.entity.ComprobanteDetalle;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.entity.PuntoEmision;
import ec.tusaas.efactura.repository.ComprobanteDetalleRepository;
import ec.tusaas.efactura.repository.ComprobanteRepository;
import ec.tusaas.efactura.repository.PuntoEmisionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class DocumentoEmisionSupport {

  private final PuntoEmisionRepository puntoEmisionRepository;
  private final ComprobanteRepository comprobanteRepository;
  private final ComprobanteDetalleRepository comprobanteDetalleRepository;

  public PuntoEmision resolverPunto(UUID empresaId, UUID puntoEmisionId) {
    PuntoEmision punto =
        puntoEmisionRepository
            .findByIdAndEmpresa_Id(puntoEmisionId, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Punto de emision no encontrado"));
    validarPuntoActivo(punto);
    return punto;
  }

  public static void validarPuntoActivo(PuntoEmision punto) {
    if (!"ACTIVO".equals(punto.getEstado()) || !"ACTIVO".equals(punto.getEstablecimiento().getEstado())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "El establecimiento o punto de emision se encuentra inactivo");
    }
  }

  public ComprobanteResponse toResponse(Comprobante c) {
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
        c.getCustomData(),
        detalles);
  }

  public void guardarDetalles(Comprobante c, Empresa empresa, List<FacturaItemRequest> items, String usuario) {
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

  public FacturaItemRequest itemDesdeDetalle(ComprobanteDetalle d) {
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

  public static Totales calcularTotales(List<FacturaItemRequest> items) {
    if (items == null || items.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El comprobante requiere al menos una linea");
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
        subtotal, subtotal0, subtotalGravado, descuento, iva, subtotal.add(iva).setScale(2, RoundingMode.HALF_UP));
  }

  public Comprobante guardarComprobante(Comprobante c) {
    return comprobanteRepository.save(c);
  }

  public void eliminarDetalles(UUID comprobanteId) {
    comprobanteDetalleRepository.deleteByComprobante_Id(comprobanteId);
  }

  public List<ComprobanteDetalle> listarDetalles(UUID comprobanteId) {
    return comprobanteDetalleRepository.findByComprobante_IdOrderByLineaAsc(comprobanteId);
  }

  public Comprobante buscarComprobante(UUID empresaId, UUID id) {
    return comprobanteRepository
        .findByIdAndEmpresa_Id(id, empresaId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comprobante no encontrado"));
  }

  public static BigDecimal lineSubtotal(FacturaItemRequest item) {
    return item.cantidad().multiply(item.precioUnitario()).subtract(nullToZero(item.descuento()));
  }

  public static BigDecimal nullToZero(BigDecimal v) {
    return v == null ? BigDecimal.ZERO : v;
  }

  public static String secuencialProvisional(UUID id) {
    String hex = id.toString().replace("-", "").toUpperCase();
    return ("B" + hex.substring(0, 8)).substring(0, 9);
  }

  public static String claveProvisional(UUID id) {
    String digits = id.toString().replaceAll("[^0-9]", "");
    if (digits.length() < 47) {
      digits = (digits + "12345678901234567890123456789012345678901234567").substring(0, 47);
    }
    return ("99" + digits).substring(0, 49);
  }

  public static UUID parseUuid(Object raw) {
    if (raw == null) {
      return null;
    }
    try {
      return UUID.fromString(String.valueOf(raw).trim());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public static void aplicarTotales(Comprobante c, Totales totales) {
    c.setSubtotalSinImpuestos(totales.subtotal());
    c.setSubtotal12(totales.subtotalGravado());
    c.setSubtotal0(totales.subtotal0());
    c.setDescuentoTotal(totales.descuento());
    c.setIvaTotal(totales.iva());
    c.setValorTotal(totales.total());
  }

  public static void marcarModificacion(Comprobante c, String usuario) {
    c.setUsuarioModificacion(usuario);
    c.setFechaModificacion(Instant.now());
  }

  public record Totales(
      BigDecimal subtotal,
      BigDecimal subtotal0,
      BigDecimal subtotalGravado,
      BigDecimal descuento,
      BigDecimal iva,
      BigDecimal total) {}
}
