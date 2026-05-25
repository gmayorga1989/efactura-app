package ec.tusaas.efactura.emision;

import ec.tusaas.efactura.dto.emision.FacturaItemRequest;
import ec.tusaas.efactura.dto.emision.FacturaRequest;
import ec.tusaas.efactura.entity.Comprobante;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Service
public class XmlLiquidacionCompraGeneratorService {

  private static final Set<String> INFO_ADICIONAL_OMITIR =
      Set.of("puntoEmisionId", "tipoIdentificacionReceptor", "emailReceptor");

  public String generarXmlInicial(Comprobante c, FacturaRequest req) {
    try {
      Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      Element root = doc.createElement("liquidacionCompra");
      root.setAttribute("id", "comprobante");
      root.setAttribute("version", "1.1.0");
      doc.appendChild(root);

      root.appendChild(XmlComprobanteDomUtil.infoTributaria(doc, c));
      root.appendChild(infoLiquidacion(doc, c, req));
      root.appendChild(XmlComprobanteDomUtil.detallesConImpuestos(doc, req.items(), false));
      XmlComprobanteDomUtil.appendInfoAdicional(doc, root, c.getCustomData(), INFO_ADICIONAL_OMITIR);
      return XmlComprobanteDomUtil.serialize(doc);
    } catch (Exception e) {
      throw new IllegalStateException("No se pudo generar XML de liquidacion de compra", e);
    }
  }

  private static Element infoLiquidacion(Document doc, Comprobante c, FacturaRequest req) {
    Element info = doc.createElement("infoLiquidacionCompra");
    XmlComprobanteDomUtil.text(doc, info, "fechaEmision", c.getFechaEmision().format(XmlComprobanteDomUtil.SRI_DATE));
    XmlComprobanteDomUtil.text(doc, info, "dirEstablecimiento", c.getEmpresa().getDireccionMatriz());
    XmlComprobanteDomUtil.text(
        doc, info, "obligadoContabilidad", c.getEmpresa().isObligadoContabilidad() ? "SI" : "NO");
    XmlComprobanteDomUtil.text(doc, info, "tipoIdentificacionProveedor", req.tipoIdentificacionReceptor());
    XmlComprobanteDomUtil.text(doc, info, "razonSocialProveedor", c.getRazonSocialReceptor());
    XmlComprobanteDomUtil.text(doc, info, "identificacionProveedor", c.getIdentificacionReceptor());
    XmlComprobanteDomUtil.text(doc, info, "totalSinImpuestos", XmlComprobanteDomUtil.money(c.getSubtotalSinImpuestos()));
    XmlComprobanteDomUtil.text(doc, info, "totalDescuento", XmlComprobanteDomUtil.money(c.getDescuentoTotal()));
    info.appendChild(XmlComprobanteDomUtil.totalConImpuestos(doc, req.items()));
    XmlComprobanteDomUtil.text(doc, info, "importeTotal", XmlComprobanteDomUtil.money(c.getValorTotal()));
    XmlComprobanteDomUtil.text(doc, info, "moneda", XmlComprobanteDomUtil.moneda(c));
    return info;
  }
}
