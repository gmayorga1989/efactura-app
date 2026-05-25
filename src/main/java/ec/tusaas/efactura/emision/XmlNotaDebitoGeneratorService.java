package ec.tusaas.efactura.emision;



import ec.tusaas.efactura.dto.emision.DocumentoModificadoRequest;

import ec.tusaas.efactura.dto.emision.FacturaItemRequest;

import ec.tusaas.efactura.entity.Comprobante;

import java.time.LocalDate;

import java.util.List;

import java.util.Map;

import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Service;

import org.w3c.dom.Document;

import org.w3c.dom.Element;



@Service

public class XmlNotaDebitoGeneratorService {



  private static final Set<String> INFO_ADICIONAL_OMITIR =

      Set.of(

          "puntoEmisionId",

          "tipoIdentificacionReceptor",

          "facturaOrigenId",

          "facturaModificadaId",

          "claveAccesoFactura",

          "numeroFactura",

          "motivo",

          "emailReceptor",

          "subtotalSinImpuestos",

          "ivaTotal");



  public record DocumentoModificado(String numDocModificado, LocalDate fechaEmisionDocSustento) {}



  public String generarXmlInicial(

      Comprobante c, DocumentoModificadoRequest req, DocumentoModificado docModificado, List<FacturaItemRequest> items) {

    try {

      Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

      Element notaDebito = doc.createElement("notaDebito");

      notaDebito.setAttribute("id", "comprobante");

      notaDebito.setAttribute("version", "1.0.0");

      doc.appendChild(notaDebito);



      notaDebito.appendChild(XmlComprobanteDomUtil.infoTributaria(doc, c));

      notaDebito.appendChild(infoNotaDebito(doc, c, req, docModificado, items));

      notaDebito.appendChild(motivos(doc, req, c, items));

      XmlComprobanteDomUtil.appendInfoAdicional(doc, notaDebito, c.getCustomData(), INFO_ADICIONAL_OMITIR);

      return XmlComprobanteDomUtil.serialize(doc);

    } catch (Exception e) {

      throw new IllegalStateException("No se pudo generar XML de nota de debito", e);

    }

  }



  private static Element infoNotaDebito(

      Document doc,

      Comprobante c,

      DocumentoModificadoRequest req,

      DocumentoModificado docModificado,

      List<FacturaItemRequest> items) {

    Element info = doc.createElement("infoNotaDebito");

    XmlComprobanteDomUtil.text(doc, info, "fechaEmision", c.getFechaEmision().format(XmlComprobanteDomUtil.SRI_DATE));

    XmlComprobanteDomUtil.text(doc, info, "dirEstablecimiento", c.getEmpresa().getDireccionMatriz());

    XmlComprobanteDomUtil.text(doc, info, "tipoIdentificacionComprador", req.tipoIdentificacionReceptor());

    XmlComprobanteDomUtil.text(doc, info, "razonSocialComprador", c.getRazonSocialReceptor());

    XmlComprobanteDomUtil.text(doc, info, "identificacionComprador", c.getIdentificacionReceptor());

    XmlComprobanteDomUtil.text(

        doc, info, "obligadoContabilidad", c.getEmpresa().isObligadoContabilidad() ? "SI" : "NO");

    XmlComprobanteDomUtil.text(doc, info, "codDocModificado", XmlComprobanteDomUtil.COD_DOC_FACTURA);

    XmlComprobanteDomUtil.text(doc, info, "numDocModificado", docModificado.numDocModificado());

    XmlComprobanteDomUtil.text(

        doc, info, "fechaEmisionDocSustento", docModificado.fechaEmisionDocSustento().format(XmlComprobanteDomUtil.SRI_DATE));

    XmlComprobanteDomUtil.text(doc, info, "totalSinImpuestos", XmlComprobanteDomUtil.money(c.getSubtotalSinImpuestos()));

    info.appendChild(
        XmlComprobanteDomUtil.impuestosInfo(
            doc, items, c.getSubtotalSinImpuestos(), c.getIvaTotal()));

    XmlComprobanteDomUtil.text(doc, info, "valorTotal", XmlComprobanteDomUtil.money(c.getValorTotal()));

    return info;

  }



  private static Element motivos(

      Document doc, DocumentoModificadoRequest req, Comprobante c, List<FacturaItemRequest> items) {

    Element motivos = doc.createElement("motivos");

    String razonDefault = motivoXml(req, c);

    if (items != null && !items.isEmpty()) {

      for (FacturaItemRequest item : items) {

        Element motivo = doc.createElement("motivo");

        String razon =

            item.descripcion() != null && !item.descripcion().isBlank()

                ? item.descripcion().trim()

                : razonDefault;

        XmlComprobanteDomUtil.text(doc, motivo, "razon", razon);

        XmlComprobanteDomUtil.text(doc, motivo, "valor", XmlComprobanteDomUtil.money(XmlComprobanteDomUtil.lineSubtotal(item)));

        motivos.appendChild(motivo);

      }

    } else {

      Element motivo = doc.createElement("motivo");

      XmlComprobanteDomUtil.text(doc, motivo, "razon", razonDefault);

      XmlComprobanteDomUtil.text(doc, motivo, "valor", XmlComprobanteDomUtil.money(c.getSubtotalSinImpuestos()));

      motivos.appendChild(motivo);

    }

    return motivos;

  }



  private static String motivoXml(DocumentoModificadoRequest req, Comprobante c) {

    if (req.motivo() != null && !req.motivo().isBlank()) {

      return req.motivo().trim();

    }

    Map<String, Object> cd = c.getCustomData();

    if (cd != null && cd.get("motivo") != null) {

      return String.valueOf(cd.get("motivo")).trim();

    }

    return "Ajuste";

  }

}


