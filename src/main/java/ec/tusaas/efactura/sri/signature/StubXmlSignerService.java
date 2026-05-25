package ec.tusaas.efactura.sri.signature;

import ec.tusaas.efactura.entity.Certificado;
import org.springframework.stereotype.Service;

@Service
public class StubXmlSignerService implements XmlSignerService {

  @Override
  public SignedXml firmar(String xml, Certificado certificado) {
    String signedStub =
        xml.replace(
            "</factura>",
            "<!-- FIRMA_STUB: reemplazar por XAdES-BES real usando certificado "
                + certificado.getId()
                + " --></factura>");
    return new SignedXml(signedStub, true);
  }
}
