package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.Vendedor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendedorRepository extends JpaRepository<Vendedor, UUID> {

  Page<Vendedor> findByEmpresa_IdAndEstadoOrderByNombresAsc(UUID empresaId, String estado, Pageable pageable);

  Page<Vendedor> findByEmpresa_IdOrderByNombresAsc(UUID empresaId, Pageable pageable);

  Optional<Vendedor> findByIdAndEmpresa_Id(UUID id, UUID empresaId);

  List<Vendedor> findByEmpresa_IdAndEstadoOrderByNombresAsc(UUID empresaId, String estado);

  boolean existsByEmpresa_IdAndCodigoIgnoreCaseAndIdNot(UUID empresaId, String codigo, UUID id);

  boolean existsByEmpresa_IdAndCodigoIgnoreCase(UUID empresaId, String codigo);

  long countByEmpresa_Id(UUID empresaId);
}
