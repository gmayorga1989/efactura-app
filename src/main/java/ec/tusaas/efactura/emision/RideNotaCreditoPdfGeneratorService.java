package ec.tusaas.efactura.emision;

import ec.tusaas.efactura.entity.Comprobante;
import ec.tusaas.efactura.entity.ComprobanteDetalle;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RideNotaCreditoPdfGeneratorService {

  private static final int MAX_DETALLES = 18;

  public byte[] generar(Comprobante comprobante, List<ComprobanteDetalle> detalles) {
    List<String> lines = new ArrayList<>();
    var empresa = comprobante.getEmpresa();
    Map<String, Object> cd = comprobante.getCustomData() == null ? Map.of() : comprobante.getCustomData();

    lines.add("RIDE - NOTA DE CREDITO ELECTRONICA");
    lines.add("");
    lines.add("Emisor: " + empresa.getRazonSocial());
    lines.add("Nombre comercial: " + RidePdfUtil.valueOrDash(empresa.getNombreComercial()));
    lines.add("RUC: " + empresa.getRuc());
    lines.add("Direccion matriz: " + RidePdfUtil.valueOrDash(empresa.getDireccionMatriz()));
    lines.add("Obligado a llevar contabilidad: " + (empresa.isObligadoContabilidad() ? "SI" : "NO"));
    lines.add("");
    lines.add("Comprobante: " + comprobante.getEstablecimientoCodigo() + "-"
        + comprobante.getPuntoEmisionCodigo() + "-" + comprobante.getSecuencial());
    lines.add("Clave de acceso: " + comprobante.getClaveAcceso());
    lines.add("Estado SRI: " + comprobante.getEstadoSri());
    lines.add("Numero autorizacion: " + RidePdfUtil.valueOrDash(comprobante.getNumeroAutorizacion()));
    lines.add("Fecha autorizacion: " + fechaAutorizacion(comprobante));
    lines.add("Ambiente: " + (comprobante.getAmbienteSri() == 2 ? "PRODUCCION" : "PRUEBAS"));
    lines.add("");
    lines.add("Fecha emision: " + comprobante.getFechaEmision());
    lines.add("Cliente: " + RidePdfUtil.valueOrDash(comprobante.getRazonSocialReceptor()));
    lines.add("Identificacion: " + RidePdfUtil.valueOrDash(comprobante.getIdentificacionReceptor()));
    lines.add("");
    lines.add("Documento modificado (factura): " + docModificado(cd));
    lines.add("Motivo: " + RidePdfUtil.valueOrDash(cd.get("motivo") != null ? String.valueOf(cd.get("motivo")) : null));
    lines.add("");
    lines.add("DETALLE");
    lines.add("Cant.      Codigo        Descripcion                         Subtotal");
    for (ComprobanteDetalle detalle : detalles.stream().limit(MAX_DETALLES).toList()) {
      lines.add(
          RidePdfUtil.fit(RidePdfUtil.numeric(detalle.getCantidad()), 10)
              + RidePdfUtil.fit(RidePdfUtil.valueOrDash(detalle.getCodigoPrincipal()), 14)
              + RidePdfUtil.fit(RidePdfUtil.valueOrDash(detalle.getDescripcion()), 36)
              + RidePdfUtil.money(detalle.getPrecioTotalSinImpuesto()));
    }
    if (detalles.size() > MAX_DETALLES) {
      lines.add("... " + (detalles.size() - MAX_DETALLES) + " lineas adicionales");
    }
    lines.add("");
    lines.add("Subtotal sin impuestos: " + RidePdfUtil.money(comprobante.getSubtotalSinImpuestos()));
    lines.add("Descuento: " + RidePdfUtil.money(comprobante.getDescuentoTotal()));
    lines.add("IVA: " + RidePdfUtil.money(comprobante.getIvaTotal()));
    lines.add("Valor modificacion: " + RidePdfUtil.money(comprobante.getValorTotal()));
    lines.add("");
    lines.add("Representacion impresa del comprobante electronico.");
    return RidePdfUtil.simplePdf(lines);
  }

  private static String docModificado(Map<String, Object> cd) {
    Object num = cd.get("numeroFactura");
    if (num != null && !String.valueOf(num).isBlank()) {
      return String.valueOf(num).trim();
    }
    return RidePdfUtil.valueOrDash(null);
  }

  private static String fechaAutorizacion(Comprobante comprobante) {
    if (comprobante.getFechaAutorizacion() == null) {
      return "-";
    }
    return DateTimeFormatter.ISO_OFFSET_DATE_TIME
        .format(comprobante.getFechaAutorizacion().atZone(ZoneId.of("America/Guayaquil")));
  }
}
