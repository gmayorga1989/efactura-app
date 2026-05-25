package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.ProductoCategoria;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoCategoriaRepository extends JpaRepository<ProductoCategoria, UUID> {

  List<ProductoCategoria> findByEmpresa_IdAndEstadoOrderByOrdenAscNombreAsc(UUID empresaId, String estado);

  boolean existsByEmpresa_IdAndCodigoIgnoreCaseAndIdNot(UUID empresaId, String codigo, UUID excludeId);

  boolean existsByEmpresa_IdAndCodigoIgnoreCase(UUID empresaId, String codigo);

  long countByParent_IdAndEstadoNot(UUID parentId, String estadoExcluir);

  java.util.Optional<ProductoCategoria> findByIdAndEmpresa_Id(UUID id, UUID empresaId);
}
