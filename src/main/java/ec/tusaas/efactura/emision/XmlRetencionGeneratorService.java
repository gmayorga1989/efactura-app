package ec.tusaas.efactura.emision;

import ec.tusaas.efactura.dto.emision.RetencionImpuestoRequest;
import ec.tusaas.efactura.dto.emision.RetencionRequest;
import ec.tusaas.efactura.entity.Comprobante;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Service
public class XmlRetencionGeneratorService {

  private static final Set<String> INFO_ADICIONAL_OMITIR =
      Set.of("puntoEmisionId", "tipoIdentificacionSujetoRetenido");

  public String generarXmlInicial(Comprobante c, RetencionRequest req) {
    try {
      Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      Element root = doc.createElement("comprobanteRetencion");
      root.setAttribute("id", "comprobante");
      root.setAttribute("version", "1.0.0");
      doc.appendChild(root);

      root.appendChild(XmlComprobanteDomUtil.infoTributaria(doc, c));
      root.appendChild(infoRetencion(doc, c, req));
      root.appendChild(impuestos(doc, req.impuestos()));
      XmlComprobanteDomUtil.appendInfoAdicional(doc, root, c.getCustomData(), INFO_ADICIONAL_OMITIR);
      return XmlComprobanteDomUtil.serialize(doc);
    } catch (Exception e) {
      throw new IllegalStateException("No se pudo generar XML de retencion", e);
    }
  }

  private static Element infoRetencion(Document doc, Comprobante c, RetencionRequest req) {
    Element info = doc.createElement("infoCompRetencion");
    XmlComprobanteDomUtil.text(doc, info, "fechaEmision", c.getFechaEmision().format(XmlComprobanteDomUtil.SRI_DATE));
    XmlComprobanteDomUtil.text(doc, info, "dirEstablecimiento", c.getEmpresa().getDireccionMatriz());
    XmlComprobanteDomUtil.text(
        doc, info, "obligadoContabilidad", c.getEmpresa().isObligadoContabilidad() ? "SI" : "NO");
    XmlComprobanteDomUtil.text(doc, info, "tipoIdentificacionSujetoRetenido", req.tipoIdentificacionSujetoRetenido());
    XmlComprobanteDomUtil.text(doc, info, "razonSocialSujetoRetenido", req.razonSocialSujetoRetenido());
    XmlComprobanteDomUtil.text(doc, info, "identificacionSujetoRetenido", req.identificacionSujetoRetenido());
    XmlComprobanteDomUtil.text(doc, info, "periodoFiscal", req.periodoFiscal());
    return info;
  }

  private static Element impuestos(Document doc, List<RetencionImpuestoRequest> impuestos) {
    Element impuestosEl = doc.createElement("impuestos");
    for (RetencionImpuestoRequest imp : impuestos) {
      Element impuesto = doc.createElement("impuesto");
      XmlComprobanteDomUtil.text(doc, impuesto, "codigo", imp.codigo());
      XmlComprobanteDomUtil.text(doc, impuesto, "codigoRetencion", imp.codigoRetencion());
      XmlComprobanteDomUtil.text(doc, impuesto, "baseImponible", XmlComprobanteDomUtil.money(imp.baseImponible()));
      XmlComprobanteDomUtil.text(doc, impuesto, "porcentajeRetener", XmlComprobanteDomUtil.money(imp.porcentajeRetener()));
      XmlComprobanteDomUtil.text(doc, impuesto, "valorRetenido", XmlComprobanteDomUtil.money(imp.valorRetenido()));
      String codDoc = imp.codDocSustento() == null || imp.codDocSustento().isBlank() ? "01" : imp.codDocSustento();
      XmlComprobanteDomUtil.text(doc, impuesto, "codDocSustento", codDoc);
      if (imp.numDocSustento() != null && !imp.numDocSustento().isBlank()) {
        XmlComprobanteDomUtil.text(doc, impuesto, "numDocSustento", imp.numDocSustento());
      }
      if (imp.fechaEmisionDocSustento() != null) {
        XmlComprobanteDomUtil.text(
            doc, impuesto, "fechaEmisionDocSustento", imp.fechaEmisionDocSustento().format(XmlComprobanteDomUtil.SRI_DATE));
      }
      impuestosEl.appendChild(impuesto);
    }
    return impuestosEl;
  }
}
