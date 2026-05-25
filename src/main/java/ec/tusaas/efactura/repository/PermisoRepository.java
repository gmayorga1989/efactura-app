package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.Permiso;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermisoRepository extends JpaRepository<Permiso, UUID> {

  Optional<Permiso> findByCodigo(String codigo);

  List<Permiso> findAllByEstadoOrderByModuloAscCodigoAsc(String estado);
}
