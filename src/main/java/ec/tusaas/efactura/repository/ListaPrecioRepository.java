package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.ListaPrecio;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListaPrecioRepository extends JpaRepository<ListaPrecio, UUID> {

  List<ListaPrecio> findByEmpresa_IdAndEstadoOrderByCodigoAsc(UUID empresaId, String estado);

  Optional<ListaPrecio> findByEmpresa_IdAndCodigo(UUID empresaId, String codigo);
}
