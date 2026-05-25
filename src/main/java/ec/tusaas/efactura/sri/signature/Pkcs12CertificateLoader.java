package ec.tusaas.efactura.sri.signature;

import ec.tusaas.efactura.crypto.AesGcmSecretCrypto;
import ec.tusaas.efactura.entity.Certificado;
import ec.tusaas.efactura.storage.LocalCertificadoStorage;
import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Pkcs12CertificateLoader {

  private final LocalCertificadoStorage localCertificadoStorage;
  private final AesGcmSecretCrypto aesGcmSecretCrypto;

  public LoadedCertificate load(Certificado certificado) {
    try {
      String password = aesGcmSecretCrypto.decrypt(certificado.getPasswordCifrado());
      KeyStore ks = KeyStore.getInstance("PKCS12");
      ks.load(
          new ByteArrayInputStream(localCertificadoStorage.leer(certificado.getArchivoStorageKey())),
          password.toCharArray());
      String alias = ks.aliases().nextElement();
      PrivateKey privateKey = (PrivateKey) ks.getKey(alias, password.toCharArray());
      X509Certificate certificate = (X509Certificate) ks.getCertificate(alias);
      return new LoadedCertificate(privateKey, certificate);
    } catch (Exception e) {
      throw new IllegalStateException("No se pudo cargar el certificado de firma", e);
    }
  }

  public record LoadedCertificate(PrivateKey privateKey, X509Certificate certificate) {}
}
