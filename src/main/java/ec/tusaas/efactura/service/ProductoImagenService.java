package ec.tusaas.efactura.service;

import ec.tusaas.efactura.entity.Producto;
import ec.tusaas.efactura.repository.ProductoRepository;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.storage.ObjectStorageService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductoImagenService {

  private static final Set<String> CONTENT_TYPES = Set.of("image/png", "image/jpeg", "image/webp");

  private final ProductoRepository productoRepository;
  private final ObjectStorageService objectStorageService;

  @Transactional
  public Producto subir(UUID empresaId, UUID productoId, MultipartFile archivo, UsuarioPrincipal principal)
      throws Exception {
    validar(archivo);
    Producto p =
        productoRepository
            .findByIdAndEmpresa_Id(productoId, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto/servicio no encontrado"));
    String extension = extension(archivo.getOriginalFilename(), archivo.getContentType());
    String key = "productos/" + empresaId + "/" + productoId + "/img-" + UUID.randomUUID() + extension;
    objectStorageService.guardarPublico(key, archivo.getBytes(), archivo.getContentType());
    String url = objectStorageService.publicUrl(key);
    p.setImagenStorageKey(key);
    p.setImagenUrl(url);
    p.setFechaModificacion(Instant.now());
    p.setUsuarioModificacion(principal.getEmail());
    productoRepository.save(p);
    log.info("Producto imagen: empresaId={} productoId={} key={}", empresaId, productoId, key);
    return p;
  }

  private static void validar(MultipartFile archivo) {
    if (archivo == null || archivo.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo vacio");
    }
    if (archivo.getSize() > 3 * 1024 * 1024) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Imagen maximo 3MB");
    }
    String contentType = archivo.getContentType();
    if (contentType == null || !CONTENT_TYPES.contains(contentType)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de imagen no permitido (PNG, JPEG, WebP)");
    }
  }

  private static String extension(String nombre, String contentType) {
    if (nombre != null) {
      String lower = nombre.toLowerCase();
      for (String ext : List.of(".png", ".jpg", ".jpeg", ".webp")) {
        if (lower.endsWith(ext)) {
          return ext;
        }
      }
    }
    return switch (contentType) {
      case "image/png" -> ".png";
      case "image/jpeg" -> ".jpg";
      case "image/webp" -> ".webp";
      default -> ".bin";
    };
  }
}
