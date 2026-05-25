package ec.tusaas.efactura.emision.ride;

import com.lowagie.text.Image;
import ec.tusaas.efactura.config.props.StorageProperties;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.repository.EmpresaRepository;
import ec.tusaas.efactura.storage.ObjectStorageService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RideLogoService {

  private final EmpresaRepository empresaRepository;
  private final ObjectStorageService objectStorageService;
  private final StorageProperties storageProperties;

  @Value("${server.port:8080}")
  private int serverPort;

  public Image cargarLogo(Empresa empresa) {
    if (empresa == null) {
      return null;
    }
    String logoUrl = resolverLogoUrl(empresa);
    if (logoUrl == null || logoUrl.isBlank()) {
      return null;
    }
    logoUrl = logoUrl.trim();
    String key = extraerStorageKey(logoUrl);
    if (key != null) {
      try {
        byte[] bytes = objectStorageService.leer(key);
        Image img = RidePdfSupport.imagenDesdeBytes(bytes, logoUrl, 130, 95);
        if (img != null) {
          return img;
        }
      } catch (Exception e) {
        log.debug("Logo no leido desde storage key={}: {}", key, e.getMessage());
      }
    }
    String absUrl = resolverUrlAbsoluta(logoUrl);
    Image http = RidePdfSupport.cargarLogoHttp(absUrl);
    if (http != null) {
      return http;
    }
    log.warn("No se pudo cargar logo empresaId={} url={} key={}", empresa.getId(), logoUrl, key);
    return null;
  }

  private String resolverLogoUrl(Empresa empresa) {
    if (empresa.getLogoUrl() != null && !empresa.getLogoUrl().isBlank()) {
      return empresa.getLogoUrl();
    }
    UUID id = empresa.getId();
    if (id == null) {
      return null;
    }
    return empresaRepository.findById(id).map(Empresa::getLogoUrl).orElse(null);
  }

  static String extraerStorageKey(String logoUrl) {
    if (logoUrl == null || logoUrl.isBlank()) {
      return null;
    }
    String trimmed = logoUrl.trim();
    if (trimmed.startsWith("logos/")) {
      String key = trimmed;
      int q = key.indexOf('?');
      if (q > 0) {
        key = key.substring(0, q);
      }
      return key;
    }
    String marker = "/storage/";
    int storageIdx = trimmed.indexOf(marker);
    if (storageIdx >= 0) {
      String key = trimmed.substring(storageIdx + marker.length());
      if (key.startsWith("logos/")) {
        int q = key.indexOf('?');
        if (q > 0) {
          key = key.substring(0, q);
        }
        return key;
      }
    }
    int idx = trimmed.indexOf("logos/");
    if (idx < 0) {
      return null;
    }
    String key = trimmed.substring(idx);
    int q = key.indexOf('?');
    if (q > 0) {
      key = key.substring(0, q);
    }
    int h = key.indexOf('#');
    if (h > 0) {
      key = key.substring(0, h);
    }
    return key;
  }

  private String resolverUrlAbsoluta(String logoUrl) {
    if (logoUrl.startsWith("http://") || logoUrl.startsWith("https://")) {
      return logoUrl;
    }
    String base = storageProperties.getPublicBaseUrl();
    if (base == null || base.isBlank()) {
      base = "http://localhost:" + serverPort;
    }
    base = base.replaceAll("/+$", "");
    if (logoUrl.startsWith("/api/")) {
      return base + logoUrl;
    }
    if (logoUrl.startsWith("/")) {
      return base + logoUrl;
    }
    if (logoUrl.startsWith("logos/") || logoUrl.startsWith("avatars/")) {
      return base + "/api/public/v1/storage/" + logoUrl;
    }
    return base + "/" + logoUrl;
  }
}
