package ec.tusaas.efactura.sri.signature;

import ec.tusaas.efactura.entity.Certificado;

public interface XmlSignerService {

  SignedXml firmar(String xml, Certificado certificado);
}
