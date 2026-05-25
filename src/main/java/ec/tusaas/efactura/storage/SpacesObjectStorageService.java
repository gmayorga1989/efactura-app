package ec.tusaas.efactura.storage;

import ec.tusaas.efactura.config.props.StorageProperties;
import java.io.IOException;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "efactura.storage", name = "provider", havingValue = "spaces")
public class SpacesObjectStorageService implements ObjectStorageService {

  private final StorageProperties storageProperties;
  private final S3Client s3Client;

  public SpacesObjectStorageService(StorageProperties storageProperties) {
    this.storageProperties = storageProperties;
    validarConfiguracion(storageProperties);
    this.s3Client =
        S3Client.builder()
            .endpointOverride(URI.create(endpoint(storageProperties)))
            .region(Region.of(storageProperties.getRegion()))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        storageProperties.getAccessKey(), storageProperties.getSecretKey())))
            .build();
    log.info(
        "Storage Spaces inicializado bucket={} region={} endpoint={} publicBaseUrl={}",
        storageProperties.getBucket(),
        storageProperties.getRegion(),
        endpoint(storageProperties),
        storageProperties.getPublicBaseUrl());
  }

  @Override
  public String guardar(String storageKey, byte[] contenido, String contentType) {
    return guardar(storageKey, contenido, contentType, false);
  }

  @Override
  public String guardarPublico(String storageKey, byte[] contenido, String contentType) {
    return guardar(storageKey, contenido, contentType, true);
  }

  private String guardar(String storageKey, byte[] contenido, String contentType, boolean publico) {
    PutObjectRequest.Builder request =
        PutObjectRequest.builder().bucket(storageProperties.getBucket()).key(storageKey);
    if (contentType != null && !contentType.isBlank()) {
      request.contentType(contentType);
    }
    if (publico) {
      request.acl(ObjectCannedACL.PUBLIC_READ);
    }
    try {
      s3Client.putObject(request.build(), RequestBody.fromBytes(contenido));
      log.info(
          "Storage Spaces: objeto guardado bucket={} key={} bytes={} contentType={} publico={}",
          storageProperties.getBucket(),
          storageKey,
          contenido.length,
          contentType,
          publico);
    } catch (S3Exception e) {
      log.error(
          "Storage Spaces: error guardando objeto bucket={} key={} status={} code={} message={}",
          storageProperties.getBucket(),
          storageKey,
          e.statusCode(),
          e.awsErrorDetails() == null ? null : e.awsErrorDetails().errorCode(),
          e.awsErrorDetails() == null ? e.getMessage() : e.awsErrorDetails().errorMessage(),
          e);
      throw e;
    }
    return storageKey;
  }

  @Override
  public byte[] leer(String storageKey) throws IOException {
    try {
      log.debug("Storage Spaces: leyendo objeto bucket={} key={}", storageProperties.getBucket(), storageKey);
      return s3Client
          .getObjectAsBytes(GetObjectRequest.builder().bucket(storageProperties.getBucket()).key(storageKey).build())
          .asByteArray();
    } catch (S3Exception e) {
      log.error(
          "Storage Spaces: error leyendo objeto bucket={} key={} status={} code={} message={}",
          storageProperties.getBucket(),
          storageKey,
          e.statusCode(),
          e.awsErrorDetails() == null ? null : e.awsErrorDetails().errorCode(),
          e.awsErrorDetails() == null ? e.getMessage() : e.awsErrorDetails().errorMessage(),
          e);
      throw e;
    }
  }

  @Override
  public String publicUrl(String storageKey) {
    String base = storageProperties.getPublicBaseUrl();
    if (base != null && !base.isBlank()) {
      return base.replaceAll("/+$", "") + "/" + storageKey;
    }
    return "https://"
        + storageProperties.getBucket()
        + "."
        + storageProperties.getRegion()
        + ".digitaloceanspaces.com/"
        + storageKey;
  }

  private static void validarConfiguracion(StorageProperties props) {
    if (blank(props.getBucket()) || blank(props.getAccessKey()) || blank(props.getSecretKey())) {
      throw new IllegalStateException(
          "efactura.storage.bucket, access-key y secret-key son obligatorios para provider=spaces");
    }
  }

  private static String endpoint(StorageProperties props) {
    if (!blank(props.getEndpoint())) {
      return props.getEndpoint();
    }
    return "https://" + props.getRegion() + ".digitaloceanspaces.com";
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
