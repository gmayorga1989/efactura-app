package ec.tusaas.efactura.service;

import ec.tusaas.efactura.entity.TenantDatasourceConfig;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Punto unico para resolver donde vive la data operativa de una empresa.
 * La fase actual solo expone metadata; el routing fisico de DataSource se conectara aqui.
 */
@Service
@RequiredArgsConstructor
public class TenantRoutingService {

  private final TenantDatasourceConfigService tenantDatasourceConfigService;

  @Transactional(readOnly = true)
  public TenantResolution resolver(UUID empresaId) {
    TenantDatasourceConfig config = tenantDatasourceConfigService.obtenerOError(empresaId);
    return new TenantResolution(empresaId, config.getModoTenant(), config.getDatasourceKey());
  }

  public record TenantResolution(UUID empresaId, String modoTenant, String datasourceKey) {}
}
