package ec.tusaas.efactura.storage;

import ec.tusaas.efactura.config.props.StorageProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalCertificadoStorage {

  private final StorageProperties storageProperties;
  private final ObjectStorageService objectStorageService;

  /** Guarda bytes y devuelve una clave estable dentro del proveedor activo. */
  public String guardar(UUID empresaId, byte[] contenido, String nombreOriginal) throws IOException {
    String key = "certificados/" + empresaId + "/" + UUID.randomUUID() + extensionOf(nombreOriginal);
    log.debug("Certificado storage: guardando empresaId={} key={} bytes={}", empresaId, key, contenido.length);
    return objectStorageService.guardar(key, contenido, "application/x-pkcs12");
  }

  public byte[] leer(String storageKey) throws IOException {
    try {
      return objectStorageService.leer(storageKey);
    } catch (IOException e) {
      return leerLegacyLocal(storageKey, e);
    }
  }

  private byte[] leerLegacyLocal(String storageKey, IOException original) throws IOException {
    Path root = Path.of(storageProperties.getLocalRoot()).toAbsolutePath().normalize();
    Path file = root.resolve(storageKey).normalize();
    if (!file.startsWith(root) || !Files.exists(file)) {
      throw original;
    }
    return Files.readAllBytes(file);
  }

  private static String extensionOf(String original) {
    if (original == null) {
      return ".p12";
    }
    String lower = original.toLowerCase();
    if (lower.endsWith(".pfx")) {
      return ".pfx";
    }
    if (lower.endsWith(".p12")) {
      return ".p12";
    }
    return ".p12";
  }
}
