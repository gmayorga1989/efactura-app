package ec.tusaas.efactura.storage;

import ec.tusaas.efactura.config.props.StorageProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalComprobanteStorage {

  private final StorageProperties storageProperties;
  private final ObjectStorageService objectStorageService;

  public String guardarTexto(UUID empresaId, UUID comprobanteId, String tipo, String contenido)
      throws IOException {
    return guardarBytes(empresaId, comprobanteId, tipo, "xml", contenido.getBytes(StandardCharsets.UTF_8));
  }

  public String guardarBytes(UUID empresaId, UUID comprobanteId, String tipo, String extension, byte[] contenido)
      throws IOException {
    String filename = tipo.toLowerCase() + "." + extension;
    String key = "comprobantes/" + empresaId + "/" + comprobanteId + "/" + filename;
    log.debug("Comprobante storage: guardando empresaId={} comprobanteId={} key={}", empresaId, comprobanteId, key);
    return objectStorageService.guardar(key, contenido, contentType(extension));
  }

  public String leerTexto(String storageKey) throws IOException {
    return new String(leerBytes(storageKey), StandardCharsets.UTF_8);
  }

  public byte[] leerBytes(String storageKey) throws IOException {
    try {
      return objectStorageService.leer(storageKey);
    } catch (IOException e) {
      return Files.readAllBytes(resolverLegacyLocal(storageKey, e));
    }
  }

  private Path resolverLegacyLocal(String storageKey, IOException original) throws IOException {
    Path root = Path.of(storageProperties.getComprobantesRoot()).toAbsolutePath().normalize();
    Path file = root.resolve(storageKey).normalize();
    if (!file.startsWith(root) || !Files.exists(file)) {
      throw original;
    }
    return file;
  }

  private static String contentType(String extension) {
    return switch (extension.toLowerCase()) {
      case "xml" -> "application/xml";
      case "pdf" -> "application/pdf";
      default -> "application/octet-stream";
    };
  }
}
