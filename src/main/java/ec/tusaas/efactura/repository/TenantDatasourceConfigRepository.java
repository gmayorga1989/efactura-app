package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.TenantDatasourceConfig;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantDatasourceConfigRepository extends JpaRepository<TenantDatasourceConfig, UUID> {

  Optional<TenantDatasourceConfig> findByEmpresa_Id(UUID empresaId);

  List<TenantDatasourceConfig> findByDatasourceKeyAndEstadoOrderByEmpresa_RazonSocialAsc(
      String datasourceKey, String estado);
}
