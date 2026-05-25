package ec.tusaas.efactura.storage;

import java.io.IOException;

public interface ObjectStorageService {

  String guardar(String storageKey, byte[] contenido, String contentType) throws IOException;

  default String guardarPublico(String storageKey, byte[] contenido, String contentType) throws IOException {
    return guardar(storageKey, contenido, contentType);
  }

  byte[] leer(String storageKey) throws IOException;

  String publicUrl(String storageKey);
}
