package ec.tusaas.efactura.web;

import ec.tusaas.efactura.storage.ObjectStorageService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/public/v1/storage")
@RequiredArgsConstructor
public class PublicStorageController {

  private static final String LOGOS_PREFIX = "/api/public/v1/storage/logos/";
  private static final String AVATARS_PREFIX = "/api/public/v1/storage/avatars/";

  private final ObjectStorageService objectStorageService;

  @GetMapping("/logos/**")
  public ResponseEntity<byte[]> logo(HttpServletRequest request) throws IOException {
    String uri = request.getRequestURI();
    String suffix = uri.substring(uri.indexOf(LOGOS_PREFIX) + LOGOS_PREFIX.length());
    String key = "logos/" + suffix;
    log.debug("Storage publico: leyendo logo key={}", key);
    return ResponseEntity.ok().contentType(mediaType(key)).body(objectStorageService.leer(key));
  }

  @GetMapping("/avatars/**")
  public ResponseEntity<byte[]> avatar(HttpServletRequest request) throws IOException {
    String uri = request.getRequestURI();
    String suffix = uri.substring(uri.indexOf(AVATARS_PREFIX) + AVATARS_PREFIX.length());
    String key = "avatars/" + suffix;
    log.debug("Storage publico: leyendo avatar key={}", key);
    return ResponseEntity.ok().contentType(mediaType(key)).body(objectStorageService.leer(key));
  }

  private static MediaType mediaType(String key) {
    String lower = key.toLowerCase();
    if (lower.endsWith(".png")) {
      return MediaType.IMAGE_PNG;
    }
    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
      return MediaType.IMAGE_JPEG;
    }
    if (lower.endsWith(".svg")) {
      return MediaType.valueOf("image/svg+xml");
    }
    if (lower.endsWith(".webp")) {
      return MediaType.valueOf("image/webp");
    }
    return MediaType.APPLICATION_OCTET_STREAM;
  }
}
