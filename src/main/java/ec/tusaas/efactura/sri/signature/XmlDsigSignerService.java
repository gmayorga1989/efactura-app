package ec.tusaas.efactura.sri.signature;

import ec.tusaas.efactura.entity.Certificado;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.XMLObject;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Primary
@Service
@RequiredArgsConstructor
public class XmlDsigSignerService implements XmlSignerService {

  private static final String RSA_SHA256 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
  private static final String XADES_NS = "http://uri.etsi.org/01903/v1.3.2#";
  private static final String SIGNED_PROPERTIES_TYPE = "http://uri.etsi.org/01903#SignedProperties";
  private static final String C14N_INCLUSIVE = CanonicalizationMethod.INCLUSIVE;

  static {
    System.setProperty("com.sun.org.apache.xml.internal.security.ignoreLineBreaks", "true");
    System.setProperty("org.apache.xml.security.ignoreLineBreaks", "true");
  }

  private final Pkcs12CertificateLoader certificateLoader;

  /**
   * Firma XAdES-BES inicial: XMLDSig enveloped + objeto QualifyingProperties/SignedProperties.
   *
   * <p>Debe validarse contra la ficha técnica SRI vigente porque algunos ambientes SRI son sensibles a versión XAdES,
   * prefijos y canonicalización exacta.
   */
  @Override
  public SignedXml firmar(String xml, Certificado certificado) {
    try {
      var loaded = certificateLoader.load(certificado);
      Document doc = parse(xml);
      Element root = doc.getDocumentElement();
      String comprobanteId = registrarIdComprobante(root);

      XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

      String signatureId = "Signature-" + UUID.randomUUID();
      String signedPropertiesId = "SignedProperties-" + UUID.randomUUID();

      // Enveloped excluye ds:Signature del digest; sin él el SRI responde error 39 (firma alterada).
      List<Transform> documentTransforms =
          List.of(
              fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null),
              fac.newCanonicalizationMethod(C14N_INCLUSIVE, (C14NMethodParameterSpec) null));
      String documentUri = comprobanteId == null ? "" : "#" + comprobanteId;

      Reference documentReference =
          fac.newReference(
              documentUri,
              fac.newDigestMethod(DigestMethod.SHA256, null),
              documentTransforms,
              null,
              "Reference-Document");
      Reference signedPropertiesReference =
          fac.newReference(
              "#" + signedPropertiesId,
              fac.newDigestMethod(DigestMethod.SHA256, null),
              List.of(
                  fac.newCanonicalizationMethod(
                      CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null)),
              SIGNED_PROPERTIES_TYPE,
              "Reference-SignedProperties");
      SignedInfo si =
          fac.newSignedInfo(
              fac.newCanonicalizationMethod(
                  CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
              fac.newSignatureMethod(RSA_SHA256, null),
              List.of(documentReference, signedPropertiesReference));
      KeyInfo ki = keyInfo(fac.getKeyInfoFactory(), loaded.certificate());
      XMLObject xadesObject =
          fac.newXMLObject(
              List.of(
                  new DOMStructure(
                      qualifyingProperties(doc, signatureId, signedPropertiesId, loaded.certificate()))),
              "Object-XAdES-" + UUID.randomUUID(),
              null,
              null);
      DOMSignContext signContext = new DOMSignContext(loaded.privateKey(), root);
      signContext.setDefaultNamespacePrefix("ds");
      fac.newXMLSignature(si, ki, List.of(xadesObject), signatureId, null).sign(signContext);
      validarFirmaLocal(fac, doc, root, loaded);
      return new SignedXml(serializarSinRomperFirma(doc), false);
    } catch (Exception e) {
      throw new IllegalStateException("Error firmando XML con XAdES-BES inicial", e);
    }
  }

  /** Registra id="comprobante" (u otro) como ID XML para la referencia exigida por el SRI. */
  private static String registrarIdComprobante(Element root) {
    if (root == null || !root.hasAttribute("id")) {
      return null;
    }
    String id = root.getAttribute("id").trim();
    if (id.isEmpty()) {
      return null;
    }
    root.setIdAttribute("id", true);
    return id;
  }

  private static Document parse(String xml) throws Exception {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return dbf.newDocumentBuilder()
        .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
  }

  private static KeyInfo keyInfo(KeyInfoFactory kif, X509Certificate certificate) {
    X509Data xd = kif.newX509Data(List.of(certificate));
    return kif.newKeyInfo(List.of(xd));
  }

