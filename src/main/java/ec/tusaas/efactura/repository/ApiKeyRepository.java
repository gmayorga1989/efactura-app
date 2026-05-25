package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.ApiKey;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

  boolean existsByPrefix(String prefix);

  List<ApiKey> findByEmpresa_IdOrderByFechaCreacionDesc(UUID empresaId);

  @EntityGraph(attributePaths = "empresa")
  Optional<ApiKey> findByPrefixAndEstado(String prefix, String estado);

  Optional<ApiKey> findByIdAndEmpresa_Id(UUID id, UUID empresaId);
}
