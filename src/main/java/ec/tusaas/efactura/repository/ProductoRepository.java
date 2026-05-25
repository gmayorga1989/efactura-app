package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.Producto;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, UUID> {

  Page<Producto> findByEmpresa_IdAndEstadoNotOrderByDescripcionAsc(UUID empresaId, String estado, Pageable pageable);

  Optional<Producto> findByIdAndEmpresa_Id(UUID id, UUID empresaId);

  Optional<Producto> findByEmpresa_IdAndCodigoPrincipal(UUID empresaId, String codigoPrincipal);

  long countByCategoria_IdAndEstadoNot(UUID categoriaId, String estado);

  long countByEmpresa_IdAndEstadoNot(UUID empresaId, String estado);

  long countByEmpresa_IdAndEstadoNotAndTipoIgnoreCase(UUID empresaId, String estado, String tipo);
}
