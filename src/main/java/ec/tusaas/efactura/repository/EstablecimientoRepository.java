package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.Establecimiento;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstablecimientoRepository extends JpaRepository<Establecimiento, UUID> {

  List<Establecimiento> findByEmpresa_IdOrderByCodigoAsc(UUID empresaId);

  Optional<Establecimiento> findByEmpresa_IdAndCodigo(UUID empresaId, String codigo);

  Optional<Establecimiento> findByIdAndEmpresa_Id(UUID id, UUID empresaId);

  long countByEmpresa_IdAndEstado(UUID empresaId, String estado);
}
