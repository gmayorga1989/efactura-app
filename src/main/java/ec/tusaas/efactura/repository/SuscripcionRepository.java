package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.entity.Suscripcion;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SuscripcionRepository extends JpaRepository<Suscripcion, UUID> {

  Optional<Suscripcion> findFirstByEmpresaAndEstadoOrderByFechaInicioDesc(
      Empresa empresa, String estado);
}
