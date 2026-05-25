package ec.tusaas.efactura.emision;

import ec.tusaas.efactura.entity.Comprobante;
import ec.tusaas.efactura.entity.ComprobanteDetalle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RideDocumentoGenericoPdfGeneratorService {

  public byte[] generar(Comprobante c, List<ComprobanteDetalle> detalles, String titulo) {
    List<String> lines = new ArrayList<>();
    var empresa = c.getEmpresa();
    lines.add("RIDE - " + titulo);
    lines.add("");
    lines.add("Emisor: " + empresa.getRazonSocial());
    lines.add("RUC: " + empresa.getRuc());
    lines.add("Comprobante: " + c.getEstablecimientoCodigo() + "-" + c.getPuntoEmisionCodigo() + "-" + c.getSecuencial());
    lines.add("Clave de acceso: " + c.getClaveAcceso());
    lines.add("Estado SRI: " + c.getEstadoSri());
    lines.add("Fecha emision: " + c.getFechaEmision());
    lines.add("Receptor/Sujeto: " + RidePdfUtil.valueOrDash(c.getRazonSocialReceptor()));
    lines.add("Identificacion: " + RidePdfUtil.valueOrDash(c.getIdentificacionReceptor()));
    Map<String, Object> cd = c.getCustomData() == null ? Map.of() : c.getCustomData();
    if (cd.get("motivo") != null) {
      lines.add("Motivo: " + cd.get("motivo"));
    }
    if (cd.get("numeroFactura") != null) {
      lines.add("Doc. sustento: " + cd.get("numeroFactura"));
    }
    if (cd.get("periodoFiscal") != null) {
      lines.add("Periodo fiscal: " + cd.get("periodoFiscal"));
    }
    lines.add("");
    if (!detalles.isEmpty()) {
      lines.add("DETALLE");
      for (ComprobanteDetalle d : detalles.stream().limit(18).toList()) {
        lines.add(
            RidePdfUtil.fit(RidePdfUtil.valueOrDash(d.getDescripcion()), 50)
                + " "
                + RidePdfUtil.money(d.getPrecioTotalSinImpuesto()));
      }
    }
    lines.add("");
    lines.add("Valor total: " + RidePdfUtil.money(c.getValorTotal()));
    lines.add("Representacion impresa del comprobante electronico.");
    return RidePdfUtil.simplePdf(lines);
  }
}