  private static Element qualifyingProperties(
      Document doc, String signatureId, String signedPropertiesId, X509Certificate certificate)
      throws Exception {
    Element qp = doc.createElementNS(XADES_NS, "xades:QualifyingProperties");
    qp.setAttribute("Target", "#" + signatureId);

    Element signedProperties = doc.createElementNS(XADES_NS, "xades:SignedProperties");
    signedProperties.setAttribute("Id", signedPropertiesId);
    signedProperties.setIdAttribute("Id", true);
    qp.appendChild(signedProperties);

    Element signedSignatureProperties = doc.createElementNS(XADES_NS, "xades:SignedSignatureProperties");
    signedProperties.appendChild(signedSignatureProperties);

    text(doc, signedSignatureProperties, "xades:SigningTime", Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
    signedSignatureProperties.appendChild(signingCertificate(doc, certificate));
    signedSignatureProperties.appendChild(signaturePolicyImplied(doc));
    return qp;
  }

  private static Element signingCertificate(Document doc, X509Certificate certificate) throws Exception {
    Element signingCertificate = doc.createElementNS(XADES_NS, "xades:SigningCertificate");
    Element cert = doc.createElementNS(XADES_NS, "xades:Cert");
    signingCertificate.appendChild(cert);

    Element certDigest = doc.createElementNS(XADES_NS, "xades:CertDigest");
    cert.appendChild(certDigest);
    Element digestMethod = doc.createElementNS(javax.xml.crypto.dsig.XMLSignature.XMLNS, "ds:DigestMethod");
    digestMethod.setAttribute("Algorithm", DigestMethod.SHA256);
    certDigest.appendChild(digestMethod);
    text(
        doc,
        certDigest,
        "ds:DigestValue",
        Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded())),
        javax.xml.crypto.dsig.XMLSignature.XMLNS);

    Element issuerSerial = doc.createElementNS(XADES_NS, "xades:IssuerSerial");
    cert.appendChild(issuerSerial);
    text(
        doc,
        issuerSerial,
        "ds:X509IssuerName",
        certificate.getIssuerX500Principal().getName(),
        javax.xml.crypto.dsig.XMLSignature.XMLNS);
    text(
        doc,
        issuerSerial,
        "ds:X509SerialNumber",
        certificate.getSerialNumber().toString(),
        javax.xml.crypto.dsig.XMLSignature.XMLNS);
    return signingCertificate;
  }

  private static Element signaturePolicyImplied(Document doc) {
    Element signaturePolicyIdentifier = doc.createElementNS(XADES_NS, "xades:SignaturePolicyIdentifier");
    signaturePolicyIdentifier.appendChild(doc.createElementNS(XADES_NS, "xades:SignaturePolicyImplied"));
    return signaturePolicyIdentifier;
  }

  private static void text(Document doc, Element parent, String qName, String value) {
    text(doc, parent, qName, value, XADES_NS);
  }

  private static void text(Document doc, Element parent, String qName, String value, String ns) {
    Element el = doc.createElementNS(ns, qName);
    el.setTextContent(value);
    parent.appendChild(el);
  }

  private static void validarFirmaLocal(
      XMLSignatureFactory fac, Document doc, Element root, Pkcs12CertificateLoader.LoadedCertificate loaded)
      throws Exception {
    var nodes = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
    if (nodes.getLength() == 0) {
      throw new IllegalStateException("No se encontró ds:Signature tras firmar");
    }
    XMLSignature signature = fac.unmarshalXMLSignature(new DOMStructure(nodes.item(0)));
    DOMValidateContext validateContext = new DOMValidateContext(loaded.certificate().getPublicKey(), root);
    if (!signature.validate(validateContext)) {
      throw new IllegalStateException("Validación local de firma falló antes de enviar al SRI");
    }
  }

  private static String serializarSinRomperFirma(Document doc) throws Exception {
    StringWriter writer = new StringWriter();
    var transformer = TransformerFactory.newInstance().newTransformer();
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
    transformer.setOutputProperty(OutputKeys.INDENT, "no");
    transformer.transform(new DOMSource(doc), new StreamResult(writer));
    // El SRI rechaza firmas con CR (&#13;) dentro de SignatureValue / X509Certificate.
    return writer.toString().replace("\r", "");
  }
}
