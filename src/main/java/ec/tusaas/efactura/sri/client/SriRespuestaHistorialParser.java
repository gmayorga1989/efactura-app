package ec.tusaas.efactura.sri.client;

import ec.tusaas.efactura.dto.comprobante.SriMensajeHistorialDto;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Formatea respuestas SOAP del SRI para el historial electrónico. */
public final class SriRespuestaHistorialParser {

  private SriRespuestaHistorialParser() {}

  public static List<SriMensajeHistorialDto> mensajes(String xml) {
    if (xml == null || xml.isBlank() || esMarcadorStub(xml)) {
      return List.of();
    }
    List<SriMensajeHistorialDto> out = new ArrayList<>();
    try {
      Document doc = SoapXml.parse(xml);
      NodeList nodos = doc.getElementsByTagNameNS("*", "mensaje");
      if (nodos.getLength() == 0) {
        nodos = doc.getElementsByTagName("mensaje");
      }
      for (int i = 0; i < nodos.getLength(); i++) {
        Node n = nodos.item(i);
        if (!(n instanceof Element el)) {
          continue;
        }
        if (tieneHijoMensaje(el)) {
          out.add(
              new SriMensajeHistorialDto(
                  textoHijo(el, "identificador"),
                  textoHijo(el, "tipo"),
                  textoHijo(el, "mensaje"),
                  textoHijo(el, "informacionAdicional")));
        }
      }
      if (out.isEmpty()) {
        String estado = SoapXml.textByLocalName(xml, "estado");
        String mensaje = SoapXml.textByLocalName(xml, "mensaje");
        if (mensaje != null && !mensaje.isBlank()) {
          out.add(new SriMensajeHistorialDto(null, estado, mensaje, null));
        }
      }
    } catch (Exception ignored) {
      return List.of();
    }
    return List.copyOf(out);
  }

  public static String resumenLegible(String xml, String errorMensaje) {
    List<SriMensajeHistorialDto> msgs = mensajes(xml);
    if (!msgs.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      for (SriMensajeHistorialDto m : msgs) {
        if (sb.length() > 0) {
          sb.append(" · ");
        }
        if (m.identificador() != null && !m.identificador().isBlank()) {
          sb.append('[').append(m.identificador()).append("] ");
        }
        if (m.mensaje() != null) {
          sb.append(m.mensaje().trim());
        }
        if (m.informacionAdicional() != null && !m.informacionAdicional().isBlank()) {
          sb.append(" — ").append(m.informacionAdicional().trim());
        }
      }
      return sb.toString();
    }
    if (errorMensaje != null && !errorMensaje.isBlank()) {
      return errorMensaje.trim();
    }
    String estado = xml == null ? null : SoapXml.textByLocalName(xml, "estado");
    if (estado != null && !estado.isBlank()) {
      return "Estado SRI: " + estado;
    }
    return resumenTextoPlano(xml);
  }

  public static String formatearXml(String raw) {
    if (raw == null || raw.isBlank()) {
      return "";
    }
    String t = raw.trim();
    if (!t.startsWith("<")) {
      return t;
    }
    try {
      Document doc = SoapXml.parse(t);
      Transformer tf = TransformerFactory.newInstance().newTransformer();
      tf.setOutputProperty(OutputKeys.INDENT, "yes");
      tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
      StringWriter sw = new StringWriter();
      tf.transform(new DOMSource(doc), new StreamResult(sw));
      return sw.toString();
    } catch (Exception e) {
      return t;
    }
  }

  private static boolean esMarcadorStub(String raw) {
    return raw.contains("_STUB");
  }

  private static boolean tieneHijoMensaje(Element el) {
    return textoHijo(el, "mensaje") != null || textoHijo(el, "identificador") != null;
  }

  private static String textoHijo(Element parent, String localName) {
    NodeList hijos = parent.getElementsByTagNameNS("*", localName);
    if (hijos.getLength() == 0) {
      hijos = parent.getElementsByTagName(localName);
    }
    for (int i = 0; i < hijos.getLength(); i++) {
      Node n = hijos.item(i);
      if (n.getParentNode() == parent && n.getTextContent() != null) {
        String v = n.getTextContent().trim();
        if (!v.isEmpty()) {
          return v;
        }
      }
    }
    return null;
  }

  private static String resumenTextoPlano(String raw) {
    if (raw == null || raw.isBlank()) {
      return "";
    }
    String t = raw.trim().replaceAll("\\s+", " ");
    return t.length() > 220 ? t.substring(0, 217) + "…" : t;
  }
}
