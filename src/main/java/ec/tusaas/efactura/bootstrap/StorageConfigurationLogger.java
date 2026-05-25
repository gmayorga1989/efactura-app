package ec.tusaas.efactura.bootstrap;

import ec.tusaas.efactura.config.props.StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(50)
@RequiredArgsConstructor
public class StorageConfigurationLogger implements ApplicationRunner {

  private final StorageProperties storageProperties;

  @Override
  public void run(ApplicationArguments args) {
    log.info(
        "Storage configurado provider={} objectRoot={} bucket={} region={} endpoint={} publicBaseUrl={}",
        storageProperties.getProvider(),
        storageProperties.getObjectRoot(),
        storageProperties.getBucket(),
        storageProperties.getRegion(),
        storageProperties.getEndpoint(),
        storageProperties.getPublicBaseUrl());
  }
}
