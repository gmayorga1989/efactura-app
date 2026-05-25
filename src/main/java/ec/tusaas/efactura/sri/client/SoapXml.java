package ec.tusaas.efactura.sri.client;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

final class SoapXml {

  private SoapXml() {}

  static String textByLocalName(String xml, String localName) {
    try {
      Document doc = parse(xml);
      NodeList nodes = doc.getElementsByTagNameNS("*", localName);
      if (nodes.getLength() == 0) {
        nodes = doc.getElementsByTagName(localName);
      }
      if (nodes.getLength() == 0 || nodes.item(0) == null) {
        return null;
      }
      return nodes.item(0).getTextContent();
    } catch (Exception e) {
      return null;
    }
  }

  static Document parse(String xml) throws Exception {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return dbf.newDocumentBuilder()
        .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
  }

  static String stripWsdl(String url) {
    if (url == null) {
      return null;
    }
    return url.endsWith("?wsdl") ? url.substring(0, url.length() - 5) : url;
  }
}
