package ec.tusaas.efactura.storage;

import ec.tusaas.efactura.config.props.StorageProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "efactura.storage", name = "provider", havingValue = "local", matchIfMissing = true)
public class LocalObjectStorageService implements ObjectStorageService {

  private final StorageProperties storageProperties;

  @Override
  public String guardar(String storageKey, byte[] contenido, String contentType) throws IOException {
    Path root = root();
    Path target = root.resolve(storageKey).normalize();
    if (!target.startsWith(root)) {
      throw new IllegalArgumentException("Ruta invalida");
    }
    Files.createDirectories(target.getParent());
    Files.write(target, contenido);
    log.info(
        "Storage local: objeto guardado key={} path={} bytes={} contentType={}",
        storageKey,
        target,
        contenido.length,
        contentType);
    return storageKey;
  }

  @Override
  public byte[] leer(String storageKey) throws IOException {
    Path root = root();
    Path file = root.resolve(storageKey).normalize();
    if (!file.startsWith(root)) {
      throw new IllegalArgumentException("Ruta invalida");
    }
    log.debug("Storage local: leyendo objeto key={} path={}", storageKey, file);
    return Files.readAllBytes(file);
  }

  @Override
  public String publicUrl(String storageKey) {
    String path = "/api/public/v1/storage/" + storageKey;
    String base = storageProperties.getPublicBaseUrl();
    if (base == null || base.isBlank()) {
      return path;
    }
    base = base.replaceAll("/+$", "");
    if (base.endsWith("/api/public/v1/storage")) {
      return base + "/" + storageKey;
    }
    return base + path;
  }

  private Path root() {
    return Path.of(storageProperties.getObjectRoot()).toAbsolutePath().normalize();
  }

  @Override
  public void eliminar(String storageKey) throws IOException {
    Path root = root();
    Path file = root.resolve(storageKey).normalize();
    if (!file.startsWith(root)) {
      throw new IllegalArgumentException("Ruta invalida");
    }
    boolean deleted = Files.deleteIfExists(file);
    log.info("Storage local: eliminado key={} path={} deleted={}", storageKey, file, deleted);
  }
}
