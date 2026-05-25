package ec.tusaas.efactura.emision;

import ec.tusaas.efactura.dto.emision.FacturaItemRequest;
import ec.tusaas.efactura.dto.emision.GuiaRemisionRequest;
import ec.tusaas.efactura.entity.Comprobante;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Service
public class XmlGuiaRemisionGeneratorService {

  private static final Set<String> INFO_ADICIONAL_OMITIR =
      Set.of("puntoEmisionId", "facturaSustentoId", "facturaOrigenId");

  public record DocSustento(String numDocSustento, LocalDate fechaEmisionDocSustento) {}

  public String generarXmlInicial(
      Comprobante c, GuiaRemisionRequest req, DocSustento docSustento, List<FacturaItemRequest> items) {
    try {
      Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      Element root = doc.createElement("guiaRemision");
      root.setAttribute("id", "comprobante");
      root.setAttribute("version", "1.1.0");
      doc.appendChild(root);

      root.appendChild(XmlComprobanteDomUtil.infoTributaria(doc, c));
      root.appendChild(infoGuia(doc, c, req));
      root.appendChild(destinatarios(doc, c, req, docSustento, items));
      XmlComprobanteDomUtil.appendInfoAdicional(doc, root, c.getCustomData(), INFO_ADICIONAL_OMITIR);
      return XmlComprobanteDomUtil.serialize(doc);
    } catch (Exception e) {
      throw new IllegalStateException("No se pudo generar XML de guia de remision", e);
    }
  }

  private static Element infoGuia(Document doc, Comprobante c, GuiaRemisionRequest req) {
    Element info = doc.createElement("infoGuiaRemision");
    XmlComprobanteDomUtil.text(doc, info, "dirEstablecimiento", c.getEmpresa().getDireccionMatriz());
    XmlComprobanteDomUtil.text(doc, info, "dirPartida", req.dirPartida());
    XmlComprobanteDomUtil.text(doc, info, "razonSocialTransportista", req.razonSocialTransportista());
    XmlComprobanteDomUtil.text(doc, info, "tipoIdentificacionTransportista", req.tipoIdentificacionTransportista());
    XmlComprobanteDomUtil.text(doc, info, "rucTransportista", req.identificacionTransportista());
    XmlComprobanteDomUtil.text(
        doc, info, "obligadoContabilidad", c.getEmpresa().isObligadoContabilidad() ? "SI" : "NO");
    XmlComprobanteDomUtil.text(
        doc, info, "fechaIniTransporte", req.fechaIniTransporteOrToday().format(XmlComprobanteDomUtil.SRI_DATE));
    XmlComprobanteDomUtil.text(
        doc, info, "fechaFinTransporte", req.fechaFinTransporteOrToday().format(XmlComprobanteDomUtil.SRI_DATE));
    XmlComprobanteDomUtil.text(doc, info, "placa", req.placa());
    return info;
  }

  private static Element destinatarios(
      Document doc, Comprobante c, GuiaRemisionRequest req, DocSustento docSustento, List<FacturaItemRequest> items) {
    Element destinatarios = doc.createElement("destinatarios");
    Element dest = doc.createElement("destinatario");
    XmlComprobanteDomUtil.text(doc, dest, "identificacionDestinatario", req.identificacionDestinatario());
    XmlComprobanteDomUtil.text(doc, dest, "razonSocialDestinatario", req.razonSocialDestinatario());
    XmlComprobanteDomUtil.text(doc, dest, "dirDestinatario", req.dirDestinatario());
    XmlComprobanteDomUtil.text(doc, dest, "motivoTraslado", req.motivoTraslado());
    if (docSustento != null) {
      XmlComprobanteDomUtil.text(doc, dest, "codDocSustento", XmlComprobanteDomUtil.COD_DOC_FACTURA);
      XmlComprobanteDomUtil.text(doc, dest, "numDocSustento", docSustento.numDocSustento());
      XmlComprobanteDomUtil.text(
          doc, dest, "fechaEmisionDocSustento", docSustento.fechaEmisionDocSustento().format(XmlComprobanteDomUtil.SRI_DATE));
    }
    dest.appendChild(detallesDestinatario(doc, items));
    destinatarios.appendChild(dest);
    return destinatarios;
  }

  private static Element detallesDestinatario(Document doc, List<FacturaItemRequest> items) {
    Element detalles = doc.createElement("detalles");
    for (FacturaItemRequest item : items) {
      Element detalle = doc.createElement("detalle");
      XmlComprobanteDomUtil.text(doc, detalle, "codigoInterno", item.codigoPrincipal());
      XmlComprobanteDomUtil.text(doc, detalle, "codigoAdicional", item.codigoAuxiliar());
      XmlComprobanteDomUtil.text(doc, detalle, "descripcion", item.descripcion());
      XmlComprobanteDomUtil.text(doc, detalle, "cantidad", XmlComprobanteDomUtil.decimal6(item.cantidad()));
      detalles.appendChild(detalle);
    }
    return detalles;
  }
}
