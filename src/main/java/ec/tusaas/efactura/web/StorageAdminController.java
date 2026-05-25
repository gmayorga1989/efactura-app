package ec.tusaas.efactura.web;

import ec.tusaas.efactura.config.props.StorageProperties;
import ec.tusaas.efactura.dto.storage.StorageStatusResponse;
import ec.tusaas.efactura.storage.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/web/v1/storage")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
public class StorageAdminController {

  private final StorageProperties storageProperties;
  private final ObjectStorageService objectStorageService;

  @GetMapping("/status")
  public StorageStatusResponse status() {
    return new StorageStatusResponse(
        storageProperties.getProvider(),
        objectStorageService.getClass().getSimpleName(),
        storageProperties.getObjectRoot(),
        storageProperties.getBucket(),
        storageProperties.getRegion(),
        storageProperties.getEndpoint(),
        storageProperties.getPublicBaseUrl(),
        hasText(storageProperties.getAccessKey()),
        hasText(storageProperties.getSecretKey()));
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
