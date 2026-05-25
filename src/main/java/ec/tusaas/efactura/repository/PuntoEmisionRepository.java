package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.PuntoEmision;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PuntoEmisionRepository extends JpaRepository<PuntoEmision, UUID> {

  List<PuntoEmision> findByEstablecimiento_IdOrderByCodigoAsc(UUID establecimientoId);

  Optional<PuntoEmision> findByEstablecimiento_IdAndCodigo(UUID establecimientoId, String codigo);

  Optional<PuntoEmision> findByIdAndEmpresa_Id(UUID id, UUID empresaId);

  List<PuntoEmision> findByEmpresa_IdOrderByEstablecimiento_CodigoAscCodigoAsc(UUID empresaId);

  long countByEmpresa_IdAndEstado(UUID empresaId, String estado);
}
