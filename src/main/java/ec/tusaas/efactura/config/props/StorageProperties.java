package ec.tusaas.efactura.config.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "efactura.storage")
public class StorageProperties {

  /** Proveedor activo: local o spaces. */
  private String provider = "local";

  /** Directorio raiz comun para el almacenamiento local por objetos. */
  private String objectRoot = "./data";

  /** Directorio raiz para certificados (.p12) en almacenamiento local. */
  private String localRoot = "./data/certificados";

  /** Directorio raiz para XML/RIDE de comprobantes emitidos. */
  private String comprobantesRoot = "./data/comprobantes";

  /** Directorio raiz para logos cuando se usa almacenamiento local. */
  private String logosRoot = "./data/logos";

  /** Bucket/Space para DigitalOcean Spaces. */
  private String bucket;

  /** Region slug de Spaces, por ejemplo nyc3, sfo3, ams3. */
  private String region = "nyc3";

  /** Endpoint S3-compatible. Si se omite, se arma como https://{region}.digitaloceanspaces.com. */
  private String endpoint;

  private String accessKey;

  private String secretKey;

  /** URL publica/CDN opcional para exponer logos. Ej: https://cdn.example.com. */
  private String publicBaseUrl;
}
